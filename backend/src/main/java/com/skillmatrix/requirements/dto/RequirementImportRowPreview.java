package com.skillmatrix.requirements.dto;

import java.util.List;

/**
 * Row-level preview for the Application Skill Requirement Mapping Excel import.
 *
 * action values:
 *   INSERT  — no existing mapping (version + skill); will be inserted on confirm
 *   UPDATE  — existing mapping found; expected_level/mandatory/role/grade differ
 *   SKIP    — existing mapping found; values identical
 *   ERROR   — validation failure; import is blocked until fixed
 */
public record RequirementImportRowPreview(
        String sheetName,
        int    rowNumber,
        String applicationCode,
        String skillCode,
        String skillName,
        Integer expectedLevel,
        String mandatoryFlag,
        /** INSERT | UPDATE | SKIP | ERROR */
        String action,
        String reason,
        List<String> fieldErrors
) {
    public static RequirementImportRowPreview of(
            String sheetName, int rowNumber,
            String applicationCode, String skillCode, String skillName,
            Integer expectedLevel, String mandatoryFlag,
            String action, String reason) {
        return new RequirementImportRowPreview(sheetName, rowNumber, applicationCode,
                skillCode, skillName, expectedLevel, mandatoryFlag, action, reason, List.of());
    }

    public static RequirementImportRowPreview error(
            String sheetName, int rowNumber,
            String applicationCode, String skillCode, String skillName,
            Integer expectedLevel, String mandatoryFlag,
            String reason, List<String> fieldErrors) {
        return new RequirementImportRowPreview(sheetName, rowNumber, applicationCode,
                skillCode, skillName, expectedLevel, mandatoryFlag, "ERROR", reason, fieldErrors);
    }
}
