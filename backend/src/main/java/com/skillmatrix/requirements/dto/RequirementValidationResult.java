package com.skillmatrix.requirements.dto;

import java.util.List;

public record RequirementValidationResult(
        boolean valid,
        int totalRows,
        int insertRows,
        int updateRows,
        int skipRows,
        int errorRows,
        List<RequirementImportRowPreview> rows
) {
    public static RequirementValidationResult of(List<RequirementImportRowPreview> rows) {
        int insert = 0, update = 0, skip = 0, error = 0;
        for (RequirementImportRowPreview r : rows) {
            switch (r.action()) {
                case "INSERT" -> insert++;
                case "UPDATE" -> update++;
                case "SKIP"   -> skip++;
                default       -> error++;
            }
        }
        int total = rows.size();
        return new RequirementValidationResult(error == 0, total, insert, update, skip, error, rows);
    }
}
