package com.skillmatrix.requirements.dto;

public record RequirementImportResult(
        boolean success,
        int totalRows,
        int insertedRows,
        int updatedRows,
        int skippedRows,
        int errorRows,
        String importHistoryPublicId,
        String message
) {}
