package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.ApplicationAssignmentDto;
import com.skillmatrix.admin.dto.AssignmentRequest;
import com.skillmatrix.admin.dto.AssignmentStatusRequest;
import com.skillmatrix.admin.dto.BulkStatusRequest;
import com.skillmatrix.admin.dto.BulkStatusResponse;
import com.skillmatrix.admin.dto.DuplicateCheckResponse;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.UserApplicationMapping;
import com.skillmatrix.domain.repository.ApplicationRepository;
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

    private final UserApplicationMappingRepository mappingRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
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

        return ApplicationAssignmentDto.from(mappingRepository.save(mapping));
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
        auditService.log(request.active() ? "BULK_ACTIVATE_ASSIGNMENT" : "BULK_DEACTIVATE_ASSIGNMENT",
                "ASSIGNMENT", null, actorUsername, null, null, null,
                "{\"requested\":" + requestedCount + ",\"success\":" + successCount
                        + ",\"failed\":" + failures.size() + ",\"reason\":\"" + request.reason() + "\"}");

        return new BulkStatusResponse(requestedCount, successCount, failures.size(), failures);
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
