package com.skillmatrix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Skill Matrix Platform — Spring Boot entry point.
 *
 * <p>Stack: Spring Boot 3.3.x · Java 21 · MySQL 8 · Flyway · Spring Security (JWT)
 *
 * <p>Profiles: dev | qa | prod
 * Set via: --spring.profiles.active=dev  or  SPRING_PROFILES_ACTIVE=dev
 */
@SpringBootApplication
public class SkillMatrixPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillMatrixPlatformApplication.class, args);
    }
}
