package com.skillmatrix.skills.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSkillRequest(

        @NotBlank(message = "skillCode is required")
        @Size(max = 100, message = "skillCode max 100 chars")
        String skillCode,

        @NotBlank(message = "skillName is required")
        @Size(max = 200, message = "skillName max 200 chars")
        String skillName,

        String description,

        @NotBlank(message = "categoryCode is required")
        String categoryCode,

        /** SKILL_TYPE lookup value code: TECHNICAL / NON_TECHNICAL */
        String skillTypeCode,

        /** SKILL_SCOPE lookup value code: GLOBAL / APPLICATION_TYPE / BUNDLE / APPLICATION / GENERIC */
        @NotBlank(message = "scopeCode is required")
        String scopeCode,

        /** Required when scopeCode = APPLICATION_TYPE */
        String applicationTypeCode,

        /** Required when scopeCode = BUNDLE */
        String bundleCode,

        /** Required when scopeCode = APPLICATION — publicId of the application */
        String applicationPublicId
) {}
