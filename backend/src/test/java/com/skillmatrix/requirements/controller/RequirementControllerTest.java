package com.skillmatrix.requirements.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatrix.auth.service.AuthService;
import com.skillmatrix.auth.service.JwtService;
import com.skillmatrix.auth.service.RefreshTokenService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.config.AppConfig;
import com.skillmatrix.config.SecurityConfig;
import com.skillmatrix.domain.entity.Role;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserRole;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.requirements.dto.CreateSkillRequirementRequest;
import com.skillmatrix.requirements.dto.SkillRequirementDto;
import com.skillmatrix.requirements.service.RequirementImportService;
import com.skillmatrix.requirements.service.RequirementVersionService;
import com.skillmatrix.requirements.service.SkillRequirementService;
import com.skillmatrix.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = RequirementController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import({SecurityConfig.class, AppConfig.class})
@DisplayName("RequirementController — MockMvc tests")
class RequirementControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean RequirementVersionService requirementVersionService;
    @MockBean SkillRequirementService skillRequirementService;
    @MockBean RequirementImportService requirementImportService;
    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean RefreshTokenService refreshTokenService;
    @MockBean UserRepository userRepository;

    private UserPrincipal adminPrincipal;
    private UserPrincipal techPrincipal;
    private UserPrincipal leadPrincipal;

    private SkillRequirementDto sampleRequirement;

    @BeforeEach
    void setUp() {
        adminPrincipal = buildPrincipal("admin", "ADMIN");
        techPrincipal  = buildPrincipal("tech1", "TECHNICIAN");
        leadPrincipal  = buildPrincipal("lead1", "LEAD_MANAGER");

        sampleRequirement = new SkillRequirementDto(
                "pub-er-1", "pub-rv-1", "v1.0",
                "pub-app-atlas", "ATLAS_DEZENTRAL", "ATLAS-deZentral",
                "pub-skill-java", "TECH_JAVA", "Java",
                "COMMON_TECH", "TECHNICAL", "Technical Skills",
                3, true,
                null, null, null, null, null, null,
                true, LocalDateTime.now(), LocalDateTime.now(), "admin", "admin");
    }

    // ---------------------------------------------------------------
    // GET /admin/requirement-versions
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /admin/requirement-versions without eligibleOnly calls the unfiltered lookup")
    void getVersions_defaultCallsUnfiltered() throws Exception {
        when(requirementVersionService.getVersionsForApplication("pub-app-atlas")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/requirement-versions")
                        .param("applicationPublicId", "pub-app-atlas")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(requirementVersionService).getVersionsForApplication("pub-app-atlas");
        org.mockito.Mockito.verify(requirementVersionService, org.mockito.Mockito.never())
                .getEligibleVersionsForApplication(any());
    }

    @Test
    @DisplayName("GET /admin/requirement-versions?eligibleOnly=true calls the eligible-only lookup")
    void getVersions_eligibleOnlyCallsFilteredLookup() throws Exception {
        when(requirementVersionService.getEligibleVersionsForApplication("pub-app-avus")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/requirement-versions")
                        .param("applicationPublicId", "pub-app-avus")
                        .param("eligibleOnly", "true")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        org.mockito.Mockito.verify(requirementVersionService).getEligibleVersionsForApplication("pub-app-avus");
        org.mockito.Mockito.verify(requirementVersionService, org.mockito.Mockito.never())
                .getVersionsForApplication(any());
    }

    @Test
    @DisplayName("GET /admin/requirement-versions returns empty array (not an error) when application has no eligible version")
    void getVersions_emptyListIsNotError() throws Exception {
        when(requirementVersionService.getEligibleVersionsForApplication("pub-app-avus")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/requirement-versions")
                        .param("applicationPublicId", "pub-app-avus")
                        .param("eligibleOnly", "true")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---------------------------------------------------------------
    // GET /admin/skill-requirements
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can list skill requirements")
    void getRequirements_adminCan() throws Exception {
        PagedResponse<SkillRequirementDto> paged = new PagedResponse<>(
                List.of(sampleRequirement), 0, 20, 1, 1, false, false);
        when(skillRequirementService.getRequirements(
                anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(paged);

        mockMvc.perform(get("/api/v1/admin/skill-requirements").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].skillCode").value("TECH_JAVA"));
    }

    @Test
    @DisplayName("LEAD_MANAGER gets 403 on skill requirements list")
    void getRequirements_leadForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/skill-requirements").with(user(leadPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 on skill requirements list")
    void getRequirements_technicianForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/skill-requirements").with(user(techPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated gets 401 on skill requirements list")
    void getRequirements_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/skill-requirements"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // POST /admin/skill-requirements
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can create skill requirement — returns 201")
    void createRequirement_adminCan() throws Exception {
        when(skillRequirementService.createRequirement(any(), eq("admin"))).thenReturn(sampleRequirement);

        CreateSkillRequirementRequest req = new CreateSkillRequirementRequest(
                "pub-rv-1", "pub-skill-java", 3, true, null, null, null);

        mockMvc.perform(post("/api/v1/admin/skill-requirements")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skillCode").value("TECH_JAVA"));
    }

    @Test
    @DisplayName("POST /admin/skill-requirements returns 409 for duplicate mapping")
    void createRequirement_duplicateReturns409() throws Exception {
        when(skillRequirementService.createRequirement(any(), any()))
                .thenThrow(new BusinessException("Duplicate requirement", HttpStatus.CONFLICT));

        CreateSkillRequirementRequest req = new CreateSkillRequirementRequest(
                "pub-rv-1", "pub-skill-java", 3, true, null, null, null);

        mockMvc.perform(post("/api/v1/admin/skill-requirements")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /admin/skill-requirements returns 400 for missing required fields")
    void createRequirement_missingFields() throws Exception {
        mockMvc.perform(post("/api/v1/admin/skill-requirements")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 creating skill requirement")
    void createRequirement_technicianForbidden() throws Exception {
        CreateSkillRequirementRequest req = new CreateSkillRequirementRequest(
                "pub-rv-1", "pub-skill-java", 3, true, null, null, null);

        mockMvc.perform(post("/api/v1/admin/skill-requirements")
                        .with(user(techPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private UserPrincipal buildPrincipal(String username, String roleCode) {
        Role role = Role.builder()
                .id(1L).publicId("role-" + roleCode)
                .roleCode(roleCode).roleName(roleCode).active(true)
                .rolePermissions(new HashSet<>()).build();
        UserRole userRole = UserRole.builder().id(1L).role(role).active(true).build();
        User u = User.builder()
                .id(1L).publicId("pub-" + username)
                .username(username).email(username + "@nttdata.com")
                .fullName(username).passwordHash("$hash")
                .active(true).accountLocked(false).mustChangePassword(false)
                .userRoles(new HashSet<>(Set.of(userRole))).build();
        userRole.setUser(u);
        return new UserPrincipal(u);
    }
}
