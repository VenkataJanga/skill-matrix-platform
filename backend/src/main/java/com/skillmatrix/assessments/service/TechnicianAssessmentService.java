package com.skillmatrix.assessments.service;

import com.skillmatrix.assessments.dto.SaveDraftRequest;
import com.skillmatrix.assessments.dto.TechnicianAssessmentDto;
import com.skillmatrix.assessments.dto.TechnicianAssessmentSummaryDto;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.*;
import com.skillmatrix.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Technician self-assessment service (M4B).
 * PLACEHOLDER — built incrementally.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TechnicianAssessmentService {

    private static final String ENTITY_ASSESSMENT = "TECHNICIAN_ASSESSMENT";
    private static final String CYCLE_LOOKUP_TYPE = "ASSESSMENT_CYCLE_STATUS";
    private static final String STATUS_LOOKUP_TYPE = "ASSESSMENT_STATUS";
    private static final String CYCLE_STATUS_OPEN = "OPEN";
    private static final String ASSESSMENT_STATUS_DRAFT = "DRAFT";
    private static final String ASSESSMENT_STATUS_SUBMITTED = "SUBMITTED";

    private final AssessmentCycleRepository cycleRepository;
    private final TechnicianAssessmentHeaderRepository headerRepository;
    private final AssessmentItemRepository itemRepository;
    private final ExpectedRatingRepository expectedRatingRepository;
    private final UserApplicationMappingRepository userApplicationMappingRepository;
    private final UserRepository userRepository;
    private final LookupValueRepository lookupValueRepository;
    private final AuditService auditService;
    private final Clock clock;

    // ---------------------------------------------------------------
    // My Assessments list
    // ---------------------------------------------------------------

    public List<TechnicianAssessmentSummaryDto> getMyAssessments(String username) {
        User user = currentUser(username);

        List<Long> activeAppIds = userApplicationMappingRepository.findByUser_IdAndActiveTrue(user.getId())
                .stream().map(m -> m.getApplication().getId()).distinct().toList();
        if (activeAppIds.isEmpty()) return List.of();

        List<AssessmentCycle> eligibleCycles = cycleRepository.findAllByApplicationIds(activeAppIds);

        Map<Long, TechnicianAssessmentHeader> headersByCycle = new HashMap<>();
        for (TechnicianAssessmentHeader h : headerRepository.findAllByUserId(user.getId())) {
            headersByCycle.put(h.getCycle().getId(), h);
        }

        List<TechnicianAssessmentSummaryDto> result = new ArrayList<>();
        for (AssessmentCycle cycle : eligibleCycles) {
            TechnicianAssessmentHeader header = headersByCycle.get(cycle.getId());
            String status;
            int total = 0, rated = 0;
            String assessmentPublicId = null;
            LocalDateTime updatedAt = null;

            if (header == null) {
                status = "NOT_STARTED";
            } else {
                assessmentPublicId = header.getPublicId();
                updatedAt = header.getUpdatedAt();
                var statusLookup = header.getStatusLookup();
                status = statusLookup != null ? statusLookup.getValueCode() : ASSESSMENT_STATUS_DRAFT;
                List<AssessmentItem> items = itemRepository.findAllByHeaderId(header.getId());
                total = items.size();
                rated = (int) items.stream().filter(i -> i.getSelfRating() != null).count();
            }

            result.add(new TechnicianAssessmentSummaryDto(
                    assessmentPublicId, cycle.getPublicId(), cycle.getCycleName(),
                    cycle.getApplication().getApplicationCode(), cycle.getApplication().getApplicationName(),
                    cycle.getCycleStartDate(), cycle.getCycleEndDate(), status, total, rated, updatedAt));
        }
        return result;
    }

    // ---------------------------------------------------------------
    // Start assessment (idempotent)
    // ---------------------------------------------------------------

    @Transactional
    public TechnicianAssessmentDto startAssessment(String cyclePublicId, String username) {
        User user = currentUser(username);
        AssessmentCycle cycle = cycleRepository.findByPublicId(cyclePublicId)
                .orElseThrow(() -> new BusinessException("Assessment cycle not found", HttpStatus.NOT_FOUND));

        String cycleStatus = cycle.getStatusLookup() != null ? cycle.getStatusLookup().getValueCode() : null;
        if (!CYCLE_STATUS_OPEN.equals(cycleStatus)) {
            throw new BusinessException("Assessment cycle is not currently open.", HttpStatus.CONFLICT);
        }
        validateWithinDateWindow(cycle, "Assessment cycle has not started yet.", "Assessment cycle has ended.");

        boolean assigned = userApplicationMappingRepository
                .existsByUser_IdAndApplication_IdAndActiveTrue(user.getId(), cycle.getApplication().getId());
        if (!assigned) {
            throw new BusinessException("You are not assigned to this application.", HttpStatus.FORBIDDEN);
        }

        // Idempotency: return existing header if one already exists for this technician+cycle.
        Optional<TechnicianAssessmentHeader> existing = headerRepository.findByCycle_IdAndUser_Id(cycle.getId(), user.getId());
        TechnicianAssessmentHeader header;
        if (existing.isPresent()) {
            header = existing.get();
        } else {
            header = createHeaderWithItems(cycle, user, username);
            log.info("Assessment started: publicId={} cycle={} user={}", header.getPublicId(), cycle.getCycleName(), username);
            auditService.log("START_ASSESSMENT", ENTITY_ASSESSMENT, user.getId(), username, header.getPublicId(),
                    null, null, "{\"cycle\":\"" + cycle.getCycleName() + "\"}");
        }

        List<AssessmentItem> items = itemRepository.findAllByHeaderId(header.getId());
        return TechnicianAssessmentDto.from(header, items);
    }

    /** Generates header + all items (one per active ExpectedRating) transactionally. */
    private TechnicianAssessmentHeader createHeaderWithItems(AssessmentCycle cycle, User user, String username) {
        var draftStatus = lookupValueRepository.findByTypeCodeAndValueCode(STATUS_LOOKUP_TYPE, ASSESSMENT_STATUS_DRAFT).orElse(null);

        TechnicianAssessmentHeader header = TechnicianAssessmentHeader.builder()
                .publicId(UUID.randomUUID().toString())
                .cycle(cycle)
                .user(user)
                .statusLookup(draftStatus)
                .createdBy(username)
                .updatedBy(username)
                .build();
        header = headerRepository.save(header);

        List<ExpectedRating> ratings = expectedRatingRepository.findAllByRequirementVersionId(cycle.getRequirementVersion().getId())
                .stream().filter(ExpectedRating::isActive).toList();

        List<AssessmentItem> items = new ArrayList<>();
        for (ExpectedRating er : ratings) {
            items.add(AssessmentItem.builder()
                    .publicId(UUID.randomUUID().toString())
                    .header(header)
                    .cycle(cycle)
                    .user(user)
                    .expectedRating(er)
                    .active(true)
                    .createdBy(username)
                    .updatedBy(username)
                    .build());
        }
        itemRepository.saveAll(items);
        return header;
    }

    // ---------------------------------------------------------------
    // Get assessment detail (ownership enforced)
    // ---------------------------------------------------------------

    public TechnicianAssessmentDto getAssessment(String assessmentPublicId, String username) {
        User user = currentUser(username);
        TechnicianAssessmentHeader header = ownedHeaderOrThrow(assessmentPublicId, user);
        List<AssessmentItem> items = itemRepository.findAllByHeaderId(header.getId());
        return TechnicianAssessmentDto.from(header, items);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private User currentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.UNAUTHORIZED));
    }

    /**
     * Resolves a header by publicId AND verifies it belongs to the given user.
     * IDOR protection: never trust publicId alone.
     */
    private TechnicianAssessmentHeader ownedHeaderOrThrow(String assessmentPublicId, User user) {
        TechnicianAssessmentHeader header = headerRepository.findDetailByPublicId(assessmentPublicId)
                .orElseThrow(() -> new BusinessException("Assessment not found", HttpStatus.NOT_FOUND));
        if (!header.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Assessment not found", HttpStatus.NOT_FOUND);
        }
        return header;
    }

    private String headerStatusCode(TechnicianAssessmentHeader header) {
        return header.getStatusLookup() != null ? header.getStatusLookup().getValueCode() : null;
    }

    // ---------------------------------------------------------------
    // Save draft
    // ---------------------------------------------------------------

    @Transactional
    public TechnicianAssessmentDto saveDraft(String assessmentPublicId, SaveDraftRequest request, String username) {
        User user = currentUser(username);
        TechnicianAssessmentHeader header = ownedHeaderOrThrow(assessmentPublicId, user);

        if (ASSESSMENT_STATUS_SUBMITTED.equals(headerStatusCode(header))) {
            throw new BusinessException("This assessment has already been submitted.", HttpStatus.CONFLICT);
        }
        String cycleStatus = header.getCycle().getStatusLookup() != null
                ? header.getCycle().getStatusLookup().getValueCode() : null;
        if (!CYCLE_STATUS_OPEN.equals(cycleStatus)) {
            throw new BusinessException("Assessment cycle is not currently open.", HttpStatus.CONFLICT);
        }
        validateWithinDateWindow(header.getCycle(),
                "Assessment cycle has not started yet. Draft changes can no longer be saved.",
                "Assessment cycle has ended. Draft changes can no longer be saved.");

        // Optimistic lock check — explicit version compare before touching the row,
        // in addition to JPA @Version doing the authoritative DB-level check on save.
        if (!Objects.equals(request.version(), header.getVersion())) {
            throw new BusinessException(
                    "This assessment was updated in another session. Refresh and try again.", HttpStatus.CONFLICT);
        }

        List<AssessmentItem> items = itemRepository.findAllByHeaderId(header.getId());
        Map<String, AssessmentItem> byPublicId = new HashMap<>();
        for (AssessmentItem i : items) byPublicId.put(i.getPublicId(), i);

        if (request.items() != null) {
            for (SaveDraftRequest.ItemRatingRequest ir : request.items()) {
                AssessmentItem item = byPublicId.get(ir.assessmentItemPublicId());
                if (item == null) {
                    throw new BusinessException("Invalid assessment item: " + ir.assessmentItemPublicId());
                }
                if (ir.selfRating() != null && (ir.selfRating() < 1 || ir.selfRating() > 5)) {
                    throw new BusinessException("selfRating must be between 1 and 5");
                }
                item.setSelfRating(ir.selfRating());
                item.setTechnicianComment(ir.comment());
                item.setEvidence(ir.evidence());
                item.setUpdatedBy(username);
            }
            itemRepository.saveAll(items);
        }

        header.setUpdatedBy(username);
        TechnicianAssessmentHeader saved = headerRepository.save(header);

        log.info("Assessment draft saved: publicId={} user={}", assessmentPublicId, username);

        return TechnicianAssessmentDto.from(saved, itemRepository.findAllByHeaderId(saved.getId()));
    }

    // ---------------------------------------------------------------
    // Submit
    // ---------------------------------------------------------------

    @Transactional
    public TechnicianAssessmentDto submit(String assessmentPublicId, String username) {
        User user = currentUser(username);
        TechnicianAssessmentHeader header = ownedHeaderOrThrow(assessmentPublicId, user);

        if (ASSESSMENT_STATUS_SUBMITTED.equals(headerStatusCode(header))) {
            throw new BusinessException("This assessment has already been submitted.", HttpStatus.CONFLICT);
        }

        String cycleStatus = header.getCycle().getStatusLookup() != null
                ? header.getCycle().getStatusLookup().getValueCode() : null;
        if (!CYCLE_STATUS_OPEN.equals(cycleStatus)) {
            throw new BusinessException("Assessment cycle is not currently open.", HttpStatus.CONFLICT);
        }
        validateWithinDateWindow(header.getCycle(),
                "Assessment cycle has not started yet and cannot be submitted.",
                "Assessment cycle has ended and can no longer be submitted.");

        boolean stillAssigned = userApplicationMappingRepository
                .existsByUser_IdAndApplication_IdAndActiveTrue(user.getId(), header.getCycle().getApplication().getId());
        if (!stillAssigned) {
            throw new BusinessException("You are not assigned to this application.", HttpStatus.FORBIDDEN);
        }

        List<AssessmentItem> items = itemRepository.findAllByHeaderId(header.getId());
        for (AssessmentItem item : items) {
            boolean mandatory = item.getExpectedRating().isMandatory();
            Integer rating = item.getSelfRating();
            if (mandatory && rating == null) {
                throw new BusinessException("Please rate all mandatory skills before submitting.");
            }
            if (rating != null && (rating < 1 || rating > 5)) {
                throw new BusinessException("All ratings must be between 1 and 5.");
            }
        }

        var submittedStatus = lookupValueRepository.findByTypeCodeAndValueCode(STATUS_LOOKUP_TYPE, ASSESSMENT_STATUS_SUBMITTED).orElse(null);
        header.setStatusLookup(submittedStatus);
        header.setSubmittedAt(LocalDateTime.now());
        header.setUpdatedBy(username);
        TechnicianAssessmentHeader saved = headerRepository.save(header);

        log.info("Assessment submitted: publicId={} user={}", assessmentPublicId, username);
        auditService.log("SUBMIT_ASSESSMENT", ENTITY_ASSESSMENT, user.getId(), username, assessmentPublicId,
                null, null, "{\"cycle\":\"" + header.getCycle().getCycleName() + "\"}");

        return TechnicianAssessmentDto.from(saved, items);
    }

    // ---------------------------------------------------------------
    // Date-window enforcement
    // ---------------------------------------------------------------

    /**
     * Validates that "today" (per the injected business {@link Clock}) falls within
     * the cycle's configured start/end date window. Status (OPEN/CLOSED/PLANNED) is
     * validated separately by the caller — this method only enforces the date window
     * for an already-OPEN cycle, per the M4 stabilization business rule:
     * <pre>
     * status = OPEN AND today >= cycleStartDate AND today <= cycleEndDate
     * </pre>
     */
    private void validateWithinDateWindow(AssessmentCycle cycle, String beforeStartMessage, String afterEndMessage) {
        LocalDate today = LocalDate.now(clock);
        if (cycle.getCycleStartDate() != null && today.isBefore(cycle.getCycleStartDate())) {
            throw new BusinessException(beforeStartMessage, HttpStatus.CONFLICT);
        }
        if (cycle.getCycleEndDate() != null && today.isAfter(cycle.getCycleEndDate())) {
            throw new BusinessException(afterEndMessage, HttpStatus.CONFLICT);
        }
    }
}
