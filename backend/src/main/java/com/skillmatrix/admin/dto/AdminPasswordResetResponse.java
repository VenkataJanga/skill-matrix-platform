package com.skillmatrix.admin.dto;

/**
 * Response for the Admin-assisted password reset endpoint.
 * NEVER contains the user's password — only confirmation and, under the
 * dev profile only, the raw reset link for local testing convenience.
 */
public record AdminPasswordResetResponse(
        String userPublicId,
        String username,
        String message,
        String devResetLink
) {
    public static AdminPasswordResetResponse of(String userPublicId, String username, String devResetLink) {
        return new AdminPasswordResetResponse(
                userPublicId,
                username,
                "Password reset initiated. The user must use the reset link (or sign in and change their password) to set a new password.",
                devResetLink);
    }
}
