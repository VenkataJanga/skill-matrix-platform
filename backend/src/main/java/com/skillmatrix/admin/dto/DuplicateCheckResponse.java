package com.skillmatrix.admin.dto;

/**
 * Response for GET /api/v1/admin/application-assignments/check-duplicate
 */
public record DuplicateCheckResponse(
        boolean isDuplicate,
        String existingAssignmentPublicId,
        String message
) {
    public static DuplicateCheckResponse noDuplicate() {
        return new DuplicateCheckResponse(false, null, "No existing active assignment found.");
    }

    public static DuplicateCheckResponse duplicate(String publicId) {
        return new DuplicateCheckResponse(true, publicId,
                "This technician is already assigned to the selected application.");
    }
}
