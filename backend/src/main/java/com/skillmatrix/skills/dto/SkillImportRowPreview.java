package com.skillmatrix.skills.dto;

import java.util.List;

/**
 * Row-level preview returned by the SMART_UPSERT validate endpoint.
 * Every data row in the uploaded Excel produces one of these.
 *
 * action values:
 *   INSERT  — skill_code not in DB; row will be inserted on confirm
 *   UPDATE  — skill_code exists in DB; at least one non-blank field differs
 *   SKIP    — skill_code exists in DB; all comparable fields are identical
 *   ERROR   — row has a validation problem; import will be blocked until fixed
 */
public record SkillImportRowPreview(
        String sheetName,
        int    rowNumber,
        String skillCode,
        String skillName,
        /** INSERT | UPDATE | SKIP | ERROR */
        String action,
        String reason,
        /** Non-empty only when action == ERROR */
        List<String> fieldErrors
) {
    /** Convenience factory — no field errors */
    public static SkillImportRowPreview of(
            String sheetName, int rowNumber,
            String skillCode, String skillName,
            String action, String reason) {
        return new SkillImportRowPreview(sheetName, rowNumber, skillCode, skillName, action, reason, List.of());
    }

    /** Convenience factory — with field errors (always ERROR action) */
    public static SkillImportRowPreview error(
            String sheetName, int rowNumber,
            String skillCode, String skillName,
            String reason, List<String> fieldErrors) {
        return new SkillImportRowPreview(sheetName, rowNumber, skillCode, skillName,
                "ERROR", reason, fieldErrors);
    }
}
