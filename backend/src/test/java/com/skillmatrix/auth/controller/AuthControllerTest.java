package com.skillmatrix.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatrix.auth.dto.*;
import com.skillmatrix.auth.service.AuthService;
import com.skillmatrix.auth.service.JwtService;
import com.skillmatrix.common.exception.AuthException;
import com.skillmatrix.domain.entity.*;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link AuthController}.
 *
 * <p>{@code .with(csrf())} is required on POST requests in {@code @WebMvcTest}
 * because MockMvc enables CSRF by default in the test slice, regardless of the
 * production security config. Adding it ensures we test the right HTTP status
 * codes rather than getting CSRF 403s.
 */
@WebMvcTest(value = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import({com.skillmatrix.config.SecurityConfig.class, com.skillmatrix.config.AppConfig.class})
@DisplayName("AuthController — MockMvc tests")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean UserRepository userRepository;

    private User testUser;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        Role role = Role.builder()
                .id(1L).publicId("role-pub-1").roleCode("ADMIN").roleName("Administrator")
                .active(true).rolePermissions(new HashSet<>()).build();

        UserRole userRole = UserRole.builder().id(1L).role(role).active(true).build();

        testUser = User.builder()
                .id(1L).publicId("user-pub-1").username("alice")
                .email("alice@example.com").passwordHash("$2a$12$hash")
                .fullName("Alice Smith").active(true).accountLocked(false)
                .mustChangePassword(false)
                .userRoles(new HashSet<>(Set.of(userRole))).build();

        userRole.setUser(testUser);
        testPrincipal = new UserPrincipal(testUser);
    }

    // ----------------------------------------------------------------
    // POST /api/v1/auth/login
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /login returns 200 with tokens on valid credentials")
    void login_returns200WithTokens() throws Exception {
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken("access-jwt").refreshToken("refresh-uuid")
                .tokenType("Bearer").expiresIn(900)
                .userPublicId("user-pub-1").username("alice")
                .fullName("Alice Smith").roles(List.of("ADMIN"))
                .mustChangePassword(false).build();

        when(authService.login(any(), any(), any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"Password1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-uuid"))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.mustChangePassword").value(false));
    }

    @Test
    @DisplayName("POST /login returns 400 for missing username")
    void login_returns400ForMissingUsername() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Password1!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /login returns 401 for invalid credentials")
    void login_returns401ForInvalidCredentials() throws Exception {
        when(authService.login(any(), any(), any()))
                .thenThrow(new AuthException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    // ----------------------------------------------------------------
    // POST /api/v1/auth/refresh
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /refresh returns 200 with new access token")
    void refresh_returns200WithNewToken() throws Exception {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("new-access-jwt").tokenType("Bearer").expiresIn(900).build();

        when(authService.refresh(any())).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"valid-refresh-uuid\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-jwt"));
    }

    @Test
    @DisplayName("POST /refresh returns 400 for missing refresh token")
    void refresh_returns400ForMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------
    // POST /api/v1/auth/logout
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /logout returns 200 for authenticated user")
    void logout_returns200ForAuthenticatedUser() throws Exception {
        when(userRepository.findByPublicId("user-pub-1")).thenReturn(Optional.of(testUser));
        doNothing().when(authService).logout(any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(user(testPrincipal))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @DisplayName("POST /logout returns 401 without authentication")
    void logout_returns401WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    // POST /api/v1/auth/change-password
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /change-password returns 200 on valid request")
    void changePassword_returns200OnValidRequest() throws Exception {
        when(userRepository.findByPublicId("user-pub-1")).thenReturn(Optional.of(testUser));
        doNothing().when(authService).changePassword(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(user(testPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPass1!\",\"newPassword\":\"NewPass8!\",\"confirmPassword\":\"NewPass8!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("POST /change-password returns 400 for short new password")
    void changePassword_returns400ForShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(user(testPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPass1!\",\"newPassword\":\"short\",\"confirmPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ----------------------------------------------------------------
    // GET /api/v1/auth/me
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /me returns 200 with user profile for authenticated user")
    void me_returns200WithProfile() throws Exception {
        UserMeResponse meResponse = UserMeResponse.builder()
                .publicId("user-pub-1").username("alice")
                .email("alice@example.com").fullName("Alice Smith")
                .roles(List.of("ADMIN"))
                .permissions(List.of("USER_VIEW", "USER_CREATE"))
                .mustChangePassword(false).build();

        when(authService.getMe("user-pub-1")).thenReturn(meResponse);

        mockMvc.perform(get("/api/v1/auth/me")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.publicId").value("user-pub-1"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /me returns 401 without authentication")
    void me_returns401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
