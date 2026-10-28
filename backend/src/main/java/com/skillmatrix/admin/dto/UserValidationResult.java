package com.skillmatrix.admin.dto;

import java.util.List;

/**
 * Aggregate validation result returned by POST /api/v1/admin/users/import/validate.
 * {@code valid} is true when errorRows == 0, meaning the file can be confirmed.
 */
public record UserValidationResult(
        boolean valid,
        int totalRows,
        int insertRows,
        int updateRows,
        int skipRows,
        int errorRows,
        List<UserImportRowPreview> rows
) {
    public static UserValidationResult of(List<UserImportRowPreview> rows) {
        int insert = 0, update = 0, skip = 0, error = 0;
        for (UserImportRowPreview r : rows) {
            switch (r.action()) {
                case "INSERT" -> insert++;
                case "UPDATE" -> update++;
                case "SKIP"   -> skip++;
                default       -> error++;
            }
        }
        return new UserValidationResult(error == 0, rows.size(), insert, update, skip, error, rows);
    }
}
