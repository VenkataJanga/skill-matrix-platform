package com.skillmatrix.admin.dto;

/**
 * Request body for PATCH /api/v1/admin/users/{publicId}/status
 */
public record UserStatusRequest(
        boolean active,
        String reason
) {}
