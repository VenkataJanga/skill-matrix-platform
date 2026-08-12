package com.skillmatrix.auth.service;

import com.skillmatrix.common.exception.AuthException;
import com.skillmatrix.domain.entity.RefreshToken;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Manages refresh token lifecycle.
 *
 * <p>Raw token = random UUID. Only the SHA-256 hash is persisted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Create and persist a new refresh token for the user.
     *
     * @return the raw (unhashed) token — returned to client once, never stored
     */
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .publicId(UUID.randomUUID().toString())
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .revoked(false)
                .deleted(false)
                .createdBy(user.getId())
                .build();

        refreshTokenRepository.save(refreshToken);
        log.debug("Created refresh token for user '{}'", user.getUsername());
        return rawToken;
    }

    /**
     * Find and validate a refresh token by its raw value.
     *
     * @throws AuthException if the token is not found, revoked, or expired
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (!token.isValid()) {
            throw new AuthException("Refresh token is expired or revoked");
        }
        return token;
    }

    /**
     * Revoke all active refresh tokens for the user (logout / password change).
     */
    @Transactional
    public void revokeAllForUser(User user) {
        int count = refreshTokenRepository.revokeAllByUser(user, LocalDateTime.now());
        log.debug("Revoked {} refresh tokens for user '{}'", count, user.getUsername());
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

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
