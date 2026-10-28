package com.skillmatrix.requirements.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.*;
import com.skillmatrix.domain.repository.ExpectedRatingRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.RequirementVersionRepository;
import com.skillmatrix.domain.repository.SkillRepository;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.requirements.dto.CreateSkillRequirementRequest;
import com.skillmatrix.requirements.dto.RequirementStatusRequest;
import com.skillmatrix.requirements.dto.SkillRequirementDto;
import com.skillmatrix.requirements.dto.UpdateSkillRequirementRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillRequirementService unit tests")
class SkillRequirementServiceTest {

    @Mock ExpectedRatingRepository expectedRatingRepository;
    @Mock RequirementVersionRepository requirementVersionRepository;
    @Mock SkillRepository skillRepository;
    @Mock LookupValueRepository lookupValueRepository;
    @Mock AuditService auditService;

    @InjectMocks SkillRequirementService service;

    private Application atlasApp;
    private RequirementVersion draftVersion;
    private Skill javaSkill;
    private ExpectedRating existingRating;

    @BeforeEach
    void setUp() {
        atlasApp = Application.builder()
                .id(1L).publicId("pub-app-atlas")
                .applicationCode("ATLAS_DEZENTRAL").applicationName("ATLAS-deZentral")
                .active(true).build();

        draftVersion = RequirementVersion.builder()
                .id(1L).publicId("pub-rv-1")
                .application(atlasApp).versionCode("v1.0").versionName("Draft v1")
                .active(true).build();

        javaSkill = Skill.builder()
                .id(1L).publicId("pub-skill-java")
                .skillCode("TECH_JAVA").skillName("Java")
                .active(true).build();

        existingRating = ExpectedRating.builder()
                .id(1L).publicId("pub-er-1")
                .requirementVersion(draftVersion).skill(javaSkill)
                .expectedLevel(3).mandatory(true).active(true).build();
    }

    // ---------------------------------------------------------------
    // Create — success
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createRequirement succeeds for valid application and skill")
    void createRequirement_success() {
        CreateSkillRequirementRequest req = new CreateSkillRequirementRequest(
                "pub-rv-1", "pub-skill-java", 3, true, null, null, null);

        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(draftVersion));
        when(skillRepository.findByPublicId("pub-skill-java")).thenReturn(Optional.of(javaSkill));
        when(expectedRatingRepository.existsByRequirementVersion_IdAndSkill_Id(1L, 1L)).thenReturn(false);
        when(expectedRatingRepository.save(any())).thenAnswer(inv -> {
            ExpectedRating er = inv.getArgument(0);
            er.setId(100L);
            return er;
        });

        SkillRequirementDto dto = service.createRequirement(req, "admin");

