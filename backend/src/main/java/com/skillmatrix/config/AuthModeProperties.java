package com.skillmatrix.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Authentication mode toggle — Okta-readiness scaffold ONLY.
 *
 * <pre>
 * app:
 *   auth:
 *     mode: LOCAL   # LOCAL (dev/pilot/demo) | OKTA (future production target)
 * </pre>
 *
 * <p>This property does not implement Okta/Corporate SSO integration. It
 * exists solely so that local-authentication-only features (Forgot Password,
 * Admin password reset) can later be conditionally disabled when a future
 * Okta integration sets {@code app.auth.mode=OKTA}, without requiring any
 * change to User Master, RBAC, application assignments, Skill Management,
 * Assessment workflow, or authorization rules.
 *
 * <p>See {@code docs/AUTH_ARCHITECTURE.md} for the full production-readiness
 * design note.
 */
@Component
@ConfigurationProperties(prefix = "app.auth")
@Getter
@Setter
public class AuthModeProperties {

    /** Current authentication mode. Only {@code LOCAL} is implemented today. */
    private String mode = "LOCAL";

    public boolean isLocalModeActive() {
        return "LOCAL".equalsIgnoreCase(mode);
    }
}
