package com.skillmatrix.admin.dto;

/**
 * Per-user result returned inside {@link BulkDeleteResponse} and as the body
 * of DELETE /api/v1/admin/users/{publicId}.
 *
 * <ul>
 *   <li>{@code DELETED}  — user was permanently removed.</li>
 *   <li>{@code BLOCKED}  — user has protected business/history records; use Deactivate instead.</li>
 *   <li>{@code FAILED}   — an unexpected error occurred for this specific user.</li>
 * </ul>
 */
public record DeleteUserResult(
        String userPublicId,
        String username,
        String status,    // "DELETED" | "BLOCKED" | "FAILED"
        String reason
) {

    public static DeleteUserResult deleted(String publicId, String username) {
        return new DeleteUserResult(publicId, username, "DELETED", null);
    }

    public static DeleteUserResult blocked(String publicId, String username, String reason) {
        return new DeleteUserResult(publicId, username, "BLOCKED", reason);
    }

    public static DeleteUserResult failed(String publicId, String username, String reason) {
        return new DeleteUserResult(publicId, username, "FAILED", reason);
    }
}
