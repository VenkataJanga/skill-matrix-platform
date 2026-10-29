package com.skillmatrix.requirements.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.Application;
import com.skillmatrix.domain.entity.RequirementVersion;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.ExpectedRatingRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.RequirementVersionRepository;
import com.skillmatrix.requirements.dto.CreateRequirementVersionRequest;
import com.skillmatrix.requirements.dto.RequirementVersionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Admin service for RequirementVersion lifecycle.
 * M3.1 scope: Admin can create and maintain DRAFT requirement versions.
 * Approval/activation workflow is a handoff prerequisite for M4 and is NOT
 * implemented here — versions are created with status DRAFT only.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RequirementVersionService {

    private static final String ENTITY_REQUIREMENT_VERSION = "REQUIREMENT_VERSION";
    private static final String STATUS_DRAFT = "DRAFT";

    private final RequirementVersionRepository requirementVersionRepository;
    private final ApplicationRepository applicationRepository;
    private final LookupValueRepository lookupValueRepository;
    private final ExpectedRatingRepository expectedRatingRepository;
    private final AuditService auditService;

    public List<RequirementVersionDto> getVersionsForApplication(String applicationPublicId) {
        return requirementVersionRepository.findAllByApplicationPublicId(applicationPublicId)
                .stream().map(this::toDtoWithActiveCount).toList();
    }

    /**
     * Returns only the Requirement Versions for an application that are eligible
     * for Assessment Cycle creation (active, not REJECTED). Used to populate the
     * Assessment Cycle "Requirement Version" dropdown so an Admin is never offered
     * a version that would be rejected by {@code AssessmentCycleService}. Returns
     * an empty list (never an error) when the application has no eligible version —
     * the caller is responsible for distinguishing "no data" from an API failure.
     *
     * Each returned DTO also carries {@code activeRequirementCount} so the Admin UI
     * can proactively warn (and disable "Save as PLANNED") when a version has zero
     * active skill requirements — BEFORE the Admin attempts to Open the cycle. The
     * backend Open-time safeguard in {@code AssessmentCycleService.openCycle()} is
     * unaffected and remains the final authoritative check.
     */
    public List<RequirementVersionDto> getEligibleVersionsForApplication(String applicationPublicId) {
        return requirementVersionRepository.findEligibleByApplicationPublicId(applicationPublicId)
                .stream().map(this::toDtoWithActiveCount).toList();
    }

    private RequirementVersionDto toDtoWithActiveCount(RequirementVersion rv) {
        long activeCount = expectedRatingRepository.countByRequirementVersion_IdAndActiveTrue(rv.getId());
        return RequirementVersionDto.from(rv, activeCount);
    }

    public RequirementVersionDto getByPublicId(String publicId) {
        return RequirementVersionDto.from(findOrThrow(publicId));
    }

    @Transactional
    public RequirementVersionDto createDraftVersion(CreateRequirementVersionRequest req, String actorUsername) {
        Application app = applicationRepository.findByPublicId(req.applicationPublicId())
                .orElseThrow(() -> new BusinessException("Invalid applicationPublicId: " + req.applicationPublicId()));

        if (!app.isActive()) {
            throw new BusinessException("Application is inactive: " + app.getApplicationCode());
        }

        if (requirementVersionRepository.existsByApplication_IdAndVersionCode(app.getId(), req.versionCode().trim())) {
            throw new BusinessException(
                    "Requirement version already exists for this application: " + req.versionCode(),
                    HttpStatus.CONFLICT);
        }

        var draftStatus = lookupValueRepository
                .findByTypeCodeAndValueCode("REQUIREMENT_STATUS", STATUS_DRAFT)
                .orElse(null);

        RequirementVersion rv = RequirementVersion.builder()
                .publicId(UUID.randomUUID().toString())
                .application(app)
                .versionCode(req.versionCode().trim())
                .versionName(req.versionName().trim())
                .description(req.description())
                .statusLookup(draftStatus)
                .active(true)
                .createdBy(actorUsername)
                .updatedBy(actorUsername)
                .build();

        RequirementVersion saved = requirementVersionRepository.save(rv);
        log.info("Requirement version created: publicId={} application={} versionCode={} by={}",
                saved.getPublicId(), app.getApplicationCode(), saved.getVersionCode(), actorUsername);
        auditService.log("CREATE_REQUIREMENT_VERSION", ENTITY_REQUIREMENT_VERSION, null, actorUsername,
                saved.getPublicId(), null, null,
                "{\"applicationCode\":\"" + app.getApplicationCode() + "\",\"versionCode\":\"" + saved.getVersionCode() + "\"}");

        return RequirementVersionDto.from(saved);
    }

    /**
     * Returns an existing DRAFT version for the application, or creates a new one
     * named "DRAFT-<timestamp>" if none exists. Used by the Excel import flow so
     * Admin does not have to pre-create a version before uploading.
     */
    @Transactional
    public RequirementVersion getOrCreateDraftVersion(Application app, String actorUsername) {
        List<RequirementVersion> drafts = requirementVersionRepository.findDraftsByApplicationId(app.getId());
        if (!drafts.isEmpty()) {
            return drafts.get(0);
        }
        var draftStatus = lookupValueRepository
                .findByTypeCodeAndValueCode("REQUIREMENT_STATUS", STATUS_DRAFT)
                .orElse(null);
        String versionCode = "DRAFT-IMPORT-" + System.currentTimeMillis();
        RequirementVersion rv = RequirementVersion.builder()
                .publicId(UUID.randomUUID().toString())
                .application(app)
                .versionCode(versionCode)
                .versionName("Draft requirements (import)")
                .statusLookup(draftStatus)
                .active(true)
                .createdBy(actorUsername)
                .updatedBy(actorUsername)
                .build();
        RequirementVersion saved = requirementVersionRepository.save(rv);
        log.info("Draft requirement version auto-created for import: publicId={} application={} versionCode={} by={}",
                saved.getPublicId(), app.getApplicationCode(), saved.getVersionCode(), actorUsername);
        return saved;
    }

    RequirementVersion findOrThrow(String publicId) {
        return requirementVersionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("Requirement version not found: " + publicId, HttpStatus.NOT_FOUND));
    }
}
