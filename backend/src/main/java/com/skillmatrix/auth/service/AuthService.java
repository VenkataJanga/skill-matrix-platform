package com.skillmatrix.auth.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.auth.dto.*;
import com.skillmatrix.common.exception.AuthException;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.RefreshToken;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserRole;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core authentication service implementing all M1 auth flows.
 * Also implements {@link UserDetailsService} for Spring Security filter chain.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements UserDetailsService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    // ----------------------------------------------------------------
    // UserDetailsService (used by JwtAuthenticationFilter)
    // ----------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new UserPrincipal(user);
    }

    // ----------------------------------------------------------------
    // Login
    // ----------------------------------------------------------------

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String correlationId) {
        User user = userRepository.findByUsernameWithRolesAndPermissions(request.getUsername())
                .orElse(null);

        if (user == null) {
            auditService.logAuth(AuditService.LOGIN_FAILURE, null, request.getUsername(), ipAddress, correlationId);
            throw new AuthException("Invalid username or password");
        }

        // Account checks
        if (!user.isActive()) {
            auditService.logAuth(AuditService.LOGIN_FAILURE, user.getId(), user.getUsername(), ipAddress, correlationId);
            throw new AuthException("Account is inactive");
        }

        if (user.isAccountLocked()) {
            auditService.logAuth(AuditService.LOGIN_FAILURE, user.getId(), user.getUsername(), ipAddress, correlationId);
            throw new AuthException("Account is locked. Please contact your administrator");
        }

        // Password verification
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            auditService.logAuth(AuditService.LOGIN_FAILURE, user.getId(), user.getUsername(), ipAddress, correlationId);
            throw new AuthException("Invalid username or password");
        }

        // Success — reset failed attempts and update last login
        user.setFailedLoginAttempts(0);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        List<String> roles = extractRoleCodes(user);
        String accessToken = jwtService.generateToken(user.getPublicId(), user.getUsername(), roles);
        String rawRefreshToken = refreshTokenService.createRefreshToken(user);

        auditService.logAuth(AuditService.LOGIN_SUCCESS, user.getId(), user.getUsername(), ipAddress, correlationId);
        log.info("User '{}' logged in from {}", user.getUsername(), ipAddress);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .userPublicId(user.getPublicId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(roles)
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    // ----------------------------------------------------------------
    // Token Refresh
    // ----------------------------------------------------------------

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken storedToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        User user = storedToken.getUser();

        // Reload full graph for roles
        user = userRepository.findByUsernameWithRolesAndPermissions(user.getUsername())
                .orElseThrow(() -> new AuthException("User not found"));

        List<String> roles = extractRoleCodes(user);
        String newAccessToken = jwtService.generateToken(user.getPublicId(), user.getUsername(), roles);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .build();
    }

    // ----------------------------------------------------------------
    // Logout
    // ----------------------------------------------------------------

    @Transactional
    public void logout(User user, String ipAddress, String correlationId) {
        refreshTokenService.revokeAllForUser(user);
        auditService.logAuth(AuditService.LOGOUT, user.getId(), user.getUsername(), ipAddress, correlationId);
        log.info("User '{}' logged out from {}", user.getUsername(), ipAddress);
    }

    // ----------------------------------------------------------------
    // Change Password
    // ----------------------------------------------------------------

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request, String ipAddress, String correlationId) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("New password and confirmation do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Revoke all existing refresh tokens — force re-login with new password
        refreshTokenService.revokeAllForUser(user);

        auditService.logAuth(AuditService.PASSWORD_CHANGE, user.getId(), user.getUsername(), ipAddress, correlationId);
        log.info("User '{}' changed their password", user.getUsername());
    }

    // ----------------------------------------------------------------
    // Me (current user profile)
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public UserMeResponse getMe(String publicId) {
        User user = userRepository.findByUsernameWithRolesAndPermissions(
                userRepository.findByPublicId(publicId)
                        .orElseThrow(() -> new AuthException("User not found", HttpStatus.UNAUTHORIZED))
                        .getUsername())
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.UNAUTHORIZED));

        List<String> roles = extractRoleCodes(user);
        List<String> permissions = extractPermissions(user);

        return UserMeResponse.builder()
                .publicId(user.getPublicId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .employeeId(user.getEmployeeId())
                .roles(roles)
                .permissions(permissions)
                .mustChangePassword(user.isMustChangePassword())
                .lastLogin(user.getLastLogin())
                .build();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockTime(LocalDateTime.now());
            log.warn("User '{}' account locked after {} failed login attempts", user.getUsername(), attempts);
        }
        userRepository.save(user);
    }

    private List<String> extractRoleCodes(User user) {
        return user.getUserRoles().stream()
                .filter(UserRole::isActive)
                .map(ur -> ur.getRole().getRoleCode())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> extractPermissions(User user) {
        return user.getUserRoles().stream()
                .filter(UserRole::isActive)
                .flatMap(ur -> ur.getRole().getRolePermissions().stream())
                .filter(rp -> rp.getPermission().isActive())
                .map(rp -> rp.getPermission().getPermissionCode())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
