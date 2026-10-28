package com.skillmatrix.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /api/v1/auth/reset-password.
 * The reset {@code token} is the raw, one-time, short-lived value delivered
 * to the user (never the JWT access token).
 */
@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
