package com.skillmatrix.requirements.dto;

import com.skillmatrix.domain.entity.RequirementVersion;

import java.time.LocalDateTime;

public record RequirementVersionDto(
        String publicId,
        String applicationPublicId,
        String applicationCode,
        String applicationName,
        String versionCode,
        String versionName,
        String description,
        String statusCode,
        String statusLabel,
        LocalDateTime publishedAt,
        LocalDateTime approvedAt,
        String approvedBy,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy,
        long activeRequirementCount
) {
    public static RequirementVersionDto from(RequirementVersion rv) {
        return from(rv, 0L);
    }

    /**
     * Preferred factory — includes the count of ACTIVE skill requirements for this
     * version so the Assessment Cycle UI can warn Admin before cycle creation when
     * a version has zero active requirements. Does not affect backend Open-time
     * validation in AssessmentCycleService, which remains unchanged.
     */
    public static RequirementVersionDto from(RequirementVersion rv, long activeRequirementCount) {
        var app = rv.getApplication();
        var status = rv.getStatusLookup();
        return new RequirementVersionDto(
                rv.getPublicId(),
                app.getPublicId(),
                app.getApplicationCode(),
                app.getApplicationName(),
                rv.getVersionCode(),
                rv.getVersionName(),
                rv.getDescription(),
                status != null ? status.getValueCode() : null,
                status != null ? status.getValueLabel() : null,
                rv.getPublishedAt(),
                rv.getApprovedAt(),
                rv.getApprovedBy(),
                rv.isActive(),
                rv.getCreatedAt(),
                rv.getUpdatedAt(),
                rv.getCreatedBy(),
                rv.getUpdatedBy(),
                activeRequirementCount
        );
    }
}
