package com.skillmatrix.admin.dto;

import java.util.List;

/** Overall batch result for a Multi-Technician Assignment request. */
public record BulkAssignmentResult(
        int requested,
        int created,
        int updated,
        int skipped,
        int failed,
        List<BulkAssignmentRowResult> rows
) {
    public static BulkAssignmentResult of(List<BulkAssignmentRowResult> rows) {
        int created = 0, updated = 0, skipped = 0, failed = 0;
        for (BulkAssignmentRowResult r : rows) {
            switch (r.action()) {
                case "CREATED" -> created++;
                case "UPDATED" -> updated++;
                case "SKIPPED" -> skipped++;
                case "FAILED"  -> failed++;
                default -> { }
            }
        }
        return new BulkAssignmentResult(rows.size(), created, updated, skipped, failed, rows);
    }
}
