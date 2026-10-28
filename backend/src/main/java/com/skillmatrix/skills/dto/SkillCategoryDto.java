package com.skillmatrix.skills.dto;

import com.skillmatrix.domain.entity.SkillCategory;

public record SkillCategoryDto(
        String publicId,
        String categoryCode,
        String categoryName,
        String description,
        int displayOrder
) {
    public static SkillCategoryDto from(SkillCategory sc) {
        return new SkillCategoryDto(
                sc.getPublicId(),
                sc.getCategoryCode(),
                sc.getCategoryName(),
                sc.getDescription(),
                sc.getDisplayOrder()
        );
    }
}
