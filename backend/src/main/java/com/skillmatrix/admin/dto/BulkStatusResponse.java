package com.skillmatrix.admin.dto;

import java.util.List;

/**
 * Response for PATCH /api/v1/admin/application-assignments/bulk-status
 */
public record BulkStatusResponse(
        int requestedCount,
        int successCount,
        int failedCount,
        List<BulkFailureDetail> failures
) {
    public record BulkFailureDetail(
            String publicId,
            String reason
    ) {}
}
