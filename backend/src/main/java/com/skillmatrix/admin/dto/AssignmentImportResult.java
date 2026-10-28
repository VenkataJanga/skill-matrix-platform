package com.skillmatrix.admin.dto;

public record AssignmentImportResult(
        boolean success,
        int totalRows,
        int insertedRows,
        int updatedRows,
        int skippedRows,
        int errorRows,
        String importHistoryPublicId,
        String message,
        String status // SUCCESS | PARTIAL_SUCCESS | FAILED
) {
}
