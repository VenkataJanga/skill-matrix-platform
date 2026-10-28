package com.skillmatrix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

/**
 * General application beans — separated from {@link SecurityConfig} to break
 * the circular dependency:
 * JwtAuthenticationFilter → AuthService → PasswordEncoder → SecurityConfig → JwtAuthenticationFilter
 */
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Injectable system clock — allows business-date comparisons (e.g. assessment
     * cycle date-window enforcement) to be deterministically tested with a fixed
     * Clock instead of relying on the real system date.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
