package com.skillmatrix.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Multi-Technician Assignment batch request — one Application, many technicians. */
public record BulkAssignmentRequest(
        @NotBlank(message = "applicationPublicId is required")
        String applicationPublicId,

        @NotEmpty(message = "assignments must contain at least one entry")
        @Valid
        List<BulkAssignmentEntry> assignments
) {
}
