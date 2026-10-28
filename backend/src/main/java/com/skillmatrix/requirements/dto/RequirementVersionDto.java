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
        String updatedBy
) {
    public static RequirementVersionDto from(RequirementVersion rv) {
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
                rv.getUpdatedBy()
        );
    }
}
