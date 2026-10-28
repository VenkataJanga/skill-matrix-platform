package com.skillmatrix.skills.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSkillRequest(

        @NotBlank(message = "skillName is required")
        @Size(max = 200, message = "skillName max 200 chars")
        String skillName,

        String description,

        @NotBlank(message = "categoryCode is required")
        String categoryCode,

        String skillTypeCode,

        @NotBlank(message = "scopeCode is required")
        String scopeCode,

        String applicationTypeCode,
        String bundleCode,
        String applicationPublicId
) {}
