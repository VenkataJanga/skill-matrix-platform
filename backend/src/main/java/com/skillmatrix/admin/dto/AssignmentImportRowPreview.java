package com.skillmatrix.admin.dto;

import java.util.List;

public record AssignmentImportRowPreview(
        int rowNumber,
        String username,
        String applicationCode,
        String action,   // INSERT | UPDATE | SKIP | ERROR
        String reason,
        List<String> errors
) {
    public static AssignmentImportRowPreview of(int rowNumber, String username, String applicationCode,
            String action, String reason) {
        return new AssignmentImportRowPreview(rowNumber, username, applicationCode, action, reason, List.of());
    }

    public static AssignmentImportRowPreview error(int rowNumber, String username, String applicationCode,
            String reason, List<String> errors) {
        return new AssignmentImportRowPreview(rowNumber, username, applicationCode, "ERROR", reason, errors);
    }
}
