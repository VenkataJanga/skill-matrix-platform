package com.skillmatrix.requirements.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRequirementVersionRequest(

        @NotBlank(message = "applicationPublicId is required")
        String applicationPublicId,

        @NotBlank(message = "versionCode is required")
        @Size(max = 100, message = "versionCode max 100 chars")
        String versionCode,

        @NotBlank(message = "versionName is required")
        @Size(max = 200, message = "versionName max 200 chars")
        String versionName,

        String description
) {}
