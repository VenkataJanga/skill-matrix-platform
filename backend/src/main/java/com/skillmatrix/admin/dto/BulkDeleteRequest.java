package com.skillmatrix.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for POST /api/v1/admin/users/bulk-delete.
 */
public record BulkDeleteRequest(

        @NotEmpty(message = "At least one userPublicId is required")
        @Size(max = 200, message = "Cannot delete more than 200 users in a single request")
        List<String> userPublicIds,

        /** Optional reason recorded in the audit log. */
        String reason
) {}
