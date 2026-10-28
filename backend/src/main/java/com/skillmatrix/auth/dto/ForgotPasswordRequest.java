package com.skillmatrix.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /api/v1/auth/forgot-password.
 * Accepts either the username/employee-identifier or the registered email.
 */
@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;
}
