package com.skillmatrix.admin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request body for PATCH /api/v1/admin/application-assignments/bulk-status
 */
public record BulkStatusRequest(

        @NotEmpty(message = "assignmentPublicIds must not be empty")
        List<String> assignmentPublicIds,

        boolean active,

        String reason
) {}
