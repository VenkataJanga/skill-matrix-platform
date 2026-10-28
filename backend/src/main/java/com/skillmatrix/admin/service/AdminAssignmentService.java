package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.ApplicationAssignmentDto;
import com.skillmatrix.admin.dto.AssignmentRequest;
import com.skillmatrix.admin.dto.AssignmentStatusRequest;
import com.skillmatrix.admin.dto.BulkAssignmentEntry;
import com.skillmatrix.admin.dto.BulkAssignmentRequest;
import com.skillmatrix.admin.dto.BulkAssignmentResult;
import com.skillmatrix.admin.dto.BulkAssignmentRowResult;
import com.skillmatrix.admin.dto.BulkStatusRequest;
import com.skillmatrix.admin.dto.BulkStatusResponse;
import com.skillmatrix.admin.dto.DuplicateCheckResponse;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.Application;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserApplicationMapping;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.UserApplicationMappingRepository;
import com.skillmatrix.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminAssignmentService {

    private static final String LOOKUP_TYPE_ROLE_ON_APP = "ROLE_ON_APPLICATION";

    private final UserApplicationMappingRepository mappingRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final LookupValueRepository lookupValueRepository;
    private final AuditService auditService;

    // ---------------------------------------------------------------
    // Paginated search
    // ---------------------------------------------------------------

    public PagedResponse<ApplicationAssignmentDto> getAssignments(
            int page, int size,
            String search, String appPublicId, String techPublicId,
            String typeCode, String bundleCode, String status,
            String sortBy, String sortDirection) {

        Boolean activeFilter = parseActiveFilter(status);
        String cleanSearch     = blank(search)     ? null : search.trim();
        String cleanApp        = blank(appPublicId) ? null : appPublicId.trim();
        String cleanTech       = blank(techPublicId)? null : techPublicId.trim();
        String cleanType       = blank(typeCode)    ? null : typeCode.trim();
        String cleanBundle     = blank(bundleCode)  ? null : bundleCode.trim();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<UserApplicationMapping> results = mappingRepository.searchAssignments(
                cleanSearch, cleanApp, cleanTech, cleanType, cleanBundle, activeFilter, pageable);

        return PagedResponse.of(
                results.getContent().stream().map(ApplicationAssignmentDto::from).toList(),
                results);
    }

    // ---------------------------------------------------------------
    // Duplicate check
    // ---------------------------------------------------------------

    public DuplicateCheckResponse checkDuplicate(String userPublicId, String appPublicId) {
        return mappingRepository.findActiveByUserPublicIdAndAppPublicId(userPublicId, appPublicId)
                .map(m -> DuplicateCheckResponse.duplicate(m.getPublicId()))
                .orElse(DuplicateCheckResponse.noDuplicate());
    }

    // ---------------------------------------------------------------
    // Create assignment
    // ---------------------------------------------------------------

    @Transactional
    public ApplicationAssignmentDto createAssignment(AssignmentRequest request, String actorUsername) {
        var user = userRepository.findByPublicId(request.userPublicId())
                .orElseThrow(() -> new BusinessException("User not found: " + request.userPublicId()));

        var application = applicationRepository.findByPublicId(request.applicationPublicId())
                .orElseThrow(() -> new BusinessException("Application not found: " + request.applicationPublicId()));

        // Transactional duplicate check — also enforced at DB level by V08
        if (mappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(user.getId(), application.getId())) {
            log.warn("createAssignment rejected — duplicate: user={} app={} by={}",
                    user.getUsername(), application.getApplicationCode(), actorUsername);
            throw new BusinessException(
                    "This technician is already assigned to the selected application.",
                    HttpStatus.CONFLICT);
        }

        UserApplicationMapping mapping = UserApplicationMapping.builder()
                .publicId(UUID.randomUUID().toString())
                .user(user)
                .application(application)
                .allocationPercentage(request.allocationPercentage())
                .effectiveFrom(request.effectiveFrom() != null ? request.effectiveFrom() : LocalDate.now())
                .effectiveTo(request.effectiveTo())
                .active(true)
                .createdBy(actorUsername)
                .updatedBy(actorUsername)
                .build();

        ApplicationAssignmentDto dto = ApplicationAssignmentDto.from(mappingRepository.save(mapping));
        log.info("Assignment created: publicId={} user={} app={} allocation={}% by={}",
                dto.publicId(), user.getUsername(), application.getApplicationCode(),
                request.allocationPercentage(), actorUsername);

        auditService.log("CREATE_ASSIGNMENT", "ASSIGNMENT",
                null, actorUsername, dto.publicId(), null, null,
                "{\"user\":\"" + user.getUsername() + "\",\"app\":\"" + application.getApplicationCode() + "\"}");

        return dto;
    }

    // ---------------------------------------------------------------
    // Update assignment
    // ---------------------------------------------------------------

    @Transactional
    public ApplicationAssignmentDto updateAssignment(String publicId, AssignmentRequest request, String actorUsername) {
        UserApplicationMapping mapping = mappingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("Assignment not found: " + publicId, HttpStatus.NOT_FOUND));

        mapping.setAllocationPercentage(request.allocationPercentage());
        mapping.setEffectiveFrom(request.effectiveFrom());
        mapping.setEffectiveTo(request.effectiveTo());
        mapping.setUpdatedBy(actorUsername);

        ApplicationAssignmentDto updated = ApplicationAssignmentDto.from(mappingRepository.save(mapping));
        log.info("Assignment updated: publicId={} allocation={}% by={}", publicId,
                request.allocationPercentage(), actorUsername);
        return updated;
    }

    // ---------------------------------------------------------------
    // Single status update
    // ---------------------------------------------------------------

    @Transactional
    public ApplicationAssignmentDto updateAssignmentStatus(
            String publicId, AssignmentStatusRequest request, String actorUsername) {
        UserApplicationMapping mapping = mappingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("Assignment not found: " + publicId, HttpStatus.NOT_FOUND));

        mapping.setActive(request.active());
        mapping.setUpdatedBy(actorUsername);
        if (!request.active() && mapping.getEffectiveTo() == null) {
            mapping.setEffectiveTo(LocalDate.now());
        }

        ApplicationAssignmentDto dto = ApplicationAssignmentDto.from(mappingRepository.save(mapping));
        log.info("Assignment status changed: publicId={} active={} by={}", publicId, request.active(), actorUsername);

        auditService.log(request.active() ? "ACTIVATE_ASSIGNMENT" : "DEACTIVATE_ASSIGNMENT",
                "ASSIGNMENT", null, actorUsername, publicId, null, null,
                "{\"active\":" + request.active() + ",\"reason\":\"" + request.reason() + "\"}");

        return dto;
    }

    // ---------------------------------------------------------------
    // Bulk status update
    // ---------------------------------------------------------------

    @Transactional
    public BulkStatusResponse bulkUpdateStatus(BulkStatusRequest request, String actorUsername) {
        List<BulkStatusResponse.BulkFailureDetail> failures = new ArrayList<>();
        int successCount = 0;

        for (String pid : request.assignmentPublicIds()) {
            try {
                mappingRepository.findByPublicId(pid).ifPresentOrElse(
                        m -> {
                            m.setActive(request.active());
                            m.setUpdatedBy(actorUsername);
                            if (!request.active() && m.getEffectiveTo() == null) {
                                m.setEffectiveTo(LocalDate.now());
                            }
                            mappingRepository.save(m);
                        },
                        () -> { throw new BusinessException("Assignment not found: " + pid); }
                );
                successCount++;
            } catch (Exception e) {
                failures.add(new BulkStatusResponse.BulkFailureDetail(pid, e.getMessage()));
                log.warn("Bulk status update failed for assignment {}: {}", pid, e.getMessage());
            }
        }

        int requestedCount = request.assignmentPublicIds().size();
        log.info("Bulk assignment status update: requested={} success={} failed={} active={} by={}",
                requestedCount, successCount, failures.size(), request.active(), actorUsername);

        auditService.log(request.active() ? "BULK_ACTIVATE_ASSIGNMENT" : "BULK_DEACTIVATE_ASSIGNMENT",
                "ASSIGNMENT", null, actorUsername, null, null, null,
                "{\"requested\":" + requestedCount + ",\"success\":" + successCount
                        + ",\"failed\":" + failures.size() + ",\"reason\":\"" + request.reason() + "\"}");

        return new BulkStatusResponse(requestedCount, successCount, failures.size(), failures);
    }

    // ---------------------------------------------------------------
    // Multi-Technician Assignment (bulk create, M4 stabilization enhancement —
    // Problem Statement Part B). Each technician is processed in its own
    // independent try/catch — one invalid/duplicate technician never blocks
    // unrelated valid technicians in the same batch (controlled partial
    // processing, consistent with bulkUpdateStatus above). Each individual
    // assignment insert remains atomic via its own @Transactional boundary.
    // ---------------------------------------------------------------

    @Transactional
    public BulkAssignmentResult createBulkAssignments(BulkAssignmentRequest request, String actorUsername) {
        Application application = applicationRepository.findByPublicId(request.applicationPublicId())
                .orElseThrow(() -> new BusinessException("Application not found: " + request.applicationPublicId()));

        if (!application.isActive()) {
            throw new BusinessException("Application is inactive: " + application.getApplicationCode());
        }

        List<BulkAssignmentRowResult> rows = new ArrayList<>();
        for (BulkAssignmentEntry entry : request.assignments()) {
            rows.add(createSingleInBulk(application, entry, actorUsername));
        }

        BulkAssignmentResult result = BulkAssignmentResult.of(rows);
        log.info("Bulk technician assignment: application={} requested={} created={} updated={} skipped={} failed={} by={}",
                application.getApplicationCode(), result.requested(), result.created(), result.updated(),
                result.skipped(), result.failed(), actorUsername);

        auditService.log("TECHNICIAN_ASSIGNMENT_BULK_CREATED", "ASSIGNMENT", null, actorUsername, null, null, null,
                "{\"application\":\"" + application.getApplicationCode() + "\",\"requested\":" + result.requested()
                        + ",\"created\":" + result.created() + ",\"updated\":" + result.updated()
                        + ",\"skipped\":" + result.skipped() + ",\"failed\":" + result.failed() + "}");

        return result;
    }

    /**
     * Processes exactly one technician's assignment within a bulk request. Wrapped
     * in its own transaction so that a failure for one technician does not roll
     * back assignments already committed for other technicians in the same batch.
     */
    @Transactional
    protected BulkAssignmentRowResult createSingleInBulk(Application application, BulkAssignmentEntry entry, String actorUsername) {
        User user;
        try {
            user = userRepository.findByPublicId(entry.userPublicId())
                    .orElseThrow(() -> new BusinessException("User not found: " + entry.userPublicId()));
        } catch (Exception ex) {
            return BulkAssignmentRowResult.failed(entry.userPublicId(), entry.userPublicId(), ex.getMessage());
        }

        if (!user.isActive()) {
            return BulkAssignmentRowResult.failed(entry.userPublicId(), user.getUsername(), "User is inactive.");
        }

        if (entry.effectiveTo() != null && entry.effectiveTo().isBefore(entry.effectiveFrom())) {
            return BulkAssignmentRowResult.failed(entry.userPublicId(), user.getUsername(),
                    "effectiveTo must be on or after effectiveFrom.");
        }

        if (mappingRepository.existsByUser_IdAndApplication_IdAndActiveTrue(user.getId(), application.getId())) {
            return BulkAssignmentRowResult.skipped(entry.userPublicId(), user.getUsername(),
                    "Active assignment already exists.");
        }

        var roleLookup = blank(entry.roleOnAppCode())
                ? null
                : lookupValueRepository.findByTypeCodeAndValueCode(LOOKUP_TYPE_ROLE_ON_APP, entry.roleOnAppCode().trim().toUpperCase())
                        .orElse(null);
        if (!blank(entry.roleOnAppCode()) && roleLookup == null) {
            return BulkAssignmentRowResult.failed(entry.userPublicId(), user.getUsername(),
                    "Invalid roleOnAppCode: " + entry.roleOnAppCode());
        }

        try {
            UserApplicationMapping mapping = UserApplicationMapping.builder()
                    .publicId(UUID.randomUUID().toString())
                    .user(user)
                    .application(application)
                    .roleOnAppLookup(roleLookup)
                    .allocationPercentage(entry.allocationPercentage())
                    .effectiveFrom(entry.effectiveFrom())
                    .effectiveTo(entry.effectiveTo())
                    .active(true)
                    .createdBy(actorUsername)
                    .updatedBy(actorUsername)
                    .build();

            UserApplicationMapping saved = mappingRepository.save(mapping);
            log.info("Bulk assignment created: publicId={} user={} app={} allocation={}% by={}",
                    saved.getPublicId(), user.getUsername(), application.getApplicationCode(),
                    entry.allocationPercentage(), actorUsername);
            return BulkAssignmentRowResult.created(user.getPublicId(), user.getUsername(), saved.getPublicId());
        } catch (Exception ex) {
            // Covers DB-level unique constraint races (uq_uam_active_user_app) not
            // caught by the pre-check above, plus any other unexpected failure.
            log.warn("Bulk assignment failed for user={} app={}: {}",
                    user.getUsername(), application.getApplicationCode(), ex.getMessage());
            return BulkAssignmentRowResult.failed(user.getPublicId(), user.getUsername(), "Unexpected error: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Boolean parseActiveFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        if ("ACTIVE".equalsIgnoreCase(status)) return Boolean.TRUE;
        if ("INACTIVE".equalsIgnoreCase(status)) return Boolean.FALSE;
        return null;
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
