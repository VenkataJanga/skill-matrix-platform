package com.skillmatrix.applications.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Application/Type/Bundle/Portfolio hierarchy fields are intentionally included so
 * the Admin can correct a misclassified Application — but changing hierarchy is
 * blocked by the service once the Application has business/history dependencies
 * (mirrors the Assessment Cycle "lock after headers exist" pattern).
 */
public record UpdateApplicationRequest(
        String applicationTypeCode,
        String bundleCode,
        String portfolioCode,

        @NotBlank(message = "applicationName is required")
        String applicationName,

        String lifecycleStatusCode,

        String description,

        String remarks
) {
}
