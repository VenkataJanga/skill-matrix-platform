package com.skillmatrix.admin.dto;

import java.util.List;

public record AssignmentValidationResult(
        boolean valid,
        int totalRows,
        int insertRows,
        int updateRows,
        int skipRows,
        int errorRows,
        List<AssignmentImportRowPreview> rows
) {
    public static AssignmentValidationResult of(List<AssignmentImportRowPreview> rows) {
        int insert = 0, update = 0, skip = 0, error = 0;
        for (AssignmentImportRowPreview r : rows) {
            switch (r.action()) {
                case "INSERT" -> insert++;
                case "UPDATE" -> update++;
                case "SKIP"   -> skip++;
                default       -> error++;
            }
        }
        int total = rows.size();
        return new AssignmentValidationResult(error == 0, total, insert, update, skip, error, rows);
    }
}
