package com.skillmatrix.admin.dto;

import java.util.List;

/**
 * Per-row preview returned by POST /api/v1/admin/users/import/validate.
 * Each row carries an action (INSERT / UPDATE / SKIP / ERROR) and a reason.
 */
public record UserImportRowPreview(
        int rowNumber,
        String username,
        String fullName,
        String email,
        String roleCode,
        String action,
        String reason,
        List<String> errors
) {
    public static UserImportRowPreview of(int rowNumber,
            String username, String fullName, String email, String roleCode,
            String action, String reason) {
        return new UserImportRowPreview(rowNumber, username, fullName, email, roleCode,
                action, reason, List.of());
    }

    public static UserImportRowPreview error(int rowNumber,
            String username, String fullName, String email, String roleCode,
            String reason, List<String> errors) {
        return new UserImportRowPreview(rowNumber, username, fullName, email, roleCode,
                "ERROR", reason, errors);
    }
}
