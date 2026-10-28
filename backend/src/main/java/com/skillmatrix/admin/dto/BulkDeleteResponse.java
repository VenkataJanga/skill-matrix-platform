package com.skillmatrix.admin.dto;

import java.util.List;

/**
 * Response body for POST /api/v1/admin/users/bulk-delete.
 * Also returned (wrapped in ApiResponse) for single-user DELETE when
 * a summary view is more useful than a single {@link DeleteUserResult}.
 */
public record BulkDeleteResponse(
        int requestedCount,
        int deletedCount,
        int blockedCount,
        int failedCount,

        /**
         * Only non-DELETED results are included here to keep the payload compact.
         * Successful deletes are counted in {@code deletedCount} only.
         */
        List<DeleteUserResult> results
) {
    /** Convenience factory used by the bulk-delete service. */
    public static BulkDeleteResponse of(List<DeleteUserResult> allResults) {
        int deleted = (int) allResults.stream().filter(r -> "DELETED".equals(r.status())).count();
        int blocked = (int) allResults.stream().filter(r -> "BLOCKED".equals(r.status())).count();
        int failed  = (int) allResults.stream().filter(r -> "FAILED".equals(r.status())).count();
        List<DeleteUserResult> nonDeleted = allResults.stream()
                .filter(r -> !"DELETED".equals(r.status()))
                .toList();
        return new BulkDeleteResponse(allResults.size(), deleted, blocked, failed, nonDeleted);
    }
}