        assertThat(dto.skillCode()).isEqualTo("TECH_JAVA");
        assertThat(dto.expectedLevel()).isEqualTo(3);
        assertThat(dto.mandatory()).isTrue();
        verify(auditService).log(eq("CREATE_SKILL_REQUIREMENT"), any(), any(), any(), any(), any(), any(), any());
    }

    // ---------------------------------------------------------------
    // Create — invalid application
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createRequirement rejects invalid requirementVersionPublicId")
    void createRequirement_invalidVersion() {
        CreateSkillRequirementRequest req = new CreateSkillRequirementRequest(
                "bad-version", "pub-skill-java", 3, true, null, null, null);

        when(requirementVersionRepository.findByPublicId("bad-version")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRequirement(req, "admin"))
                .isInstanceOf(BusinessException.class);
    }

    // ---------------------------------------------------------------
    // Create — inactive application
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createRequirement rejects inactive application")
    void createRequirement_inactiveApplication() {
        atlasApp.setActive(false);
        CreateSkillRequirementRequest req = new CreateSkillRequirementRequest(
                "pub-rv-1", "pub-skill-java", 3, true, null, null, null);

        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(draftVersion));
        when(skillRepository.findByPublicId("pub-skill-java")).thenReturn(Optional.of(javaSkill));

        assertThatThrownBy(() -> service.createRequirement(req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
    }

    // ---------------------------------------------------------------
    // Create — invalid skill
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createRequirement rejects invalid skillPublicId")
    void createRequirement_invalidSkill() {
        CreateSkillRequirementRequest req = new CreateSkillRequirementRequest(
                "pub-rv-1", "bad-skill", 3, true, null, null, null);

        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(draftVersion));
        when(skillRepository.findByPublicId("bad-skill")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRequirement(req, "admin"))
                .isInstanceOf(BusinessException.class);
    }

    // ---------------------------------------------------------------
    // Create — inactive skill
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createRequirement rejects inactive skill")
    void createRequirement_inactiveSkill() {
        javaSkill.setActive(false);
        CreateSkillRequirementRequest req = new CreateSkillRequirementRequest(
                "pub-rv-1", "pub-skill-java", 3, true, null, null, null);

        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(draftVersion));
        when(skillRepository.findByPublicId("pub-skill-java")).thenReturn(Optional.of(javaSkill));

        assertThatThrownBy(() -> service.createRequirement(req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
    }

    // ---------------------------------------------------------------
    // Create — duplicate prevention
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createRequirement prevents duplicate mapping (version + skill)")
    void createRequirement_duplicatePrevented() {
        CreateSkillRequirementRequest req = new CreateSkillRequirementRequest(
                "pub-rv-1", "pub-skill-java", 4, true, null, null, null);

        when(requirementVersionRepository.findByPublicId("pub-rv-1")).thenReturn(Optional.of(draftVersion));
        when(skillRepository.findByPublicId("pub-skill-java")).thenReturn(Optional.of(javaSkill));
        when(expectedRatingRepository.existsByRequirementVersion_IdAndSkill_Id(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createRequirement(req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateRequirement changes expected level and mandatory flag")
    void updateRequirement_success() {
        when(expectedRatingRepository.findByPublicId("pub-er-1")).thenReturn(Optional.of(existingRating));
        when(expectedRatingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateSkillRequirementRequest req = new UpdateSkillRequirementRequest(4, false, null, null, null);
        SkillRequirementDto dto = service.updateRequirement("pub-er-1", req, "admin");

        assertThat(dto.expectedLevel()).isEqualTo(4);
        assertThat(dto.mandatory()).isFalse();
        verify(auditService).log(eq("UPDATE_SKILL_REQUIREMENT"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateRequirement throws 404 when not found")
    void updateRequirement_notFound() {
        when(expectedRatingRepository.findByPublicId("bad-id")).thenReturn(Optional.empty());
        UpdateSkillRequirementRequest req = new UpdateSkillRequirementRequest(4, false, null, null, null);

        assertThatThrownBy(() -> service.updateRequirement("bad-id", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // Status toggle
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateStatus deactivates skill requirement")
    void updateStatus_deactivate() {
        when(expectedRatingRepository.findByPublicId("pub-er-1")).thenReturn(Optional.of(existingRating));
        when(expectedRatingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SkillRequirementDto dto = service.updateStatus("pub-er-1", new RequirementStatusRequest(false, null), "admin");

        assertThat(dto.active()).isFalse();
        verify(auditService).log(eq("DEACTIVATE_SKILL_REQUIREMENT"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus activates skill requirement")
    void updateStatus_activate() {
        existingRating.setActive(false);
        when(expectedRatingRepository.findByPublicId("pub-er-1")).thenReturn(Optional.of(existingRating));
        when(expectedRatingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SkillRequirementDto dto = service.updateStatus("pub-er-1", new RequirementStatusRequest(true, null), "admin");

        assertThat(dto.active()).isTrue();
        verify(auditService).log(eq("ACTIVATE_SKILL_REQUIREMENT"), any(), any(), any(), any(), any(), any(), any());
    }

    // ---------------------------------------------------------------
    // Get by publicId
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getByPublicId returns DTO when found")
    void getByPublicId_found() {
        when(expectedRatingRepository.findByPublicId("pub-er-1")).thenReturn(Optional.of(existingRating));
        SkillRequirementDto dto = service.getByPublicId("pub-er-1");
        assertThat(dto.skillCode()).isEqualTo("TECH_JAVA");
    }

    @Test
    @DisplayName("getByPublicId throws 404 when not found")
    void getByPublicId_notFound() {
        when(expectedRatingRepository.findByPublicId("bad-id")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByPublicId("bad-id"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // getRequirements — controlled failure vs. normal empty-list behavior
    //
    // Regression coverage for the Skill Requirement Mapping cascade defect:
    // selecting a Requirement Version must never surface a generic 500.
    // An unknown requirementVersionPublicId must fail in a controlled way
    // (BusinessException -> 400 via GlobalExceptionHandler), and a VALID
    // version with zero expected_ratings must return a normal, empty
    // PagedResponse (HTTP 200), never an exception.
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getRequirements throws a controlled BusinessException (not a 500) for an unknown requirementVersionPublicId")
    void getRequirements_unknownVersion_throwsControlledException() {
        when(requirementVersionRepository.findByPublicId("unknown-version"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequirements(
                0, 20, "unknown-version", null, null, null, null, null, "skillCode", "asc"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getRequirements returns an empty PagedResponse (HTTP 200) for a valid version with zero requirements")
    void getRequirements_validVersionZeroRequirements_returnsEmptyPage() {
        when(requirementVersionRepository.findByPublicId("pub-rv-1"))
                .thenReturn(Optional.of(draftVersion));
        when(expectedRatingRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PagedResponse<SkillRequirementDto> result = service.getRequirements(
                0, 20, "pub-rv-1", null, null, null, null, null, "skillCode", "asc");

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }
}
