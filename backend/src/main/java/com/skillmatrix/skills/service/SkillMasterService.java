package com.skillmatrix.skills.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.Skill;
import com.skillmatrix.domain.entity.SkillCategory;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.ApplicationTypeRepository;
import com.skillmatrix.domain.repository.BundleRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.SkillCategoryRepository;
import com.skillmatrix.domain.repository.SkillRepository;
import com.skillmatrix.skills.dto.CreateSkillRequest;
import com.skillmatrix.skills.dto.SkillCategoryDto;
import com.skillmatrix.skills.dto.SkillDto;
import com.skillmatrix.skills.dto.SkillStatusRequest;
import com.skillmatrix.skills.dto.UpdateSkillRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SkillMasterService {

    private static final String ENTITY_SKILL = "SKILL";

    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;
    private final LookupValueRepository lookupValueRepository;
    private final ApplicationTypeRepository appTypeRepository;
    private final BundleRepository bundleRepository;
    private final ApplicationRepository applicationRepository;
    private final AuditService auditService;

    // ---------------------------------------------------------------
    // Categories
    // ---------------------------------------------------------------

    public List<SkillCategoryDto> getAllActiveCategories() {
        return categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()
                .stream().map(SkillCategoryDto::from).toList();
    }

    // ---------------------------------------------------------------
    // Paginated skill list
    // ---------------------------------------------------------------

    public PagedResponse<SkillDto> getSkills(
            int page, int size,
            String search, String categoryCode, String scopeCode,
            String appTypeCode, String bundleCode, String appPublicId,
            String status, String sortBy, String sortDirection) {

        Boolean activeFilter   = parseActiveFilter(status);
        String cleanSearch     = blank(search)       ? null : search.trim();
        String cleanCategory   = blank(categoryCode) ? null : categoryCode.trim();
        String cleanScope      = blank(scopeCode)    ? null : scopeCode.trim();
        String cleanAppType    = blank(appTypeCode)  ? null : appTypeCode.trim();
        String cleanBundle     = blank(bundleCode)   ? null : bundleCode.trim();
        String cleanApp        = blank(appPublicId)  ? null : appPublicId.trim();

        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                blank(sortBy) ? "skillCode" : sortBy);

        // Use JpaSpecificationExecutor to avoid Hibernate 6 null-Boolean JPQL issues.
        // The specification handles all optional filters safely in Criteria API.
        var spec = SkillSpecification.build(
                cleanSearch, cleanCategory, cleanScope,
                cleanAppType, cleanBundle, cleanApp, activeFilter);

        Page<Skill> result = skillRepository.findAll(spec, PageRequest.of(page, size, sort));

        return PagedResponse.of(result.getContent().stream().map(SkillDto::from).toList(), result);
    }

    // ---------------------------------------------------------------
    // Single skill
    // ---------------------------------------------------------------

    public SkillDto getSkillByPublicId(String publicId) {
        return SkillDto.from(findOrThrow(publicId));
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Transactional
    public SkillDto createSkill(CreateSkillRequest req, String actorUsername) {
        if (skillRepository.existsBySkillCode(req.skillCode())) {
            throw new BusinessException("Skill code already exists: " + req.skillCode(), HttpStatus.CONFLICT);
        }

        Skill skill = Skill.builder()
                .publicId(UUID.randomUUID().toString())
                .skillCode(req.skillCode().trim().toUpperCase())
                .skillName(req.skillName().trim())
                .description(req.description())
                .skillCategory(resolveCategory(req.categoryCode()))
                .skillTypeLookup(req.skillTypeCode() != null
                        ? lookupValueRepository.findByTypeCodeAndValueCode("SKILL_TYPE", req.skillTypeCode()).orElse(null)
                        : null)
                .skillScopeLookup(lookupValueRepository.findByTypeCodeAndValueCode("SKILL_SCOPE", req.scopeCode())
                        .orElseThrow(() -> new BusinessException("Invalid scopeCode: " + req.scopeCode())))
                .applicationType(req.applicationTypeCode() != null
                        ? appTypeRepository.findByTypeCode(req.applicationTypeCode())
                          .orElseThrow(() -> new BusinessException("Invalid applicationTypeCode: " + req.applicationTypeCode()))
                        : null)
                .bundle(req.bundleCode() != null
                        ? bundleRepository.findByBundleCode(req.bundleCode())
                          .orElseThrow(() -> new BusinessException("Invalid bundleCode: " + req.bundleCode()))
                        : null)
                .application(req.applicationPublicId() != null
                        ? applicationRepository.findByPublicId(req.applicationPublicId())
                          .orElseThrow(() -> new BusinessException("Invalid applicationPublicId: " + req.applicationPublicId()))
                        : null)
                .active(true)
                .createdBy(actorUsername)
                .updatedBy(actorUsername)
                .build();

        Skill saved = skillRepository.save(skill);
        log.info("Skill created: publicId={} skillCode={} scope={} by={}",
                saved.getPublicId(), saved.getSkillCode(), req.scopeCode(), actorUsername);
        auditService.log("CREATE_SKILL", ENTITY_SKILL, null, actorUsername,
                saved.getPublicId(), null, null,
                "{\"skillCode\":\"" + saved.getSkillCode() + "\"}");
        return SkillDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Transactional
    public SkillDto updateSkill(String publicId, UpdateSkillRequest req, String actorUsername) {
        Skill skill = findOrThrow(publicId);

        skill.setSkillName(req.skillName().trim());
        skill.setDescription(req.description());
        skill.setSkillCategory(resolveCategory(req.categoryCode()));
        skill.setSkillTypeLookup(req.skillTypeCode() != null
                ? lookupValueRepository.findByTypeCodeAndValueCode("SKILL_TYPE", req.skillTypeCode()).orElse(null)
                : null);
        skill.setSkillScopeLookup(lookupValueRepository.findByTypeCodeAndValueCode("SKILL_SCOPE", req.scopeCode())
                .orElseThrow(() -> new BusinessException("Invalid scopeCode: " + req.scopeCode())));
        skill.setApplicationType(req.applicationTypeCode() != null
                ? appTypeRepository.findByTypeCode(req.applicationTypeCode())
                  .orElseThrow(() -> new BusinessException("Invalid applicationTypeCode: " + req.applicationTypeCode()))
                : null);
        skill.setBundle(req.bundleCode() != null
                ? bundleRepository.findByBundleCode(req.bundleCode())
                  .orElseThrow(() -> new BusinessException("Invalid bundleCode: " + req.bundleCode()))
                : null);
        skill.setApplication(req.applicationPublicId() != null
                ? applicationRepository.findByPublicId(req.applicationPublicId())
                  .orElseThrow(() -> new BusinessException("Invalid applicationPublicId: " + req.applicationPublicId()))
                : null);
        skill.setUpdatedBy(actorUsername);

        Skill saved = skillRepository.save(skill);
        log.info("Skill updated: publicId={} skillCode={} by={}", publicId, saved.getSkillCode(), actorUsername);
        auditService.log("UPDATE_SKILL", ENTITY_SKILL, null, actorUsername, publicId, null, null,
                "{\"skillName\":\"" + saved.getSkillName() + "\"}");
        return SkillDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Status toggle
    // ---------------------------------------------------------------

    @Transactional
    public SkillDto updateSkillStatus(String publicId, SkillStatusRequest req, String actorUsername) {
        Skill skill = findOrThrow(publicId);
        skill.setActive(req.active());
        skill.setUpdatedBy(actorUsername);
        Skill saved = skillRepository.save(skill);
        String action = req.active() ? "ACTIVATE_SKILL" : "DEACTIVATE_SKILL";
        log.info("Skill status changed: publicId={} skillCode={} active={} by={}",
                publicId, saved.getSkillCode(), req.active(), actorUsername);
        auditService.log(action, ENTITY_SKILL, null, actorUsername, publicId, null, null,
                "{\"active\":" + req.active() + "}");
        return SkillDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    Skill findOrThrow(String publicId) {
        return skillRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("Skill not found: " + publicId, HttpStatus.NOT_FOUND));
    }

    SkillCategory resolveCategory(String categoryCode) {
        return categoryRepository.findByCategoryCode(categoryCode)
                .orElseThrow(() -> new BusinessException("Invalid categoryCode: " + categoryCode));
    }

    private Boolean parseActiveFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        return "ACTIVE".equalsIgnoreCase(status) ? Boolean.TRUE : Boolean.FALSE;
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
