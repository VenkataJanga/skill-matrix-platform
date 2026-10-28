package com.skillmatrix.applications.dto;

/**
 * Result of a single Application hard-delete attempt.
 * <ul>
 *   <li>{@code DELETED} — application was permanently removed (DEV/test only, zero dependencies).</li>
 *   <li>{@code BLOCKED} — application has protected business/history records, or hard-delete
 *       is disabled in this environment; use Deactivate instead.</li>
 *   <li>{@code FAILED}  — an unexpected error occurred.</li>
 * </ul>
 */
public record DeleteApplicationResult(
        String applicationPublicId,
        String applicationCode,
        String status,
        String reason
) {
    public static DeleteApplicationResult deleted(String publicId, String code) {
        return new DeleteApplicationResult(publicId, code, "DELETED", null);
    }

    public static DeleteApplicationResult blocked(String publicId, String code, String reason) {
        return new DeleteApplicationResult(publicId, code, "BLOCKED", reason);
    }
}
