package com.skillmatrix.skills.dto;

/**
 * Response for POST /api/v1/admin/import/skills/confirm (SMART_UPSERT version).
 * Extends the original result with updatedRows to track UPSERT outcomes.
 */
public record SkillImportResult(
        boolean success,
        int totalRows,
        int insertedRows,
        int updatedRows,
        int skippedRows,
        int errorRows,
        String importHistoryPublicId,
        String message
) {}
