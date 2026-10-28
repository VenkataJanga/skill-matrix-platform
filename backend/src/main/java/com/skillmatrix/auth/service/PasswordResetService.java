package com.skillmatrix.auth.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.auth.dto.ForgotPasswordRequest;
import com.skillmatrix.auth.dto.ForgotPasswordResponse;
import com.skillmatrix.auth.dto.ResetPasswordRequest;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.config.PasswordResetProperties;
import com.skillmatrix.domain.entity.PasswordResetToken;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.PasswordResetTokenRepository;
import com.skillmatrix.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Development/Pilot "Forgot Password" implementation.
 *
 * <p>Design principles (see docs/AUTH_ARCHITECTURE.md for the full note):
 * <ul>
 *   <li>Account enumeration is prevented — {@link #requestReset} always
 *       returns the same generic message regardless of whether the account
 *       exists, is inactive, or is unknown.</li>
 *   <li>The one-time reset token is a cryptographically random value; only
 *       its SHA-256 hash is ever persisted (mirrors {@link RefreshTokenService}).</li>
 *   <li>Password reset never reactivates an inactive account.</li>
 *   <li>On successful reset: password hash is replaced, token is marked used,
 *       must_change_password is cleared (self-service — the user chose their
 *       own final password), all refresh tokens are revoked, and an audit
 *       record is written.</li>
 *   <li>This service is intentionally isolated from how authentication
 *       happens — it only depends on {@link User}/{@link UserRepository} and
 *       is unaffected by a future Okta/Corporate-SSO migration.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PasswordResetProperties resetProperties;
    private final Environment environment;

    private static final String FRONTEND_BASE_URL_PROPERTY = "app.frontend-base-url";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ----------------------------------------------------------------
    // Forgot Password — request a reset
    // ----------------------------------------------------------------

    @Transactional
    public ForgotPasswordResponse requestReset(ForgotPasswordRequest request, String ipAddress) {
        String identifier = request.getUsernameOrEmail() == null ? "" : request.getUsernameOrEmail().trim();

        Optional<User> userOpt = identifier.contains("@")
                ? userRepository.findByEmail(identifier)
                : userRepository.findByUsername(identifier);

        // Deliberately identical response path for unknown/inactive/active accounts.
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            log.info("Password reset requested for unresolvable or inactive account reference");
            return ForgotPasswordResponse.generic();
        }

        User user = userOpt.get();

        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);

        // Invalidate any previously issued, still-usable tokens for this user
        tokenRepository.invalidateAllActiveForUser(user);

        PasswordResetToken token = PasswordResetToken.builder()
                .publicId(UUID.randomUUID().toString())
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(resetProperties.getExpiryMinutes()))
                .requestedFromIp(ipAddress)
                .active(true)
                .build();
        tokenRepository.save(token);

        auditService.log(AuditService.PASSWORD_RESET_REQUESTED, AuditService.ENTITY_USER,
                user.getId(), user.getUsername(), user.getPublicId(), ipAddress, null, null);
        log.info("Password reset requested for user reference publicId={}", user.getPublicId());

        String resetLink = buildResetLink(rawToken);

        // DEV-ONLY convenience: never enabled outside the 'dev' profile, never logged.
        if (isDevProfileActive() && resetProperties.isDevExposeToken()) {
            return ForgotPasswordResponse.genericWithDevLink(resetLink);
        }

        // TODO (future): integrate real SMTP delivery here when email infrastructure
        // is introduced. Until then, the Admin-assisted fallback is the supported
        // path for environments without email configured.
        return ForgotPasswordResponse.generic();
    }

    // ----------------------------------------------------------------
    // Reset Password — consume the token
    // ----------------------------------------------------------------

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String ipAddress, String correlationId) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("New password and confirmation do not match");
        }

        String tokenHash = sha256(request.getToken());
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> invalidTokenException());

        if (!token.isValid()) {
            throw invalidTokenException();
        }

        User user = token.getUser();

        // Password reset must never override account lifecycle.
        if (!user.isActive()) {
            throw invalidTokenException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // Self-service Forgot Password — user chose their own final password.
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        token.setActive(false);
        tokenRepository.save(token);

        refreshTokenService.revokeAllForUser(user);

        auditService.log(AuditService.PASSWORD_RESET_COMPLETED, AuditService.ENTITY_USER,
                user.getId(), user.getUsername(), user.getPublicId(), ipAddress, correlationId, null);
        auditService.log(AuditService.SESSION_INVALIDATED_AFTER_PASSWORD_RESET, AuditService.ENTITY_USER,
                user.getId(), user.getUsername(), user.getPublicId(), ipAddress, correlationId, null);

        log.info("Password reset completed for user reference publicId={}", user.getPublicId());
    }

    // ----------------------------------------------------------------
    // Admin-assisted fallback
    // ----------------------------------------------------------------

    /**
     * Admin-triggered password reset. The Admin never sees, retrieves, or
     * chooses the user's permanent password — a secure one-time reset link
     * (same mechanism as self-service) is generated instead. {@code
     * must_change_password} is set as a safety net in case the user
     * completes the reset through some other forced-change path.
     *
     * @return the raw reset link — included in the API response ONLY under
     *         the dev profile; callers must treat the returned value as
     *         sensitive and never log it.
     */
    @Transactional
    public String adminInitiateReset(User user, String actorUsername, String ipAddress) {
        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);

        tokenRepository.invalidateAllActiveForUser(user);

        PasswordResetToken token = PasswordResetToken.builder()
                .publicId(UUID.randomUUID().toString())
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(resetProperties.getExpiryMinutes()))
                .requestedFromIp(ipAddress)
                .active(true)
                .build();
        tokenRepository.save(token);

        user.setMustChangePassword(true);
        user.setUpdatedBy(actorUsername);
        userRepository.save(user);

        // Invalidate any sessions the user currently holds — a reset in progress
        // should not leave a previously authenticated device active indefinitely.
        refreshTokenService.revokeAllForUser(user);

        auditService.log(AuditService.ADMIN_PASSWORD_RESET_INITIATED, AuditService.ENTITY_USER,
                null, actorUsername, user.getPublicId(), ipAddress, null,
                "{\"targetUsername\":\"" + user.getUsername() + "\"}");
        auditService.log(AuditService.SESSION_INVALIDATED_AFTER_PASSWORD_RESET, AuditService.ENTITY_USER,
                null, actorUsername, user.getPublicId(), ipAddress, null, null);

        log.info("Admin-assisted password reset initiated for user publicId={} by={}",
                user.getPublicId(), actorUsername);

        return buildResetLink(rawToken);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private BusinessException invalidTokenException() {
        return new BusinessException(
                "This password reset link is invalid or has expired. Please request a new one.",
                HttpStatus.BAD_REQUEST);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildResetLink(String rawToken) {
        String base = environment.getProperty(FRONTEND_BASE_URL_PROPERTY, "http://localhost:4200");
        return base + "/reset-password?token=" + rawToken;
    }

    private boolean isDevProfileActive() {
        List<String> profiles = List.of(environment.getActiveProfiles());
        return profiles.contains("dev");
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
