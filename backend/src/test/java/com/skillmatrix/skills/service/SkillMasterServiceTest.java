package com.skillmatrix.skills.service;

import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.LookupType;
import com.skillmatrix.domain.entity.LookupValue;
import com.skillmatrix.domain.entity.Skill;
import com.skillmatrix.domain.entity.SkillCategory;
import com.skillmatrix.domain.repository.ApplicationRepository;
import com.skillmatrix.domain.repository.ApplicationTypeRepository;
import com.skillmatrix.domain.repository.BundleRepository;
import com.skillmatrix.domain.repository.LookupValueRepository;
import com.skillmatrix.domain.repository.SkillCategoryRepository;
import com.skillmatrix.domain.repository.SkillRepository;
import com.skillmatrix.skills.dto.CreateSkillRequest;
import com.skillmatrix.skills.dto.SkillDto;
import com.skillmatrix.skills.dto.SkillStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillMasterService unit tests")
class SkillMasterServiceTest {

    @Mock SkillRepository skillRepository;
    @Mock SkillCategoryRepository categoryRepository;
    @Mock LookupValueRepository lookupValueRepository;
    @Mock ApplicationTypeRepository appTypeRepository;
    @Mock BundleRepository bundleRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock AuditService auditService;

    @InjectMocks SkillMasterService service;

    private SkillCategory techCategory;
    private Skill sampleSkill;

    @BeforeEach
    void setUp() {
        techCategory = SkillCategory.builder()
                .id(1L).publicId(UUID.randomUUID().toString())
                .categoryCode("TECHNICAL").categoryName("Technical Skills")
                .active(true).build();

        sampleSkill = Skill.builder()
                .id(1L).publicId("pub-skill-1")
                .skillCode("SQL_QUERY").skillName("SQL Query Analysis")
                .skillCategory(techCategory).active(true).build();
    }

    // ---------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getSkills returns paginated results")
    void getSkills_paginated() {
        Page<Skill> page = new PageImpl<>(List.of(sampleSkill));
        when(skillRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<SkillDto> result = service.getSkills(0, 10, null, null, null, null, null, null, null, "skillCode", "asc");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).skillCode()).isEqualTo("SQL_QUERY");
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createSkill succeeds with GLOBAL scope and no app references")
    void createSkill_globalScope() {
        CreateSkillRequest req = new CreateSkillRequest(
                "NEW_SKILL", "New Skill", "A new skill",
                "TECHNICAL", "TECHNICAL", "GLOBAL",
                null, null, null);

        LookupValue globalScope = LookupValue.builder()
                .id(10L).valueCode("GLOBAL").valueLabel("Global (cross-platform)")
                .active(true).build();

        when(skillRepository.existsBySkillCode("NEW_SKILL")).thenReturn(false);
        when(categoryRepository.findByCategoryCode("TECHNICAL")).thenReturn(Optional.of(techCategory));
        when(lookupValueRepository.findByTypeCodeAndValueCode("SKILL_TYPE", "TECHNICAL")).thenReturn(Optional.empty());
        when(lookupValueRepository.findByTypeCodeAndValueCode("SKILL_SCOPE", "GLOBAL")).thenReturn(Optional.of(globalScope));
        when(skillRepository.save(any())).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        SkillDto dto = service.createSkill(req, "admin");

        assertThat(dto.skillCode()).isEqualTo("NEW_SKILL");
        verify(auditService).log(eq("CREATE_SKILL"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createSkill rejects duplicate skill code with 409")
    void createSkill_duplicateCode() {
        CreateSkillRequest req = new CreateSkillRequest(
                "SQL_QUERY", "Duplicate", null, "TECHNICAL", null, "GLOBAL",
                null, null, null);

        when(skillRepository.existsBySkillCode("SQL_QUERY")).thenReturn(true);

        assertThatThrownBy(() -> service.createSkill(req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("createSkill rejects invalid categoryCode")
    void createSkill_invalidCategory() {
        CreateSkillRequest req = new CreateSkillRequest(
                "NEW2", "New 2", null, "INVALID_CAT", null, "GLOBAL",
                null, null, null);

        when(skillRepository.existsBySkillCode("NEW2")).thenReturn(false);
        when(categoryRepository.findByCategoryCode("INVALID_CAT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSkill(req, "admin"))
                .isInstanceOf(BusinessException.class);
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getSkillByPublicId returns DTO")
    void getSkillByPublicId_found() {
        when(skillRepository.findByPublicId("pub-skill-1")).thenReturn(Optional.of(sampleSkill));
        SkillDto dto = service.getSkillByPublicId("pub-skill-1");
        assertThat(dto.skillCode()).isEqualTo("SQL_QUERY");
    }

    @Test
    @DisplayName("getSkillByPublicId throws 404 when not found")
    void getSkillByPublicId_notFound() {
        when(skillRepository.findByPublicId("bad-id")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSkillByPublicId("bad-id"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // Status
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateSkillStatus deactivates skill")
    void updateSkillStatus_deactivate() {
        when(skillRepository.findByPublicId("pub-skill-1")).thenReturn(Optional.of(sampleSkill));
        when(skillRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SkillDto dto = service.updateSkillStatus("pub-skill-1",
                new SkillStatusRequest(false, "No longer needed"), "admin");

        assertThat(dto.active()).isFalse();
        verify(auditService).log(eq("DEACTIVATE_SKILL"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateSkillStatus activates skill")
    void updateSkillStatus_activate() {
        sampleSkill.setActive(false);
        when(skillRepository.findByPublicId("pub-skill-1")).thenReturn(Optional.of(sampleSkill));
        when(skillRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SkillDto dto = service.updateSkillStatus("pub-skill-1",
                new SkillStatusRequest(true, "Reactivate"), "admin");

        assertThat(dto.active()).isTrue();
        verify(auditService).log(eq("ACTIVATE_SKILL"), any(), any(), any(), any(), any(), any(), any());
    }
}
