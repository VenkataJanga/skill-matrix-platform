package com.skillmatrix.requirements.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSkillRequirementRequest(

        @NotBlank(message = "requirementVersionPublicId is required")
        String requirementVersionPublicId,

        @NotBlank(message = "skillPublicId is required")
        String skillPublicId,

        @NotNull(message = "expectedLevel is required")
        @Min(value = 1, message = "expectedLevel must be between 1 and 5")
        @Max(value = 5, message = "expectedLevel must be between 1 and 5")
        Integer expectedLevel,

        @NotNull(message = "mandatory is required")
        Boolean mandatory,

        /** Optional CRITICALITY lookup value code. */
        String criticalityCode,

        /** Optional role code — reserved for future use. */
        String roleCode,

        /** Optional grade code — reserved for future use. */
        String gradeCode
) {}
