package com.skillmatrix.skills.dto;

import com.skillmatrix.domain.entity.Skill;

import java.time.LocalDateTime;

public record SkillDto(
        String publicId,
        String skillCode,
        String skillName,
        String description,
        String categoryCode,
        String categoryName,
        String skillTypeCode,
        String skillTypeLabel,
        String scopeCode,
        String scopeLabel,
        String applicationTypeCode,
        String applicationTypePublicId,
        String bundleCode,
        String bundlePublicId,
        String applicationCode,
        String applicationPublicId,
        String applicationName,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public static SkillDto from(Skill s) {
        var cat = s.getSkillCategory();
        var type = s.getSkillTypeLookup();
        var scope = s.getSkillScopeLookup();
        var appType = s.getApplicationType();
        var bundle = s.getBundle();
        var app = s.getApplication();
        return new SkillDto(
                s.getPublicId(),
                s.getSkillCode(),
                s.getSkillName(),
                s.getDescription(),
                cat.getCategoryCode(),
                cat.getCategoryName(),
                type != null ? type.getValueCode() : null,
                type != null ? type.getValueLabel() : null,
                scope != null ? scope.getValueCode() : null,
                scope != null ? scope.getValueLabel() : null,
                appType != null ? appType.getTypeCode() : null,
                appType != null ? appType.getPublicId() : null,
                bundle != null ? bundle.getBundleCode() : null,
                bundle != null ? bundle.getPublicId() : null,
                app != null ? app.getApplicationCode() : null,
                app != null ? app.getPublicId() : null,
                app != null ? app.getApplicationName() : null,
                s.isActive(),
                s.getCreatedAt(),
                s.getUpdatedAt(),
                s.getCreatedBy(),
                s.getUpdatedBy()
        );
    }
}
