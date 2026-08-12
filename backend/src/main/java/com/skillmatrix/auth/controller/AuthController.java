package com.skillmatrix.auth.controller;

import com.skillmatrix.auth.dto.*;
import com.skillmatrix.auth.service.AuthService;
import com.skillmatrix.common.dto.ApiResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints — login, refresh, logout, change-password, me.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, token refresh and password management")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // ----------------------------------------------------------------
    // POST /api/v1/auth/login
    // ----------------------------------------------------------------

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ip = resolveIp(httpRequest);
        String correlationId = resolveCorrelationId(httpRequest);

        LoginResponse response = authService.login(request, ip, correlationId);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    // ----------------------------------------------------------------
    // POST /api/v1/auth/refresh
    // ----------------------------------------------------------------

    @PostMapping("/refresh")
    @Operation(summary = "Obtain a new access token using a refresh token")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {

        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ----------------------------------------------------------------
    // POST /api/v1/auth/logout
    // ----------------------------------------------------------------

    @PostMapping("/logout")
    @Operation(summary = "Logout — revoke all refresh tokens",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        User user = loadUser(principal);
        authService.logout(user, resolveIp(httpRequest), resolveCorrelationId(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    // ----------------------------------------------------------------
    // POST /api/v1/auth/change-password
    // ----------------------------------------------------------------

    @PostMapping("/change-password")
    @Operation(summary = "Change password (required when must_change_password=true)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        User user = loadUser(principal);
        authService.changePassword(user, request, resolveIp(httpRequest), resolveCorrelationId(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    // ----------------------------------------------------------------
    // GET /api/v1/auth/me
    // ----------------------------------------------------------------

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserMeResponse>> me(
            @AuthenticationPrincipal UserPrincipal principal) {

        UserMeResponse response = authService.getMe(principal.getPublicId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private User loadUser(UserPrincipal principal) {
        return userRepository.findByPublicId(principal.getPublicId())
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String id = request.getHeader("X-Correlation-Id");
        return (id != null && !id.isBlank()) ? id : java.util.UUID.randomUUID().toString();
    }
}
