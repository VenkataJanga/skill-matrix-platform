package com.skillmatrix.requirements.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSkillRequirementRequest(

        @NotNull(message = "expectedLevel is required")
        @Min(value = 1, message = "expectedLevel must be between 1 and 5")
        @Max(value = 5, message = "expectedLevel must be between 1 and 5")
        Integer expectedLevel,

        @NotNull(message = "mandatory is required")
        Boolean mandatory,

        String criticalityCode,

        String roleCode,

        String gradeCode
) {}
