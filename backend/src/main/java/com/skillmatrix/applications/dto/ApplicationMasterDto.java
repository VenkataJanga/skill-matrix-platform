package com.skillmatrix.applications.dto;

import com.skillmatrix.domain.entity.Application;

import java.time.LocalDateTime;

/** Full Admin-facing Application Master detail DTO (M4 Application Master enhancement). */
public record ApplicationMasterDto(
        String publicId,
        String applicationCode,
        String applicationName,
        String description,
        String remarks,
        String portfolioCode,
        String portfolioName,
        String typeCode,
        String typeName,
        String bundleCode,
        String bundleName,
        String lifecycleStatusCode,
        String lifecycleStatusLabel,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public static ApplicationMasterDto from(Application entity) {
        var portfolio = entity.getPortfolio();
        var lifecycle = entity.getLifecycleStatusLookup();
        return new ApplicationMasterDto(
                entity.getPublicId(),
                entity.getApplicationCode(),
                entity.getApplicationName(),
                entity.getDescription(),
                entity.getRemarks(),
                portfolio.getPortfolioCode(),
                portfolio.getPortfolioName(),
                portfolio.getApplicationType().getTypeCode(),
                portfolio.getApplicationType().getTypeName(),
                portfolio.getBundle().getBundleCode(),
                portfolio.getBundle().getBundleName(),
                lifecycle != null ? lifecycle.getValueCode() : null,
                lifecycle != null ? lifecycle.getValueLabel() : null,
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy()
        );
    }
}
