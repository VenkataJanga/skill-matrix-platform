package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.BulkDeleteRequest;
import com.skillmatrix.admin.dto.BulkDeleteResponse;
import com.skillmatrix.admin.dto.DeleteUserResult;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.config.UserManagementProperties;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.ImportExportHistoryRepository;
import com.skillmatrix.domain.repository.RefreshTokenRepository;
import com.skillmatrix.domain.repository.UserApplicationMappingRepository;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.domain.repository.UserTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for permanently hard-deleting users.
 *
 * <h3>Delete safety rules</h3>
 * <ul>
 *   <li>Requires {@code user-management.hard-delete-enabled=true}; otherwise HTTP 423.</li>
 *   <li>Actor cannot delete their own account.</li>
 *   <li>Last active ADMIN cannot be deleted.</li>
 *   <li>Users with protected business/history records are BLOCKED (not deleted).</li>
 * </ul>
 *
 * <h3>Pre-delete cleanup (safe, non-blocking)</h3>
 * <ul>
 *   <li>{@code refresh_tokens} — permanently removed (session data, not business history).</li>
 *   <li>{@code user_teams}     — permanently removed (current membership mapping).</li>
 *   <li>{@code user_roles}     — removed via JPA cascade on User entity.</li>
 *   <li>{@code users.manager_id} self-ref — nullified on direct reports.</li>
 * </ul>
 *
 * <h3>Blocking dependencies (any record → BLOCKED)</h3>
 * {@code user_application_mapping}, {@code import_export_history},
 * {@code technician_assessments}, {@code lead_reviews},
 * {@code assessment_approvals}, {@code skill_gap_snapshots},
 * {@code training_participants}, {@code notification_log}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserDeleteService {

    private final UserRepository                   userRepository;
    private final UserTeamRepository               userTeamRepository;
    private final RefreshTokenRepository           refreshTokenRepository;
    private final UserApplicationMappingRepository userApplicationMappingRepository;
    private final ImportExportHistoryRepository    importExportHistoryRepository;
    private final AuditService                     auditService;
    private final UserManagementProperties         userManagementProperties;
    private final JdbcTemplate                     jdbcTemplate;

    // ----------------------------------------------------------------
    // Single delete
    // ----------------------------------------------------------------

    /**
     * Permanently delete a single user identified by {@code publicId}.
     *
     * @param publicId      target user's publicId
     * @param actorUsername username of the logged-in ADMIN performing the action
     * @param actorPublicId publicId of the logged-in ADMIN (for self-delete guard)
     * @param reason        optional reason recorded in the audit log
     * @return {@link DeleteUserResult} with status DELETED, BLOCKED, or FAILED
     */
    @Transactional
    public DeleteUserResult deleteUser(String publicId,
                                       String actorUsername,
                                       String actorPublicId,
                                       String reason) {
        guardHardDeleteEnabled();

        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("User not found: " + publicId, HttpStatus.NOT_FOUND));

        // Special protections
        if (publicId.equals(actorPublicId)) {
            return DeleteUserResult.blocked(publicId, user.getUsername(),
                    "You cannot delete your own account.");
        }
        String lastAdminBlock = checkLastAdminBlock(user);
        if (lastAdminBlock != null) {
            return DeleteUserResult.blocked(publicId, user.getUsername(), lastAdminBlock);
        }

        // Dependency guard
        String blockReason = checkBlockingDependencies(user);
        if (blockReason != null) {
            log.info("User delete BLOCKED: publicId={} username={} reason={}", publicId, user.getUsername(), blockReason);
            return DeleteUserResult.blocked(publicId, user.getUsername(), blockReason);
        }

        // Pre-delete cleanup
        performPreDeleteCleanup(user);

        // Hard delete
        userRepository.delete(user);
        log.info("User DELETED: publicId={} username={} by={}", publicId, user.getUsername(), actorUsername);

        auditService.log(AuditService.USER_DELETE, AuditService.ENTITY_USER,
                null, actorUsername, publicId, null, null,
                buildAuditDetails(user.getUsername(), reason, 1, 1, 0, 0));

        return DeleteUserResult.deleted(publicId, user.getUsername());
    }

    // ----------------------------------------------------------------
    // Bulk delete
    // ----------------------------------------------------------------

    /**
     * Bulk-delete users. Each user is processed independently so that one blocked
     * user does not prevent deletion of unrelated eligible users.
     *
     * <p>{@code @Transactional} is required here so that a Hibernate session is
     * open for the entire operation. Without it, Spring AOP cannot proxy the
     * internal {@code deleteSingleInBulk} call (self-invocation bypasses the proxy),
     * and lazy collections such as {@code userRoles} cannot be initialized,
     * causing {@code LazyInitializationException}.
     *
     * @param request       contains list of publicIds and optional reason
     * @param actorUsername username of the logged-in ADMIN
     * @param actorPublicId publicId of the logged-in ADMIN
     * @return {@link BulkDeleteResponse} with per-user results and aggregated counts
     */
    @Transactional
    public BulkDeleteResponse bulkDeleteUsers(BulkDeleteRequest request,
                                               String actorUsername,
                                               String actorPublicId) {
        guardHardDeleteEnabled();

        List<DeleteUserResult> results = new ArrayList<>();

        for (String publicId : request.userPublicIds()) {
            DeleteUserResult result = deleteSingleInBulk(publicId, actorUsername, actorPublicId, request.reason());
            results.add(result);
        }

        BulkDeleteResponse response = BulkDeleteResponse.of(results);

        log.info("Bulk delete complete: requested={} deleted={} blocked={} failed={} by={}",
                response.requestedCount(), response.deletedCount(),
                response.blockedCount(), response.failedCount(), actorUsername);

        auditService.log(AuditService.USER_BULK_DELETE, AuditService.ENTITY_USER,
                null, actorUsername, null, null, null,
                buildAuditDetails(null, request.reason(),
                        response.requestedCount(), response.deletedCount(),
                        response.blockedCount(), response.failedCount()));

        return response;
    }

    // ----------------------------------------------------------------
    // Internal — single user deletion within a bulk operation
    // ----------------------------------------------------------------

    /**
     * Wraps a single-user delete in its own transaction so a failure on one
     * user does not roll back deletions of other users in the batch.
     */
    @Transactional
    protected DeleteUserResult deleteSingleInBulk(String publicId,
                                                   String actorUsername,
                                                   String actorPublicId,
                                                   String reason) {
        try {
            User user = userRepository.findByPublicId(publicId).orElse(null);
            if (user == null) {
                return DeleteUserResult.failed(publicId, publicId, "User not found");
            }

            if (publicId.equals(actorPublicId)) {
                return DeleteUserResult.blocked(publicId, user.getUsername(),
                        "You cannot delete your own account.");
            }
            String lastAdminBlock = checkLastAdminBlock(user);
            if (lastAdminBlock != null) {
                return DeleteUserResult.blocked(publicId, user.getUsername(), lastAdminBlock);
            }

            String blockReason = checkBlockingDependencies(user);
            if (blockReason != null) {
                return DeleteUserResult.blocked(publicId, user.getUsername(), blockReason);
            }

            performPreDeleteCleanup(user);
            userRepository.delete(user);
            log.info("Bulk delete — user DELETED: publicId={} username={} by={}", publicId, user.getUsername(), actorUsername);
            return DeleteUserResult.deleted(publicId, user.getUsername());

        } catch (Exception ex) {
            log.error("Bulk delete — unexpected error for publicId={}: {}", publicId, ex.getMessage(), ex);
            return DeleteUserResult.failed(publicId, publicId, "Unexpected error: " + ex.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Guards
    // ----------------------------------------------------------------

    private void guardHardDeleteEnabled() {
        if (!userManagementProperties.isHardDeleteEnabled()) {
            throw new BusinessException(
                    "Hard delete is disabled in this environment. "
                    + "Use Deactivate to prevent the user from logging in.",
                    HttpStatus.LOCKED);
        }
    }

    /**
     * Returns a non-null block reason if the user is the last active ADMIN.
     *
     * Uses {@code primaryRole} (a {@code @ManyToOne}, eagerly loaded) rather than
     * the {@code userRoles} collection (lazily loaded) to avoid
     * {@code LazyInitializationException} outside an active Hibernate session.
     */
    private String checkLastAdminBlock(User user) {
        boolean isAdmin = user.getPrimaryRole() != null
                && "ADMIN".equals(user.getPrimaryRole().getRoleCode());
        if (!isAdmin) return null;

        long activeAdminCount = userRepository.countByActiveAndPrimaryRole_RoleCode(true, "ADMIN");
        if (activeAdminCount <= 1) {
            return "Cannot delete the last active ADMIN account.";
        }
        return null;
    }

    /**
     * Checks all protecting FK tables.  Returns the first block reason found, or
     * {@code null} if the user is safe to delete.
     *
     * <p>JPA repositories are used for tables that have entities in scope.
     * Native SQL COUNT queries via {@link JdbcTemplate} are used for future-module
     * tables (assessments, reviews, training, notifications) that do not yet have
     * JPA entities in the current codebase.
     */
    private String checkBlockingDependencies(User user) {
        Long userId = user.getId();

        if (userApplicationMappingRepository.existsByUser_Id(userId)) {
            return "User has application assignment(s). Deactivate the user instead.";
        }
        if (importExportHistoryRepository.existsByInitiatedById(userId)) {
            return "User has import/export history records. Deactivate the user instead.";
        }

        // Future-module tables — checked via native SQL COUNT to avoid requiring
        // JPA entities that are not yet implemented.
        if (existsInTable("technician_assessments", "user_id", userId)) {
            return "User has technician assessment history. Deactivate the user instead.";
        }
        if (existsInTable("lead_reviews", "reviewer_user_id", userId)) {
            return "User has lead review history. Deactivate the user instead.";
        }
        if (existsAnyInTable("assessment_approvals",
                new String[]{"technician_user_id", "reviewer_user_id"}, userId)) {
            return "User has assessment approval records. Deactivate the user instead.";
        }
        if (existsInTable("skill_gap_snapshots", "user_id", userId)) {
            return "User has skill gap snapshot history. Deactivate the user instead.";
        }
        if (existsInTable("training_participants", "user_id", userId)) {
            return "User has training participation records. Deactivate the user instead.";
        }
        if (existsInTable("notification_log", "recipient_user_id", userId)) {
            return "User has notification log records. Deactivate the user instead.";
        }

        return null;
    }

    // ----------------------------------------------------------------
    // Pre-delete cleanup
    // ----------------------------------------------------------------

    /**
     * Removes session/membership records that are safe to clean up before deleting
     * the user row.  Also nullifies manager references on direct reports.
     */
    private void performPreDeleteCleanup(User user) {
        Long userId = user.getId();

        // 1. Refresh tokens (session data — not business history)
        refreshTokenRepository.deleteAllByUser(user);

        // 2. Team memberships (current mapping — not historical records)
        userTeamRepository.deleteAllByUser_Id(userId);

        // 3. Nullify manager_id on direct reports so they become unmanaged
        //    rather than orphaned by a broken FK.
        userRepository.nullifyManagerFor(userId);

        // 4. user_roles are removed via JPA cascade when the User entity is deleted.
        //    (UserRole has CascadeType.ALL / orphanRemoval on User.userRoles)
    }

    // ----------------------------------------------------------------
    // JdbcTemplate helpers for future-module tables
    // ----------------------------------------------------------------

    /**
     * Returns {@code true} if the given table has at least one row where
     * {@code column = userId}.
     */
    private boolean existsInTable(String table, String column, Long userId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM " + table + " WHERE " + column + " = ?",
                    Integer.class, userId);
            return count != null && count > 0;
        } catch (Exception ex) {
            // Table may not exist yet (future module); treat as no records.
            log.debug("existsInTable({}, {}) — table may not exist yet: {}", table, column, ex.getMessage());
            return false;
        }
    }

    /**
     * Returns {@code true} if the given table has at least one row where ANY of
     * the supplied columns equals {@code userId}.
     */
    private boolean existsAnyInTable(String table, String[] columns, Long userId) {
        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM ").append(table).append(" WHERE ");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sql.append(" OR ");
                sql.append(columns[i]).append(" = ?");
            }
            Object[] params = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) params[i] = userId;
            Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params);
            return count != null && count > 0;
        } catch (Exception ex) {
            log.debug("existsAnyInTable({}) — table may not exist yet: {}", table, ex.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------------
    // Audit helpers
    // ----------------------------------------------------------------

    private String buildAuditDetails(String username, String reason,
                                      int requested, int deleted, int blocked, int failed) {
        StringBuilder sb = new StringBuilder("{");
        if (username != null) sb.append("\"username\":\"").append(username).append("\",");
        sb.append("\"requested\":").append(requested)
          .append(",\"deleted\":").append(deleted)
          .append(",\"blocked\":").append(blocked)
          .append(",\"failed\":").append(failed);
        if (reason != null && !reason.isBlank()) {
            sb.append(",\"reason\":\"").append(reason.replace("\"", "\\\"")).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }
}
