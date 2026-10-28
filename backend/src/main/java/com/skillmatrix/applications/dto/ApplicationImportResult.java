package com.skillmatrix.applications.dto;

public record ApplicationImportResult(
        boolean success,
        int totalRows,
        int insertedRows,
        int updatedRows,
        int skippedRows,
        int errorRows,
        String importHistoryPublicId,
        String message
) {
}
