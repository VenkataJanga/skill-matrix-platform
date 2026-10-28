package com.skillmatrix.admin.dto;

/** Row-level result for one technician within a Multi-Technician Assignment batch. */
public record BulkAssignmentRowResult(
        String userPublicId,
        String username,
        String action,   // CREATED | UPDATED | SKIPPED | FAILED
        String reason,
        String assignmentPublicId
) {
    public static BulkAssignmentRowResult created(String userPublicId, String username, String assignmentPublicId) {
        return new BulkAssignmentRowResult(userPublicId, username, "CREATED", null, assignmentPublicId);
    }

    public static BulkAssignmentRowResult updated(String userPublicId, String username, String assignmentPublicId) {
        return new BulkAssignmentRowResult(userPublicId, username, "UPDATED", null, assignmentPublicId);
    }

    public static BulkAssignmentRowResult skipped(String userPublicId, String username, String reason) {
        return new BulkAssignmentRowResult(userPublicId, username, "SKIPPED", reason, null);
    }

    public static BulkAssignmentRowResult failed(String userPublicId, String username, String reason) {
        return new BulkAssignmentRowResult(userPublicId, username, "FAILED", reason, null);
    }
}
