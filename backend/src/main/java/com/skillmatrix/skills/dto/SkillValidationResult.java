package com.skillmatrix.skills.dto;

import java.util.List;

/**
 * Response for POST /api/v1/admin/import/skills/validate (SMART_UPSERT version).
 *
 * <p>Every uploaded row is analysed and appears in {@code rows} with one of:
 * INSERT / UPDATE / SKIP / ERROR.
 * {@code valid} is true when errorRows == 0, meaning the file can be safely
 * submitted to the confirm endpoint.
 */
public record SkillValidationResult(
        boolean valid,
        int totalRows,
        int insertRows,
        int updateRows,
        int skipRows,
        int errorRows,
        List<SkillImportRowPreview> rows
) {
    /** True when there are no error rows — confirm import can proceed. */
    public static SkillValidationResult of(List<SkillImportRowPreview> rows) {
        int insert = 0, update = 0, skip = 0, error = 0;
        for (SkillImportRowPreview r : rows) {
            switch (r.action()) {
                case "INSERT" -> insert++;
                case "UPDATE" -> update++;
                case "SKIP"   -> skip++;
                default       -> error++;
            }
        }
        int total = rows.size();
        return new SkillValidationResult(error == 0, total, insert, update, skip, error, rows);
    }
}
