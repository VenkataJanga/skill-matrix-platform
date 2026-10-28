package com.skillmatrix.requirements.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.ExpectedRating;
import com.skillmatrix.domain.entity.RequirementVersion;
import com.skillmatrix.domain.entity.Skill;
import com.skillmatrix.domain.repository.ExpectedRatingRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.RequirementVersionRepository;
import com.skillmatrix.domain.repository.SkillRepository;
import com.skillmatrix.requirements.dto.CreateSkillRequirementRequest;
import com.skillmatrix.requirements.dto.RequirementStatusRequest;
import com.skillmatrix.requirements.dto.SkillRequirementDto;
import com.skillmatrix.requirements.dto.UpdateSkillRequirementRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Admin service for Skill Requirement Mapping (ExpectedRating) CRUD.
 * Answers: "Which application requires the skill, is it mandatory,
 * and what proficiency level is expected?"
 *
 * Does NOT create or modify Skill Master records — skills are selected
 * from the existing Skill Master catalogue only.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SkillRequirementService {

    private static final String ENTITY_SKILL_REQUIREMENT = "SKILL_REQUIREMENT";

    private final ExpectedRatingRepository expectedRatingRepository;
    private final RequirementVersionRepository requirementVersionRepository;
    private final SkillRepository skillRepository;
    private final LookupValueRepository lookupValueRepository;
    private final AuditService auditService;

    // ---------------------------------------------------------------
    // List / search
    // ---------------------------------------------------------------

    public PagedResponse<SkillRequirementDto> getRequirements(
            int page, int size,
            String requirementVersionPublicId, String applicationPublicId,
            String search, String skillGroup, String mandatoryFilter, String status,
            String sortBy, String sortDirection) {

        Long versionId = null;
        if (!blank(requirementVersionPublicId)) {
            RequirementVersion rv = requirementVersionRepository.findByPublicId(requirementVersionPublicId)
                    .orElseThrow(() -> new BusinessException("Requirement version not found: " + requirementVersionPublicId));
            versionId = rv.getId();
        }

        Boolean mandatory = blank(mandatoryFilter) ? null
                : ("Y".equalsIgnoreCase(mandatoryFilter) || "true".equalsIgnoreCase(mandatoryFilter));
        Boolean activeFilter = parseActiveFilter(status);

        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                blank(sortBy) ? "expectedLevel" : mapSortField(sortBy));

        var spec = SkillRequirementSpecification.build(
                versionId, blank(applicationPublicId) ? null : applicationPublicId,
                blank(search) ? null : search.trim(),
                blank(skillGroup) ? null : skillGroup.trim(),
                mandatory, activeFilter);

        Page<ExpectedRating> result = expectedRatingRepository.findAll(spec, PageRequest.of(page, size, sort));
        return PagedResponse.of(result.getContent().stream().map(SkillRequirementDto::from).toList(), result);
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "skillCode" -> "skill.skillCode";
            case "skillName" -> "skill.skillName";
            default -> "expectedLevel";
        };
    }

    public SkillRequirementDto getByPublicId(String publicId) {
        return SkillRequirementDto.from(findOrThrow(publicId));
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Transactional
    public SkillRequirementDto createRequirement(CreateSkillRequirementRequest req, String actorUsername) {
        RequirementVersion rv = requirementVersionRepository.findByPublicId(req.requirementVersionPublicId())
                .orElseThrow(() -> new BusinessException("Invalid requirementVersionPublicId: " + req.requirementVersionPublicId()));

        Skill skill = skillRepository.findByPublicId(req.skillPublicId())
                .orElseThrow(() -> new BusinessException("Invalid skillPublicId: " + req.skillPublicId()));

        if (!skill.isActive()) {
            throw new BusinessException("Skill is inactive: " + skill.getSkillCode());
        }
        if (!rv.getApplication().isActive()) {
            throw new BusinessException("Application is inactive: " + rv.getApplication().getApplicationCode());
        }
        if (expectedRatingRepository.existsByRequirementVersion_IdAndSkill_Id(rv.getId(), skill.getId())) {
            throw new BusinessException(
                    "A requirement already exists for skill " + skill.getSkillCode()
                            + " in version " + rv.getVersionCode(),
                    HttpStatus.CONFLICT);
        }

        ExpectedRating er = ExpectedRating.builder()
                .publicId(UUID.randomUUID().toString())
                .requirementVersion(rv)
                .skill(skill)
                .expectedLevel(req.expectedLevel())
                .mandatory(Boolean.TRUE.equals(req.mandatory()))
                .criticalityLookup(resolveLookup("CRITICALITY", req.criticalityCode()))
                .roleLookup(resolveLookup("ROLE_ON_APPLICATION", req.roleCode()))
                .gradeLookup(resolveLookup("GRADE", req.gradeCode()))
                .active(true)
                .createdBy(actorUsername)
                .updatedBy(actorUsername)
                .build();

        ExpectedRating saved = expectedRatingRepository.save(er);
        log.info("Skill requirement created: publicId={} application={} skill={} level={} mandatory={} by={}",
                saved.getPublicId(), rv.getApplication().getApplicationCode(), skill.getSkillCode(),
                saved.getExpectedLevel(), saved.isMandatory(), actorUsername);
        auditService.log("CREATE_SKILL_REQUIREMENT", ENTITY_SKILL_REQUIREMENT, null, actorUsername,
                saved.getPublicId(), null, null,
                "{\"skillCode\":\"" + skill.getSkillCode() + "\",\"expectedLevel\":" + saved.getExpectedLevel()
                        + ",\"mandatory\":" + saved.isMandatory() + "}");

        return SkillRequirementDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Transactional
    public SkillRequirementDto updateRequirement(String publicId, UpdateSkillRequirementRequest req, String actorUsername) {
        ExpectedRating er = findOrThrow(publicId);

        er.setExpectedLevel(req.expectedLevel());
        er.setMandatory(Boolean.TRUE.equals(req.mandatory()));
        er.setCriticalityLookup(resolveLookup("CRITICALITY", req.criticalityCode()));
        er.setRoleLookup(resolveLookup("ROLE_ON_APPLICATION", req.roleCode()));
        er.setGradeLookup(resolveLookup("GRADE", req.gradeCode()));
        er.setUpdatedBy(actorUsername);

        ExpectedRating saved = expectedRatingRepository.save(er);
        log.info("Skill requirement updated: publicId={} skill={} level={} mandatory={} by={}",
                publicId, saved.getSkill().getSkillCode(), saved.getExpectedLevel(), saved.isMandatory(), actorUsername);
        auditService.log("UPDATE_SKILL_REQUIREMENT", ENTITY_SKILL_REQUIREMENT, null, actorUsername, publicId, null, null,
                "{\"expectedLevel\":" + saved.getExpectedLevel() + ",\"mandatory\":" + saved.isMandatory() + "}");

        return SkillRequirementDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Status toggle
    // ---------------------------------------------------------------

    @Transactional
    public SkillRequirementDto updateStatus(String publicId, RequirementStatusRequest req, String actorUsername) {
        ExpectedRating er = findOrThrow(publicId);
        er.setActive(req.active());
        er.setUpdatedBy(actorUsername);
        ExpectedRating saved = expectedRatingRepository.save(er);
        String action = req.active() ? "ACTIVATE_SKILL_REQUIREMENT" : "DEACTIVATE_SKILL_REQUIREMENT";
        log.info("Skill requirement status changed: publicId={} active={} by={}", publicId, req.active(), actorUsername);
        auditService.log(action, ENTITY_SKILL_REQUIREMENT, null, actorUsername, publicId, null, null,
                "{\"active\":" + req.active() + "}");
        return SkillRequirementDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    ExpectedRating findOrThrow(String publicId) {
        return expectedRatingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("Skill requirement not found: " + publicId, HttpStatus.NOT_FOUND));
    }

    private com.skillmatrix.domain.entity.LookupValue resolveLookup(String typeCode, String valueCode) {
        if (blank(valueCode)) return null;
        return lookupValueRepository.findByTypeCodeAndValueCode(typeCode, valueCode).orElse(null);
    }

    private Boolean parseActiveFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        return "ACTIVE".equalsIgnoreCase(status) ? Boolean.TRUE : Boolean.FALSE;
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
