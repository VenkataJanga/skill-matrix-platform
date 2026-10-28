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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService — unit tests")
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private Environment environment;

    private PasswordResetProperties resetProperties;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        resetProperties = new PasswordResetProperties();
        resetProperties.setExpiryMinutes(20);
        resetProperties.setDevExposeToken(false);

        lenient().when(environment.getProperty(eq("app.frontend-base-url"), anyString()))
                .thenReturn("http://localhost:4200");
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{});

        service = new PasswordResetService(
                userRepository, tokenRepository, refreshTokenService,
                passwordEncoder, auditService, resetProperties, environment);
    }

    // ----------------------------------------------------------------
    // requestReset — account enumeration prevention
    // ----------------------------------------------------------------

    @Test
    @DisplayName("requestReset returns generic message for existing active user")
    void requestReset_existingUser_returnsGeneric() {
        User user = activeUser(1L, "alice", "alice@x.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        ForgotPasswordResponse response = service.requestReset(req("alice"), "127.0.0.1");

        assertThat(response.message()).contains("If an active account matches");
        assertThat(response.devResetLink()).isNull();
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(auditService).log(eq(AuditService.PASSWORD_RESET_REQUESTED), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("requestReset returns identical generic message for unknown user")
    void requestReset_unknownUser_returnsSameGeneric() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        ForgotPasswordResponse response = service.requestReset(req("ghost"), "127.0.0.1");

        assertThat(response.message()).contains("If an active account matches");
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("requestReset does not issue a token for an inactive user (no reactivation)")
    void requestReset_inactiveUser_doesNotIssueToken() {
        User inactive = activeUser(2L, "bob", "bob@x.com");
        inactive.setActive(false);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(inactive));

        ForgotPasswordResponse response = service.requestReset(req("bob"), "127.0.0.1");

        assertThat(response.message()).contains("If an active account matches");
        verify(tokenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("requestReset resolves by email when identifier contains '@'")
    void requestReset_byEmail() {
        User user = activeUser(3L, "carol", "carol@x.com");
        when(userRepository.findByEmail("carol@x.com")).thenReturn(Optional.of(user));

        service.requestReset(req("carol@x.com"), "127.0.0.1");

        verify(userRepository).findByEmail("carol@x.com");
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("requestReset stores only the SHA-256 hash, never the raw token")
    void requestReset_storesHashedTokenOnly() {
        User user = activeUser(1L, "alice", "alice@x.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        service.requestReset(req("alice"), "127.0.0.1");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();

        assertThat(saved.getTokenHash()).isNotBlank().hasSize(64); // SHA-256 hex
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("requestReset invalidates any previously issued active tokens for the user")
    void requestReset_invalidatesPriorTokens() {
        User user = activeUser(1L, "alice", "alice@x.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        service.requestReset(req("alice"), "127.0.0.1");

        verify(tokenRepository).invalidateAllActiveForUser(user);
    }

    @Test
    @DisplayName("requestReset exposes dev reset link only when dev profile AND dev-expose-token are both true")
    void requestReset_devLinkOnlyUnderDevProfile() {
        User user = activeUser(1L, "alice", "alice@x.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        resetProperties.setDevExposeToken(true);

        ForgotPasswordResponse response = service.requestReset(req("alice"), "127.0.0.1");

        assertThat(response.devResetLink()).isNotBlank();
        assertThat(response.devResetLink()).contains("/reset-password?token=");
    }

    // ----------------------------------------------------------------
    // resetPassword
    // ----------------------------------------------------------------

    @Test
    @DisplayName("resetPassword succeeds with a valid, unexpired, unused token")
    void resetPassword_validToken_succeeds() {
        User user = activeUser(1L, "alice", "alice@x.com");
        PasswordResetToken token = validToken(user, "raw-token");
        when(tokenRepository.findByTokenHash(PasswordResetService.sha256("raw-token")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass8!")).thenReturn("hashed-new-pass");

        service.resetPassword(resetReq("raw-token", "NewPass8!", "NewPass8!"), "127.0.0.1", "corr-1");

        assertThat(user.getPasswordHash()).isEqualTo("hashed-new-pass");
        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(token.isActive()).isFalse();
        verify(refreshTokenService).revokeAllForUser(user);
        verify(auditService).log(eq(AuditService.PASSWORD_RESET_COMPLETED), any(), any(), any(), any(), any(), any(), any());
        verify(auditService).log(eq(AuditService.SESSION_INVALIDATED_AFTER_PASSWORD_RESET), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("resetPassword rejects mismatched password confirmation")
    void resetPassword_mismatchedConfirmation_throws() {
        assertThatThrownBy(() ->
                service.resetPassword(resetReq("raw-token", "NewPass8!", "Different1!"), "127.0.0.1", "corr-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("do not match");

        verify(tokenRepository, never()).findByTokenHash(any());
    }

    @Test
    @DisplayName("resetPassword rejects unknown token")
    void resetPassword_unknownToken_throws() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.resetPassword(resetReq("bad-token", "NewPass8!", "NewPass8!"), "127.0.0.1", "corr-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    @DisplayName("resetPassword rejects expired token")
    void resetPassword_expiredToken_throws() {
        User user = activeUser(1L, "alice", "alice@x.com");
        PasswordResetToken expired = validToken(user, "raw-token");
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() ->
                service.resetPassword(resetReq("raw-token", "NewPass8!", "NewPass8!"), "127.0.0.1", "corr-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    @DisplayName("resetPassword rejects already-used token")
    void resetPassword_usedToken_throws() {
        User user = activeUser(1L, "alice", "alice@x.com");
        PasswordResetToken used = validToken(user, "raw-token");
        used.setUsedAt(LocalDateTime.now().minusMinutes(5));
        used.setActive(false);
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(used));

        assertThatThrownBy(() ->
                service.resetPassword(resetReq("raw-token", "NewPass8!", "NewPass8!"), "127.0.0.1", "corr-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    @DisplayName("resetPassword does not reactivate an inactive user and rejects the reset")
    void resetPassword_inactiveUser_rejectedWithoutReactivation() {
        User inactiveUser = activeUser(1L, "alice", "alice@x.com");
        inactiveUser.setActive(false);
        PasswordResetToken token = validToken(inactiveUser, "raw-token");
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() ->
                service.resetPassword(resetReq("raw-token", "NewPass8!", "NewPass8!"), "127.0.0.1", "corr-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid or has expired");

        assertThat(inactiveUser.isActive()).isFalse(); // never reactivated
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    // ----------------------------------------------------------------
    // adminInitiateReset
    // ----------------------------------------------------------------

    @Test
    @DisplayName("adminInitiateReset sets must_change_password=true and never exposes the current password")
    void adminInitiateReset_setsMustChangePasswordAndInvalidatesSessions() {
        User user = activeUser(1L, "alice", "alice@x.com");

        String resetLink = service.adminInitiateReset(user, "admin", "127.0.0.1");

        assertThat(resetLink).contains("/reset-password?token=");
        assertThat(user.isMustChangePassword()).isTrue();
        verify(tokenRepository).invalidateAllActiveForUser(user);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(refreshTokenService).revokeAllForUser(user);
        verify(auditService).log(eq(AuditService.ADMIN_PASSWORD_RESET_INITIATED), any(), any(), any(), any(), any(), any(), any());
        verify(auditService).log(eq(AuditService.SESSION_INVALIDATED_AFTER_PASSWORD_RESET), any(), any(), any(), any(), any(), any(), any());
        // Admin never receives/chooses the permanent password — only a link is returned.
        assertThat(resetLink).doesNotContain(user.getPasswordHash());
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private ForgotPasswordRequest req(String usernameOrEmail) {
        ForgotPasswordRequest r = new ForgotPasswordRequest();
        r.setUsernameOrEmail(usernameOrEmail);
        return r;
    }

    private ResetPasswordRequest resetReq(String token, String newPassword, String confirmPassword) {
        ResetPasswordRequest r = new ResetPasswordRequest();
        r.setToken(token);
        r.setNewPassword(newPassword);
        r.setConfirmPassword(confirmPassword);
        return r;
    }

    private User activeUser(Long id, String username, String email) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setPublicId("pub-" + id);
        u.setActive(true);
        u.setPasswordHash("old-hash");
        u.setMustChangePassword(false);
        return u;
    }

    private PasswordResetToken validToken(User user, String rawToken) {
        return PasswordResetToken.builder()
                .publicId("prt-pub-1")
                .user(user)
                .tokenHash(PasswordResetService.sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .active(true)
                .build();
    }
}
