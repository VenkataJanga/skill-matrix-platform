package com.skillmatrix.assessments.service;

import com.skillmatrix.assessments.dto.AssessmentCycleDto;
import com.skillmatrix.assessments.dto.CreateAssessmentCycleRequest;
import com.skillmatrix.assessments.dto.ReopenAssessmentCycleRequest;
import com.skillmatrix.assessments.dto.UpdateAssessmentCycleRequest;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.*;
import com.skillmatrix.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Admin service for Assessment Cycle Management (M4A).
 * Lifecycle: PLANNED -> OPEN -> CLOSED (uses existing ASSESSMENT_CYCLE_STATUS lookup).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AssessmentCycleService {

    private static final String ENTITY_CYCLE = "ASSESSMENT_CYCLE";
    private static final String STATUS_PLANNED = "PLANNED";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String LOOKUP_TYPE = "ASSESSMENT_CYCLE_STATUS";

    private final AssessmentCycleRepository cycleRepository;
    private final ApplicationRepository applicationRepository;
    private final RequirementVersionRepository requirementVersionRepository;
    private final ExpectedRatingRepository expectedRatingRepository;
    private final TechnicianAssessmentHeaderRepository headerRepository;
    private final LookupValueRepository lookupValueRepository;
    private final AuditService auditService;
    private final java.time.Clock clock;

    // ---------------------------------------------------------------
    // List / search
    // ---------------------------------------------------------------

    public PagedResponse<AssessmentCycleDto> getCycles(
            int page, int size, String search, String applicationPublicId, String statusCode,
            String sortBy, String sortDirection) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                blank(sortBy) ? "createdAt" : sortBy);
        Page<AssessmentCycle> result = cycleRepository.searchCycles(
                blank(search) ? null : search.trim(),
                blank(applicationPublicId) ? null : applicationPublicId.trim(),
                blank(statusCode) ? null : statusCode.trim(),
                PageRequest.of(page, size, sort));
        return PagedResponse.of(result.getContent().stream().map(AssessmentCycleDto::from).toList(), result);
    }

    public AssessmentCycleDto getByPublicId(String publicId) {
        return AssessmentCycleDto.from(findOrThrow(publicId));
    }

    // ---------------------------------------------------------------
    // Create (DRAFT/PLANNED)
    // ---------------------------------------------------------------

    @Transactional
    public AssessmentCycleDto createCycle(CreateAssessmentCycleRequest req, String actorUsername) {
        Application app = applicationRepository.findByPublicId(req.applicationPublicId())
                .orElseThrow(() -> new BusinessException("Invalid applicationPublicId: " + req.applicationPublicId()));
        if (!app.isActive()) {
            throw new BusinessException("Application is inactive: " + app.getApplicationCode());
        }

        RequirementVersion rv = requirementVersionRepository.findByPublicId(req.requirementVersionPublicId())
                .orElseThrow(() -> new BusinessException("Invalid requirementVersionPublicId: " + req.requirementVersionPublicId()));
        if (!rv.getApplication().getId().equals(app.getId())) {
            throw new BusinessException("Requirement version does not belong to the selected application");
        }

        validateDates(req.cycleStartDate(), req.cycleEndDate());

        String trimmedName = req.cycleName().trim();
        cycleRepository.findByApplication_IdAndCycleNameIgnoreCase(app.getId(), trimmedName)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "An assessment cycle named '" + existing.getCycleName() + "' already exists for "
                                    + app.getApplicationName() + ". Please use a unique cycle name for the new assessment period.",
                            HttpStatus.CONFLICT);
                });

        var plannedStatus = lookupValueRepository.findByTypeCodeAndValueCode(LOOKUP_TYPE, STATUS_PLANNED).orElse(null);

        AssessmentCycle cycle = AssessmentCycle.builder()
                .publicId(UUID.randomUUID().toString())
                .application(app)
                .requirementVersion(rv)
                .cycleName(req.cycleName().trim())
                .periodTypeCode(blank(req.periodTypeCode()) ? null : req.periodTypeCode().trim())
                .periodValue(blank(req.periodValue()) ? null : req.periodValue().trim())
                .periodYear(req.periodYear())
                .cycleStartDate(req.cycleStartDate())
                .cycleEndDate(req.cycleEndDate())
                .statusLookup(plannedStatus)
                .active(true)
                .createdBy(actorUsername)
                .updatedBy(actorUsername)
                .build();

        AssessmentCycle saved = cycleRepository.save(cycle);
        log.info("Assessment cycle created: publicId={} application={} cycleName={} by={}",
                saved.getPublicId(), app.getApplicationCode(), saved.getCycleName(), actorUsername);
        auditService.log("CREATE_ASSESSMENT_CYCLE", ENTITY_CYCLE, null, actorUsername, saved.getPublicId(),
                null, null, "{\"cycleName\":\"" + saved.getCycleName() + "\",\"application\":\"" + app.getApplicationCode() + "\"}");

        return AssessmentCycleDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Update (only while PLANNED and no header exists yet)
    // ---------------------------------------------------------------

    @Transactional
    public AssessmentCycleDto updateCycle(String publicId, UpdateAssessmentCycleRequest req, String actorUsername) {
        AssessmentCycle cycle = findOrThrow(publicId);

        if (!STATUS_PLANNED.equals(statusCode(cycle))) {
            throw new BusinessException("Only PLANNED cycles can be edited", HttpStatus.CONFLICT);
        }

        validateDates(req.cycleStartDate(), req.cycleEndDate());

        boolean hasHeaders = headerRepository.existsByCycle_Id(cycle.getId());

        if (!blank(req.applicationPublicId()) || !blank(req.requirementVersionPublicId())) {
            if (hasHeaders) {
                throw new BusinessException(
                        "Application/Requirement Version cannot be changed after technician assessments have started",
                        HttpStatus.CONFLICT);
            }
            if (!blank(req.applicationPublicId())) {
                Application app = applicationRepository.findByPublicId(req.applicationPublicId())
                        .orElseThrow(() -> new BusinessException("Invalid applicationPublicId: " + req.applicationPublicId()));
                cycle.setApplication(app);
            }
            if (!blank(req.requirementVersionPublicId())) {
                RequirementVersion rv = requirementVersionRepository.findByPublicId(req.requirementVersionPublicId())
                        .orElseThrow(() -> new BusinessException("Invalid requirementVersionPublicId: " + req.requirementVersionPublicId()));
                if (!rv.getApplication().getId().equals(cycle.getApplication().getId())) {
                    throw new BusinessException("Requirement version does not belong to the selected application");
                }
                cycle.setRequirementVersion(rv);
            }
        }

        cycle.setCycleName(req.cycleName().trim());
        cycle.setPeriodTypeCode(blank(req.periodTypeCode()) ? null : req.periodTypeCode().trim());
        cycle.setPeriodValue(blank(req.periodValue()) ? null : req.periodValue().trim());
        cycle.setPeriodYear(req.periodYear());
        cycle.setCycleStartDate(req.cycleStartDate());
        cycle.setCycleEndDate(req.cycleEndDate());
        cycle.setUpdatedBy(actorUsername);

        AssessmentCycle saved = cycleRepository.save(cycle);
        log.info("Assessment cycle updated: publicId={} by={}", publicId, actorUsername);
        auditService.log("UPDATE_ASSESSMENT_CYCLE", ENTITY_CYCLE, null, actorUsername, publicId, null, null,
                "{\"cycleName\":\"" + saved.getCycleName() + "\"}");

        return AssessmentCycleDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Open
    // ---------------------------------------------------------------

    @Transactional
    public AssessmentCycleDto openCycle(String publicId, String actorUsername) {
        AssessmentCycle cycle = findOrThrow(publicId);

        if (!STATUS_PLANNED.equals(statusCode(cycle))) {
            throw new BusinessException("Only PLANNED cycles can be opened", HttpStatus.CONFLICT);
        }
        if (!cycle.getApplication().isActive()) {
            throw new BusinessException("Application is inactive: " + cycle.getApplication().getApplicationCode());
        }

        List<ExpectedRating> ratings = expectedRatingRepository.findAllByRequirementVersionId(cycle.getRequirementVersion().getId());
        boolean hasActive = ratings.stream().anyMatch(ExpectedRating::isActive);
        if (!hasActive) {
            throw new BusinessException("Requirement version has zero active skill requirements — cannot open cycle");
        }

        var openStatus = lookupValueRepository.findByTypeCodeAndValueCode(LOOKUP_TYPE, STATUS_OPEN).orElse(null);
        cycle.setStatusLookup(openStatus);
        cycle.setUpdatedBy(actorUsername);

        AssessmentCycle saved = cycleRepository.save(cycle);
        log.info("Assessment cycle opened: publicId={} by={}", publicId, actorUsername);
        auditService.log("OPEN_ASSESSMENT_CYCLE", ENTITY_CYCLE, null, actorUsername, publicId, null, null, null);

        return AssessmentCycleDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Close
    // ---------------------------------------------------------------

    @Transactional
    public AssessmentCycleDto closeCycle(String publicId, String actorUsername) {
        AssessmentCycle cycle = findOrThrow(publicId);

        if (!STATUS_OPEN.equals(statusCode(cycle))) {
            throw new BusinessException("Only OPEN cycles can be closed", HttpStatus.CONFLICT);
        }

        var closedStatus = lookupValueRepository.findByTypeCodeAndValueCode(LOOKUP_TYPE, STATUS_CLOSED).orElse(null);
        cycle.setStatusLookup(closedStatus);
        cycle.setUpdatedBy(actorUsername);

        AssessmentCycle saved = cycleRepository.save(cycle);
        log.info("Assessment cycle closed: publicId={} by={}", publicId, actorUsername);
        auditService.log("CLOSE_ASSESSMENT_CYCLE", ENTITY_CYCLE, null, actorUsername, publicId, null, null, null);

        return AssessmentCycleDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Reopen (CLOSED -> OPEN, M4 stabilization)
    // ---------------------------------------------------------------

    /**
     * Controlled CLOSED -> OPEN transition. Reopening the cycle only reopens the
     * assessment *period* — it never touches existing TechnicianAssessmentHeader /
     * AssessmentItem rows, so DRAFT assessments remain DRAFT (resumable) and
     * SUBMITTED assessments remain SUBMITTED/read-only. Individual return/review
     * workflow is out of scope for M4 (belongs to M5).
     */
    @Transactional
    public AssessmentCycleDto reopenCycle(String publicId, ReopenAssessmentCycleRequest req, String actorUsername) {
        AssessmentCycle cycle = findOrThrow(publicId);

        if (!STATUS_CLOSED.equals(statusCode(cycle))) {
            throw new BusinessException("Only CLOSED cycles can be reopened", HttpStatus.CONFLICT);
        }

        if (req.reason() == null || req.reason().isBlank()) {
            throw new BusinessException("A reopen reason is required");
        }

        if (!cycle.getApplication().isActive()) {
            throw new BusinessException("Application is inactive: " + cycle.getApplication().getApplicationCode());
        }

        long finalizedApprovals = cycleRepository.countFinalizedApprovalsForCycle(cycle.getId());
        if (finalizedApprovals > 0) {
            throw new BusinessException(
                    "This assessment cycle cannot be reopened because downstream review/approval records already exist.",
                    HttpStatus.CONFLICT);
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate previousEndDate = cycle.getCycleEndDate();
        LocalDate newEndDate = req.newEndDate();

        boolean expired = previousEndDate != null && today.isAfter(previousEndDate);
        if (expired) {
            if (newEndDate == null) {
                throw new BusinessException(
                        "This assessment cycle has expired. Extend the End Date before reopening.", HttpStatus.CONFLICT);
            }
            if (newEndDate.isBefore(today)) {
                throw new BusinessException("New End Date must be on or after today.");
            }
            if (cycle.getCycleStartDate() != null && newEndDate.isBefore(cycle.getCycleStartDate())) {
                throw new BusinessException("New End Date must be on or after the cycle start date.");
            }
            cycle.setCycleEndDate(newEndDate);
        } else if (newEndDate != null) {
            // Not expired, but Admin optionally supplied a new end date anyway — validate the same way.
            if (cycle.getCycleStartDate() != null && newEndDate.isBefore(cycle.getCycleStartDate())) {
                throw new BusinessException("New End Date must be on or after the cycle start date.");
            }
            cycle.setCycleEndDate(newEndDate);
        }

        var openStatus = lookupValueRepository.findByTypeCodeAndValueCode(LOOKUP_TYPE, STATUS_OPEN).orElse(null);
        cycle.setStatusLookup(openStatus);
        cycle.setUpdatedBy(actorUsername);

        AssessmentCycle saved = cycleRepository.save(cycle);
        log.info("Assessment cycle reopened: publicId={} by={} reason={}", publicId, actorUsername, req.reason());

        String appliedEndDate = saved.getCycleEndDate() != null ? saved.getCycleEndDate().toString() : null;
        String auditDetails = "{\"cycleName\":\"" + saved.getCycleName() + "\""
                + ",\"previousStatus\":\"CLOSED\",\"newStatus\":\"OPEN\""
                + ",\"reason\":\"" + req.reason().replace("\"", "\\\"") + "\""
                + ",\"previousEndDate\":\"" + previousEndDate + "\""
                + ",\"newEndDate\":\"" + appliedEndDate + "\"}";
        auditService.log("ASSESSMENT_CYCLE_REOPENED", ENTITY_CYCLE, null, actorUsername, publicId,
                null, null, auditDetails);

        return AssessmentCycleDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    AssessmentCycle findOrThrow(String publicId) {
        return cycleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("Assessment cycle not found: " + publicId, HttpStatus.NOT_FOUND));
    }

    private String statusCode(AssessmentCycle cycle) {
        return cycle.getStatusLookup() != null ? cycle.getStatusLookup().getValueCode() : null;
    }

    private void validateDates(java.time.LocalDate start, java.time.LocalDate end) {
        if (start == null || end == null) {
            throw new BusinessException("cycleStartDate and cycleEndDate are required");
        }
        if (end.isBefore(start)) {
            throw new BusinessException("cycleEndDate must be on or after cycleStartDate");
        }
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
