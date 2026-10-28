package com.skillmatrix.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for user management behaviour.
 *
 * <pre>
 * user-management:
 *   hard-delete-enabled: true   # DEV default
 * </pre>
 *
 * Set to {@code false} in production to prevent permanent user deletion and
 * direct callers to use Deactivate instead.
 */
@Component
@ConfigurationProperties(prefix = "user-management")
@Getter
@Setter
public class UserManagementProperties {

    /**
     * When {@code false} every hard-delete request is rejected with HTTP 423
     * and the caller is advised to deactivate the user instead.
     * Defaults to {@code false} (production-safe).
     */
    private boolean hardDeleteEnabled = false;
}
