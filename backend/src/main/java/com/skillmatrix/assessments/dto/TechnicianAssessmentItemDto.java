package com.skillmatrix.assessments.dto;

import com.skillmatrix.domain.entity.AssessmentItem;

/**
 * TECHNICIAN-FACING DTO ONLY.
 *
 * CRITICAL SECURITY RULE: this record intentionally has NO field for
 * expectedLevel / expectedRating / targetLevel / requiredLevel / gap.
 * Do not add one. Expected level must never reach the technician
 * payload — it remains server-side only (read via ExpectedRating on
 * the AssessmentItem entity, used solely for internal validation).
 */
public record TechnicianAssessmentItemDto(
        String assessmentItemPublicId,
        String skillPublicId,
        String skillCode,
        String skillName,
        String categoryCode,
        String categoryName,
        String skillGroup,
        boolean mandatory,
        Integer selfRating,
        String comment,
        String evidence
) {
    public static TechnicianAssessmentItemDto from(AssessmentItem item) {
        var skill = item.getExpectedRating().getSkill();
        var cat = skill.getSkillCategory();
        var scope = skill.getSkillScopeLookup();
        return new TechnicianAssessmentItemDto(
                item.getPublicId(),
                skill.getPublicId(),
                skill.getSkillCode(),
                skill.getSkillName(),
                cat != null ? cat.getCategoryCode() : null,
                cat != null ? cat.getCategoryName() : null,
                scope != null ? scope.getValueCode() : null,
                item.getExpectedRating().isMandatory(),
                item.getSelfRating(),
                item.getTechnicianComment(),
                item.getEvidence()
        );
    }
}
