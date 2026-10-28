package com.skillmatrix.assessments.dto;

import com.skillmatrix.domain.entity.AssessmentCycle;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Admin-facing DTO for an AssessmentCycle. Not used by Technician endpoints.
 */
public record AssessmentCycleDto(
        String publicId,
        String applicationPublicId,
        String applicationCode,
        String applicationName,
        String requirementVersionPublicId,
        String requirementVersionCode,
        String cycleName,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        String statusCode,
        String statusLabel,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public static AssessmentCycleDto from(AssessmentCycle c) {
        var app = c.getApplication();
        var rv = c.getRequirementVersion();
        var status = c.getStatusLookup();
        return new AssessmentCycleDto(
                c.getPublicId(),
                app.getPublicId(),
                app.getApplicationCode(),
                app.getApplicationName(),
                rv.getPublicId(),
                rv.getVersionCode(),
                c.getCycleName(),
                c.getCycleStartDate(),
                c.getCycleEndDate(),
                status != null ? status.getValueCode() : null,
                status != null ? status.getValueLabel() : null,
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getCreatedBy(),
                c.getUpdatedBy()
        );
    }
}
