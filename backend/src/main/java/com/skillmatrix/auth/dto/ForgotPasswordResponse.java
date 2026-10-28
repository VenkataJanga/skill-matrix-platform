package com.skillmatrix.auth.dto;

/**
 * Always-generic response for POST /api/v1/auth/forgot-password.
 *
 * <p>Deliberately identical whether the account exists, is inactive, or is
 * unknown — prevents account enumeration. {@code devResetLink} is populated
 * ONLY under the dev profile with {@code app.password-reset.dev-expose-token=true},
 * and is never populated in any other environment.
 */
public record ForgotPasswordResponse(
        String message,
        String devResetLink
) {
    private static final String GENERIC_MESSAGE =
            "If an active account matches the information provided, password reset instructions will be sent to the registered email address.";

    public static ForgotPasswordResponse generic() {
        return new ForgotPasswordResponse(GENERIC_MESSAGE, null);
    }

    public static ForgotPasswordResponse genericWithDevLink(String devResetLink) {
        return new ForgotPasswordResponse(GENERIC_MESSAGE, devResetLink);
    }
}
