package com.skillmatrix.applications.dto;

import java.util.List;

public record ApplicationValidationResult(
        boolean valid,
        int totalRows,
        int insertRows,
        int updateRows,
        int skipRows,
        int errorRows,
        List<ApplicationImportRowPreview> rows
) {
    public static ApplicationValidationResult of(List<ApplicationImportRowPreview> rows) {
        int insert = 0, update = 0, skip = 0, error = 0;
        for (ApplicationImportRowPreview r : rows) {
            switch (r.action()) {
                case "INSERT" -> insert++;
                case "UPDATE" -> update++;
                case "SKIP"   -> skip++;
                default       -> error++;
            }
        }
        int total = rows.size();
        return new ApplicationValidationResult(error == 0, total, insert, update, skip, error, rows);
    }
}
