package com.skillmatrix.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /api/v1/admin/users/{publicId}
 */
public record UpdateUserRequest(

        @NotBlank(message = "fullName is required")
        @Size(max = 200, message = "fullName must be max 200 characters")
        String fullName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 200, message = "email must be max 200 characters")
        String email,

        @NotBlank(message = "roleCode is required")
        String roleCode,

        String employeeId,

        /** publicId of the manager user — optional */
        String managerPublicId,

        boolean mustChangePassword
) {}
