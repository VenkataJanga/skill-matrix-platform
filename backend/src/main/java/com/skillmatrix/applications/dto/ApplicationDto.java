package com.skillmatrix.applications.dto;

import com.skillmatrix.domain.entity.Application;

public record ApplicationDto(
        String publicId,
        String applicationCode,
        String applicationName,
        String description,
        String portfolioCode,
        String portfolioName,
        String typeCode,
        String typeName,
        String bundleCode,
        String bundleName
) {
    public static ApplicationDto from(Application entity) {
        var portfolio = entity.getPortfolio();
        return new ApplicationDto(
                entity.getPublicId(),
                entity.getApplicationCode(),
                entity.getApplicationName(),
                entity.getDescription(),
                portfolio.getPortfolioCode(),
                portfolio.getPortfolioName(),
                portfolio.getApplicationType().getTypeCode(),
                portfolio.getApplicationType().getTypeName(),
                portfolio.getBundle().getBundleCode(),
                portfolio.getBundle().getBundleName()
        );
    }
}
