package com.skillmatrix.admin.dto;

/**
 * Final result returned by POST /api/v1/admin/users/import/confirm.
 *
 * <ul>
 *   <li>{@code success} — true for both STRICT success and PARTIAL_SUCCESS.</li>
 *   <li>{@code partialSuccess} — true when importMode=PARTIAL and at least one
 *       valid row was processed while at least one ERROR row was rejected.</li>
 *   <li>{@code importMode} — "STRICT" | "PARTIAL" echoed back from the request.</li>
 * </ul>
 */
public record UserImportResult(
        boolean success,
        int totalRows,
        int insertedRows,
        int updatedRows,
        int skippedRows,
        int errorRows,
        String importHistoryPublicId,
        String message,
        String importMode,
        boolean partialSuccess
) {}
