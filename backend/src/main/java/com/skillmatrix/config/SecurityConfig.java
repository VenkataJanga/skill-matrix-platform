package com.skillmatrix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline Spring Security configuration.
 *
 * <p>This is a foundation config that:
 * <ul>
 *   <li>Disables CSRF (stateless JWT API)</li>
 *   <li>Sets session management to STATELESS</li>
 *   <li>Permits Swagger UI, API docs, and Actuator health endpoints publicly</li>
 *   <li>Requires authentication for all other endpoints</li>
 *   <li>Provides a BCrypt password encoder bean</li>
 * </ul>
 *
 * <p>JWT filter chain, auth entry point, and RBAC method security will be added in M17.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        // Auth
        "/api/v1/auth/**",
        // Swagger / OpenAPI
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/api-docs",
        "/api-docs/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        // Actuator health (public)
        "/actuator/health",
        "/actuator/info"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — REST API uses JWT, not cookies for session
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session — no HTTP session created or used
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * BCrypt password encoder — used for hashing user passwords.
     * Strength 12 balances security and performance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
