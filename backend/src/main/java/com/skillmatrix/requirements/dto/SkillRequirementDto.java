package com.skillmatrix.requirements.dto;

import com.skillmatrix.domain.entity.ExpectedRating;

import java.time.LocalDateTime;

/**
 * Admin-facing DTO for a skill requirement row (ExpectedRating).
 * IMPORTANT: This DTO exposes expectedLevel and is intended for ADMIN
 * requirement configuration ONLY. It must never be reused for any future
 * technician-facing assessment DTO (see M3.1 security/business rule).
 */
public record SkillRequirementDto(
        String publicId,
        String requirementVersionPublicId,
        String versionCode,
        String applicationPublicId,
        String applicationCode,
        String applicationName,
        String skillPublicId,
        String skillCode,
        String skillName,
        String skillGroup,          // skill scope code from Skill Master (source of truth)
        String categoryCode,
        String categoryName,
        int expectedLevel,
        boolean mandatory,
        String criticalityCode,
        String criticalityLabel,
        String roleCode,
        String roleLabel,
        String gradeCode,
        String gradeLabel,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public static SkillRequirementDto from(ExpectedRating er) {
        var rv = er.getRequirementVersion();
        var app = rv.getApplication();
        var skill = er.getSkill();
        var scope = skill.getSkillScopeLookup();
        var cat = skill.getSkillCategory();
        var crit = er.getCriticalityLookup();
        var role = er.getRoleLookup();
        var grade = er.getGradeLookup();
        return new SkillRequirementDto(
                er.getPublicId(),
                rv.getPublicId(),
                rv.getVersionCode(),
                app.getPublicId(),
                app.getApplicationCode(),
                app.getApplicationName(),
                skill.getPublicId(),
                skill.getSkillCode(),
                skill.getSkillName(),
                scope != null ? scope.getValueCode() : null,
                cat != null ? cat.getCategoryCode() : null,
                cat != null ? cat.getCategoryName() : null,
                er.getExpectedLevel(),
                er.isMandatory(),
                crit != null ? crit.getValueCode() : null,
                crit != null ? crit.getValueLabel() : null,
                role != null ? role.getValueCode() : null,
                role != null ? role.getValueLabel() : null,
                grade != null ? grade.getValueCode() : null,
                grade != null ? grade.getValueLabel() : null,
                er.isActive(),
                er.getCreatedAt(),
                er.getUpdatedAt(),
                er.getCreatedBy(),
                er.getUpdatedBy()
        );
    }
}
