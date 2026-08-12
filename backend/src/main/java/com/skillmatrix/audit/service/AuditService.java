package com.skillmatrix.audit.service;

import com.skillmatrix.domain.entity.AuditLog;
import com.skillmatrix.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Async audit log writer.
 *
 * <p>All methods are {@code @Async} so they never block the main request thread
 * and run in their own transaction to survive parent rollbacks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    // ----------------------------------------------------------------
    // Audit action constants
    // ----------------------------------------------------------------
    public static final String LOGIN_SUCCESS     = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE     = "LOGIN_FAILURE";
    public static final String LOGOUT            = "LOGOUT";
    public static final String PASSWORD_CHANGE   = "PASSWORD_CHANGE";
    public static final String TOKEN_REFRESH     = "TOKEN_REFRESH";

    public static final String ENTITY_AUTH       = "AUTH";
    public static final String ENTITY_USER       = "USER";

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action,
                    String entityType,
                    Long actorUserId,
                    String actorUsername,
                    String entityPubid,
                    String ipAddress,
                    String correlationId,
                    String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .publicId(UUID.randomUUID().toString())
                    .action(action)
                    .entityType(entityType)
                    .actorUserId(actorUserId)
                    .actorUsername(actorUsername)
                    .entityPubid(entityPubid)
                    .newValue(details)
                    .ipAddress(ipAddress)
                    .correlationId(correlationId)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Never let audit failure break the main flow
            log.error("Failed to write audit log [action={}, user={}]: {}", action, actorUsername, e.getMessage(), e);
        }
    }

    /** Convenience overload for auth events with no detail payload. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuth(String action,
                        Long actorUserId,
                        String actorUsername,
                        String ipAddress,
                        String correlationId) {
        log(action, ENTITY_AUTH, actorUserId, actorUsername, null, ipAddress, correlationId, null);
    }
}
