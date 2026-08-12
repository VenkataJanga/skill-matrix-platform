package com.skillmatrix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}
