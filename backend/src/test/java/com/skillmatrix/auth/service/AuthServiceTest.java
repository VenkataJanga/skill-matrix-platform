package com.skillmatrix.auth.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.auth.dto.*;
import com.skillmatrix.common.exception.AuthException;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.*;
import com.skillmatrix.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — unit tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder()
                .id(1L).publicId("role-pub-1").roleCode("ADMIN").roleName("Administrator")
                .active(true).rolePermissions(new HashSet<>()).build();

        UserRole userRole = UserRole.builder()
                .id(1L).role(adminRole).active(true).build();

        activeUser = User.builder()
                .id(1L).publicId("user-pub-1").username("alice")
                .email("alice@example.com").passwordHash("$2a$12$hashed")
                .fullName("Alice Smith").active(true).accountLocked(false)
                .mustChangePassword(false).failedLoginAttempts(0)
                .userRoles(new HashSet<>(Set.of(userRole))).build();

        userRole.setUser(activeUser);
    }

    // ----------------------------------------------------------------
    // Login — success
    // ----------------------------------------------------------------

    @Test
    @DisplayName("login succeeds for valid credentials")
    void login_successForValidCredentials() {
        when(userRepository.findByUsernameWithRolesAndPermissions("alice"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password", "$2a$12$hashed")).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("access-token");
        when(jwtService.getExpirationMs()).thenReturn(900_000L);
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");
        when(userRepository.save(any())).thenReturn(activeUser);

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("password");

        LoginResponse resp = authService.login(req, "127.0.0.1", "corr-1");

        assertThat(resp.getAccessToken()).isEqualTo("access-token");
        assertThat(resp.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(resp.getUsername()).isEqualTo("alice");
        assertThat(resp.getUserPublicId()).isEqualTo("user-pub-1");
        assertThat(resp.isMustChangePassword()).isFalse();
    }

    // ----------------------------------------------------------------
    // Login — failures
    // ----------------------------------------------------------------

    @Test
    @DisplayName("login throws AuthException for unknown username")
    void login_throwsForUnknownUser() {
        when(userRepository.findByUsernameWithRolesAndPermissions("unknown"))
                .thenReturn(Optional.empty());
        doNothing().when(auditService).logAuth(any(), any(), any(), any(), any());

        LoginRequest req = new LoginRequest();
        req.setUsername("unknown");
        req.setPassword("any");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "corr-1"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("login throws AuthException for inactive user")
    void login_throwsForInactiveUser() {
        activeUser.setActive(false);
        when(userRepository.findByUsernameWithRolesAndPermissions("alice"))
                .thenReturn(Optional.of(activeUser));
        doNothing().when(auditService).logAuth(any(), any(), any(), any(), any());

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("password");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "corr-1"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("login throws AuthException for locked account")
    void login_throwsForLockedAccount() {
        activeUser.setAccountLocked(true);
        when(userRepository.findByUsernameWithRolesAndPermissions("alice"))
                .thenReturn(Optional.of(activeUser));
        doNothing().when(auditService).logAuth(any(), any(), any(), any(), any());

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("password");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "corr-1"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("locked");
    }

    @Test
    @DisplayName("login throws AuthException for wrong password and increments failed attempts")
    void login_throwsAndIncrementsFailedAttempts() {
        when(userRepository.findByUsernameWithRolesAndPermissions("alice"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong", "$2a$12$hashed")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(activeUser);
        doNothing().when(auditService).logAuth(any(), any(), any(), any(), any());

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "corr-1"))
                .isInstanceOf(AuthException.class);

        assertThat(activeUser.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("account locks after 5 failed attempts")
    void login_locksAccountAfter5FailedAttempts() {
        activeUser.setFailedLoginAttempts(4); // already 4 failures
        when(userRepository.findByUsernameWithRolesAndPermissions("alice"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(activeUser);
        doNothing().when(auditService).logAuth(any(), any(), any(), any(), any());

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "corr-1"))
                .isInstanceOf(AuthException.class);

        assertThat(activeUser.isAccountLocked()).isTrue();
        assertThat(activeUser.getFailedLoginAttempts()).isEqualTo(5);
    }

    // ----------------------------------------------------------------
    // Change Password
    // ----------------------------------------------------------------

    @Test
    @DisplayName("changePassword succeeds with valid request")
    void changePassword_succeeds() {
        when(passwordEncoder.matches("oldPass", "$2a$12$hashed")).thenReturn(true);
        when(passwordEncoder.matches("newPass8!", "$2a$12$hashed")).thenReturn(false);
        when(passwordEncoder.encode("newPass8!")).thenReturn("$2a$12$newhash");
        when(userRepository.save(any())).thenReturn(activeUser);
        doNothing().when(refreshTokenService).revokeAllForUser(any());
        doNothing().when(auditService).logAuth(any(), any(), any(), any(), any());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldPass");
        req.setNewPassword("newPass8!");
        req.setConfirmPassword("newPass8!");

        authService.changePassword(activeUser, req, "127.0.0.1", "corr-1");

        assertThat(activeUser.getPasswordHash()).isEqualTo("$2a$12$newhash");
        assertThat(activeUser.isMustChangePassword()).isFalse();
        verify(refreshTokenService).revokeAllForUser(activeUser);
    }

    @Test
    @DisplayName("changePassword throws when current password is wrong")
    void changePassword_throwsForWrongCurrentPassword() {
        when(passwordEncoder.matches("wrongOld", "$2a$12$hashed")).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrongOld");
        req.setNewPassword("newPass8!");
        req.setConfirmPassword("newPass8!");

        assertThatThrownBy(() -> authService.changePassword(activeUser, req, "127.0.0.1", "c"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    @DisplayName("changePassword throws when new password does not match confirmation")
    void changePassword_throwsForMismatchedConfirmation() {
        when(passwordEncoder.matches("oldPass", "$2a$12$hashed")).thenReturn(true);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldPass");
        req.setNewPassword("newPass8!");
        req.setConfirmPassword("differentPass!");

        assertThatThrownBy(() -> authService.changePassword(activeUser, req, "127.0.0.1", "c"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    @DisplayName("changePassword throws when new password equals current password")
    void changePassword_throwsWhenNewPasswordSameAsOld() {
        when(passwordEncoder.matches("samePass", "$2a$12$hashed")).thenReturn(true);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("samePass");
        req.setNewPassword("samePass");
        req.setConfirmPassword("samePass");

        assertThatThrownBy(() -> authService.changePassword(activeUser, req, "127.0.0.1", "c"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("different");
    }

    // ----------------------------------------------------------------
    // Logout
    // ----------------------------------------------------------------

    @Test
    @DisplayName("logout revokes tokens and writes audit")
    void logout_revokesTokensAndWritesAudit() {
        doNothing().when(refreshTokenService).revokeAllForUser(any());
        doNothing().when(auditService).logAuth(any(), any(), any(), any(), any());

        authService.logout(activeUser, "127.0.0.1", "corr-1");

        verify(refreshTokenService).revokeAllForUser(activeUser);
        verify(auditService).logAuth(
                eq(AuditService.LOGOUT),
                eq(activeUser.getId()),
                eq("alice"),
                eq("127.0.0.1"),
                eq("corr-1"));
    }
}
