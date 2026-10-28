package com.skillmatrix.applications.service;

import com.skillmatrix.applications.dto.ApplicationMasterDto;
import com.skillmatrix.applications.dto.ApplicationStatusRequest;
import com.skillmatrix.applications.dto.CreateApplicationRequest;
import com.skillmatrix.applications.dto.DeleteApplicationResult;
import com.skillmatrix.applications.dto.UpdateApplicationRequest;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.config.UserManagementProperties;
import com.skillmatrix.domain.entity.Application;
import com.skillmatrix.domain.entity.ApplicationPortfolio;
import com.skillmatrix.domain.repository.ApplicationPortfolioRepository;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.AssessmentCycleRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.RequirementVersionRepository;
import com.skillmatrix.domain.repository.SkillRepository;
import com.skillmatrix.domain.repository.UserApplicationMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Admin service for Application Master Management (M4 stabilization enhancement —
 * Problem Statement Part A). Application hierarchy is now fully data-driven: adding
 * applications never requires Java/Angular/Flyway/SQL changes going forward.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ApplicationMasterService {

    private static final String ENTITY_APPLICATION = "APPLICATION";
    private static final String LOOKUP_TYPE_LIFECYCLE = "APPLICATION_LIFECYCLE_STATUS";
    private static final String DEFAULT_LIFECYCLE_STATUS = "ACTIVE";

    private final ApplicationRepository applicationRepository;
    private final ApplicationPortfolioRepository portfolioRepository;
    private final LookupValueRepository lookupValueRepository;
    private final UserApplicationMappingRepository userApplicationMappingRepository;
    private final RequirementVersionRepository requirementVersionRepository;
    private final AssessmentCycleRepository assessmentCycleRepository;
    private final SkillRepository skillRepository;
    private final UserManagementProperties userManagementProperties;
    private final AuditService auditService;

    // ---------------------------------------------------------------
    // List / search
    // ---------------------------------------------------------------

    public PagedResponse<ApplicationMasterDto> getApplications(
            int page, int size, String search, String typeCode, String bundleCode,
            String portfolioCode, String lifecycleStatusCode, String status,
            String sortBy, String sortDirection) {

        Boolean activeFilter = parseActiveFilter(status);
        var sort = org.springframework.data.domain.Sort.by(
                "desc".equalsIgnoreCase(sortDirection)
                        ? org.springframework.data.domain.Sort.Direction.DESC
                        : org.springframework.data.domain.Sort.Direction.ASC,
                blank(sortBy) ? "applicationCode" : sortBy);

        Page<Application> result = applicationRepository.searchApplications(
                blank(search) ? null : search.trim(),
                blank(typeCode) ? null : typeCode.trim(),
                blank(bundleCode) ? null : bundleCode.trim(),
                blank(portfolioCode) ? null : portfolioCode.trim(),
                blank(lifecycleStatusCode) ? null : lifecycleStatusCode.trim(),
                activeFilter,
                PageRequest.of(page, size, sort));

        return PagedResponse.of(result.getContent().stream().map(ApplicationMasterDto::from).toList(), result);
    }

    public ApplicationMasterDto getByPublicId(String publicId) {
        return ApplicationMasterDto.from(findOrThrow(publicId));
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Transactional
    public ApplicationMasterDto createApplication(CreateApplicationRequest req, String actorUsername) {
        ApplicationPortfolio portfolio = resolveAndValidatePortfolio(
                req.applicationTypeCode(), req.bundleCode(), req.portfolioCode());

        String code = req.applicationCode().trim();
        if (applicationRepository.existsByPortfolio_IdAndApplicationCodeIgnoreCase(portfolio.getId(), code)) {
            throw new BusinessException(
                    "Application code '" + code + "' already exists for this hierarchy (Type/Bundle/Portfolio).",
                    HttpStatus.CONFLICT);
        }

        var lifecycleStatus = resolveLifecycleStatus(req.lifecycleStatusCode());

        Application app = Application.builder()
                .publicId(UUID.randomUUID().toString())
                .portfolio(portfolio)
                .applicationCode(code)
                .applicationName(req.applicationName().trim())
                .description(req.description())
                .remarks(req.remarks())
                .lifecycleStatusLookup(lifecycleStatus)
                .active(true)
                .createdBy(actorUsername)
                .updatedBy(actorUsername)
                .build();

        Application saved = applicationRepository.save(app);
        log.info("Application created: publicId={} code={} portfolio={} by={}",
                saved.getPublicId(), saved.getApplicationCode(), portfolio.getPortfolioCode(), actorUsername);
        auditService.log("APPLICATION_CREATED", ENTITY_APPLICATION, null, actorUsername, saved.getPublicId(),
                null, null, "{\"applicationCode\":\"" + saved.getApplicationCode() + "\",\"portfolio\":\""
                        + portfolio.getPortfolioCode() + "\"}");

        return ApplicationMasterDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Transactional
    public ApplicationMasterDto updateApplication(String publicId, UpdateApplicationRequest req, String actorUsername) {
        Application app = findOrThrow(publicId);

        boolean hierarchyChangeRequested = !blank(req.applicationTypeCode())
                || !blank(req.bundleCode()) || !blank(req.portfolioCode());

        if (hierarchyChangeRequested) {
            if (hasBusinessDependencies(app.getId())) {
                throw new BusinessException(
                        "Application Type/Bundle/Portfolio cannot be changed once this application has "
                                + "assignments, requirement versions, assessment cycles, or scoped skills.",
                        HttpStatus.CONFLICT);
            }
            String typeCode = !blank(req.applicationTypeCode())
                    ? req.applicationTypeCode() : app.getPortfolio().getApplicationType().getTypeCode();
            String bundleCode = !blank(req.bundleCode())
                    ? req.bundleCode() : app.getPortfolio().getBundle().getBundleCode();
            String portfolioCode = !blank(req.portfolioCode())
                    ? req.portfolioCode() : app.getPortfolio().getPortfolioCode();
            ApplicationPortfolio portfolio = resolveAndValidatePortfolio(typeCode, bundleCode, portfolioCode);
            app.setPortfolio(portfolio);
        }

        app.setApplicationName(req.applicationName().trim());
        app.setDescription(req.description());
        app.setRemarks(req.remarks());
        if (!blank(req.lifecycleStatusCode())) {
            app.setLifecycleStatusLookup(resolveLifecycleStatus(req.lifecycleStatusCode()));
        }
        app.setUpdatedBy(actorUsername);

        Application saved = applicationRepository.save(app);
        log.info("Application updated: publicId={} by={}", publicId, actorUsername);
        auditService.log("APPLICATION_UPDATED", ENTITY_APPLICATION, null, actorUsername, publicId, null, null,
                "{\"applicationName\":\"" + saved.getApplicationName() + "\"}");

        return ApplicationMasterDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Activate / Deactivate
    // ---------------------------------------------------------------

    @Transactional
    public ApplicationMasterDto updateStatus(String publicId, ApplicationStatusRequest req, String actorUsername) {
        Application app = findOrThrow(publicId);
        app.setActive(req.active());
        app.setUpdatedBy(actorUsername);
        Application saved = applicationRepository.save(app);

        String action = req.active() ? "APPLICATION_ACTIVATED" : "APPLICATION_DEACTIVATED";
        log.info("Application status changed: publicId={} active={} by={}", publicId, req.active(), actorUsername);
        auditService.log(action, ENTITY_APPLICATION, null, actorUsername, publicId, null, null,
                "{\"active\":" + req.active()
                        + (req.reason() != null ? ",\"reason\":\"" + req.reason().replace("\"", "\\\"") + "\"" : "")
                        + "}");

        return ApplicationMasterDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Delete (hard-delete, environment-controlled, dependency-blocked)
    // ---------------------------------------------------------------

    @Transactional
    public DeleteApplicationResult deleteApplication(String publicId, String actorUsername) {
        if (!userManagementProperties.isHardDeleteEnabled()) {
            throw new BusinessException(
                    "Hard delete is disabled in this environment. Use Deactivate instead.",
                    HttpStatus.LOCKED);
        }

        Application app = findOrThrow(publicId);

        if (hasBusinessDependencies(app.getId())) {
            String reason = "Application has business/history dependencies (assignments, requirement versions, "
                    + "assessment cycles, or scoped skills). Use Deactivate instead.";
            log.info("Application delete BLOCKED: publicId={} code={} reason={}",
                    publicId, app.getApplicationCode(), reason);
            return DeleteApplicationResult.blocked(publicId, app.getApplicationCode(), reason);
        }

        String code = app.getApplicationCode();
        applicationRepository.delete(app);
        log.info("Application DELETED: publicId={} code={} by={}", publicId, code, actorUsername);
        auditService.log("APPLICATION_DELETED", ENTITY_APPLICATION, null, actorUsername, publicId,
                null, null, "{\"applicationCode\":\"" + code + "\"}");

        return DeleteApplicationResult.deleted(publicId, code);
    }

    // ---------------------------------------------------------------
    // Hierarchy validation helpers
    // ---------------------------------------------------------------

    /**
     * Validates that typeCode/bundleCode/portfolioCode form a consistent, active
     * hierarchy combination per {@code application_portfolios}. Returns the resolved
     * portfolio or throws a business-friendly error identifying exactly which part
     * of the hierarchy is invalid.
     */
    private ApplicationPortfolio resolveAndValidatePortfolio(String typeCode, String bundleCode, String portfolioCode) {
        ApplicationPortfolio portfolio = portfolioRepository
                .findByPortfolioCodeAndTypeCodeAndBundleCode(portfolioCode.trim(), typeCode.trim(), bundleCode.trim())
                .orElseThrow(() -> new BusinessException(
                        "Invalid hierarchy: portfolio '" + portfolioCode + "' does not belong to Application Type '"
                                + typeCode + "' and Bundle '" + bundleCode + "'."));

        if (!portfolio.isActive()) {
            throw new BusinessException(
                    "Portfolio '" + portfolioCode + "' is inactive and cannot be used for a new active application.",
                    HttpStatus.CONFLICT);
        }
        if (!portfolio.getApplicationType().isActive()) {
            throw new BusinessException(
                    "Application Type '" + typeCode + "' is inactive and cannot be used for a new active application.",
                    HttpStatus.CONFLICT);
        }
        if (!portfolio.getBundle().isActive()) {
            throw new BusinessException(
                    "Bundle '" + bundleCode + "' is inactive and cannot be used for a new active application.",
                    HttpStatus.CONFLICT);
        }
        return portfolio;
    }

    private com.skillmatrix.domain.entity.LookupValue resolveLifecycleStatus(String lifecycleStatusCode) {
        String code = blank(lifecycleStatusCode) ? DEFAULT_LIFECYCLE_STATUS : lifecycleStatusCode.trim().toUpperCase();
        return lookupValueRepository.findByTypeCodeAndValueCode(LOOKUP_TYPE_LIFECYCLE, code)
                .orElseThrow(() -> new BusinessException("Invalid lifecycleStatusCode: " + code));
    }

    /**
     * Delete/hierarchy-change safety guard — checks every known FK that references
     * {@code applications.id}: assignments, requirement versions, assessment cycles,
     * and scoped skills. Mirrors {@code AdminUserDeleteService}'s dependency-check
     * pattern. Returns true if ANY dependency exists.
     */
    private boolean hasBusinessDependencies(Long applicationId) {
        return userApplicationMappingRepository.existsByApplication_Id(applicationId)
                || requirementVersionRepository.existsByApplication_Id(applicationId)
                || assessmentCycleRepository.existsByApplication_Id(applicationId)
                || skillRepository.existsByApplication_Id(applicationId);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    Application findOrThrow(String publicId) {
        return applicationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("Application not found: " + publicId, HttpStatus.NOT_FOUND));
    }

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
