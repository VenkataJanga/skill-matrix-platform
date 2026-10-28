package com.skillmatrix.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the development/pilot Forgot Password flow.
 *
 * <pre>
 * app:
 *   password-reset:
 *     expiry-minutes: 20
 *     dev-expose-token: false   # ONLY ever true under the 'dev' profile
 * </pre>
 *
 * <p>{@code devExposeToken} must remain {@code false} outside the {@code dev}
 * profile. It is additionally guarded in code by checking the active Spring
 * profile before ever including the raw token/link in an API response — this
 * property alone is not sufficient authorization to expose it.
 */
@Component
@ConfigurationProperties(prefix = "app.password-reset")
@Getter
@Setter
public class PasswordResetProperties {

    /** Reset token validity window, in minutes. Recommended 15-30. */
    private int expiryMinutes = 20;

    /**
     * DEV-ONLY convenience flag: when true (and only possible under the
     * 'dev' profile — see {@code application-dev.yml}), the forgot-password
     * response additionally includes the raw reset link so local testing
     * does not require a working SMTP server. Never enabled in qa/prod.
     */
    private boolean devExposeToken = false;
}
