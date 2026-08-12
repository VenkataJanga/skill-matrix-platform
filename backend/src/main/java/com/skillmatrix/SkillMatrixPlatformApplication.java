package com.skillmatrix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Skill Matrix Platform — Spring Boot entry point.
 *
 * <p>{@code @EnableAsync} activates the {@code @Async} audit log writer
 * in {@link com.skillmatrix.audit.service.AuditService}.
 */
@SpringBootApplication
@EnableAsync
public class SkillMatrixPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillMatrixPlatformApplication.class, args);
    }
}
