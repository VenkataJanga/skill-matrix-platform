package com.skillmatrix.admin.dto;

/**
 * Request body for PATCH /api/v1/admin/application-assignments/{publicId}/status
 */
public record AssignmentStatusRequest(
        boolean active,
        String reason
) {}
