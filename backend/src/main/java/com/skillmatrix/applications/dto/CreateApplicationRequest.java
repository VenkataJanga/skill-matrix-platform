package com.skillmatrix.applications.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateApplicationRequest(
        @NotBlank(message = "applicationTypeCode is required")
        String applicationTypeCode,

        @NotBlank(message = "bundleCode is required")
        String bundleCode,

        @NotBlank(message = "portfolioCode is required")
        String portfolioCode,

        @NotBlank(message = "applicationCode is required")
        String applicationCode,

        @NotBlank(message = "applicationName is required")
        String applicationName,

        /** Optional — defaults to ACTIVE when blank. */
        String lifecycleStatusCode,

        String description,

        String remarks
) {
}
