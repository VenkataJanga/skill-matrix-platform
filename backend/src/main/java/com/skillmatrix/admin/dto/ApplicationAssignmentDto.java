package com.skillmatrix.admin.dto;

import com.skillmatrix.domain.entity.UserApplicationMapping;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationAssignmentDto(
        String publicId,
        String userPublicId,
        String username,
        String fullName,
        String applicationPublicId,
        String applicationCode,
        String applicationName,
        String typeCode,
        String bundleCode,
        BigDecimal allocationPercentage,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        String roleOnAppCode,
        boolean primary
) {
    public static ApplicationAssignmentDto from(UserApplicationMapping m) {
        var app = m.getApplication();
        var portfolio = app.getPortfolio();
        var roleLookup = m.getRoleOnAppLookup();
        String roleCode = roleLookup != null ? roleLookup.getValueCode() : null;
        return new ApplicationAssignmentDto(
                m.getPublicId(),
                m.getUser().getPublicId(),
                m.getUser().getUsername(),
                m.getUser().getFullName(),
                app.getPublicId(),
                app.getApplicationCode(),
                app.getApplicationName(),
                portfolio.getApplicationType().getTypeCode(),
                portfolio.getBundle().getBundleCode(),
                m.getAllocationPercentage(),
                m.getEffectiveFrom(),
                m.getEffectiveTo(),
                m.isActive(),
                roleCode,
                "PRIMARY".equals(roleCode)
        );
    }
}
