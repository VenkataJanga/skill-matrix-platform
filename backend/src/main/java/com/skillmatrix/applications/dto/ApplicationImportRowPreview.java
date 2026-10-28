package com.skillmatrix.applications.dto;

import java.util.List;

public record ApplicationImportRowPreview(
        int rowNumber,
        String applicationCode,
        String applicationName,
        String typeCode,
        String bundleCode,
        String portfolioCode,
        String action,     // INSERT | UPDATE | SKIP | ERROR
        String reason,
        List<String> errors
) {
    public static ApplicationImportRowPreview of(int rowNumber, String applicationCode, String applicationName,
            String typeCode, String bundleCode, String portfolioCode, String action, String reason) {
        return new ApplicationImportRowPreview(rowNumber, applicationCode, applicationName,
                typeCode, bundleCode, portfolioCode, action, reason, List.of());
    }

    public static ApplicationImportRowPreview error(int rowNumber, String applicationCode, String applicationName,
            String typeCode, String bundleCode, String portfolioCode, String reason, List<String> errors) {
        return new ApplicationImportRowPreview(rowNumber, applicationCode, applicationName,
                typeCode, bundleCode, portfolioCode, "ERROR", reason, errors);
    }
}
