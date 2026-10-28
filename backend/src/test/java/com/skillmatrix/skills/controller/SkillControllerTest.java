package com.skillmatrix.skills.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatrix.auth.service.AuthService;
import com.skillmatrix.auth.service.JwtService;
import com.skillmatrix.auth.service.RefreshTokenService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.config.AppConfig;
import com.skillmatrix.config.SecurityConfig;
import com.skillmatrix.domain.entity.Role;
import com.skillmatrix.domain.entity.Skill;
import com.skillmatrix.domain.entity.SkillCategory;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserRole;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.security.UserPrincipal;
import com.skillmatrix.skills.dto.CreateSkillRequest;
import com.skillmatrix.skills.dto.SkillCategoryDto;
import com.skillmatrix.skills.dto.SkillDto;
import com.skillmatrix.skills.dto.SkillImportRowPreview;
import com.skillmatrix.skills.dto.SkillValidationResult;
import com.skillmatrix.skills.service.SkillImportService;
import com.skillmatrix.skills.service.SkillMasterService;
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

@WebMvcTest(value = SkillController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import({SecurityConfig.class, AppConfig.class})
@DisplayName("SkillController — MockMvc tests")
class SkillControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SkillMasterService skillMasterService;
    @MockBean SkillImportService skillImportService;
    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean RefreshTokenService refreshTokenService;
    @MockBean UserRepository userRepository;

    private UserPrincipal adminPrincipal;
    private UserPrincipal techPrincipal;
    private UserPrincipal leadPrincipal;

    private SkillDto sampleSkill;
    private SkillCategoryDto sampleCategory;

    @BeforeEach
    void setUp() {
        adminPrincipal = buildPrincipal("admin", "ADMIN");
        techPrincipal  = buildPrincipal("tech1", "TECHNICIAN");
        leadPrincipal  = buildPrincipal("lead1", "LEAD_MANAGER");

        sampleCategory = new SkillCategoryDto("pub-cat-1", "TECHNICAL", "Technical Skills", null, 1);

        sampleSkill = new SkillDto(
                "pub-skill-1", "SQL_QUERY", "SQL Query Analysis", "Description",
                "TECHNICAL", "Technical Skills",
                "TECHNICAL", "Technical",
                "GLOBAL", "Global",
                null, null, null, null, null, null, null,
                true, LocalDateTime.now(), LocalDateTime.now(), "admin", "admin");
    }

    // ---------------------------------------------------------------
    // GET /admin/skill-categories
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can get skill categories")
    void getCategories_adminCan() throws Exception {
        when(skillMasterService.getAllActiveCategories()).thenReturn(List.of(sampleCategory));

        mockMvc.perform(get("/api/v1/admin/skill-categories").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryCode").value("TECHNICAL"));
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 on skill categories")
    void getCategories_technicianForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/skill-categories").with(user(techPrincipal)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // GET /admin/skills
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can list skills")
    void getSkills_adminCan() throws Exception {
        PagedResponse<SkillDto> paged = new PagedResponse<>(
                List.of(sampleSkill), 0, 20, 1, 1, false, false);
        when(skillMasterService.getSkills(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(paged);

        mockMvc.perform(get("/api/v1/admin/skills").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].skillCode").value("SQL_QUERY"));
    }

    @Test
    @DisplayName("LEAD_MANAGER gets 403 on skills list")
    void getSkills_leadForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/skills").with(user(leadPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated gets 401 on skills list")
    void getSkills_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/skills"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // POST /admin/skills
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can create skill — returns 201")
    void createSkill_adminCan() throws Exception {
        when(skillMasterService.createSkill(any(), eq("admin"))).thenReturn(sampleSkill);

        CreateSkillRequest req = new CreateSkillRequest(
                "SQL_QUERY", "SQL Query Analysis", "Desc",
                "TECHNICAL", "TECHNICAL", "GLOBAL",
                null, null, null);

        mockMvc.perform(post("/api/v1/admin/skills")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skillCode").value("SQL_QUERY"));
    }

    @Test
    @DisplayName("POST /admin/skills returns 409 for duplicate skill code")
    void createSkill_duplicateReturns409() throws Exception {
        when(skillMasterService.createSkill(any(), any()))
                .thenThrow(new BusinessException("Skill code already exists", HttpStatus.CONFLICT));

        CreateSkillRequest req = new CreateSkillRequest(
                "SQL_QUERY", "Dup", null, "TECHNICAL", null, "GLOBAL",
                null, null, null);

        mockMvc.perform(post("/api/v1/admin/skills")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /admin/skills returns 400 for missing required fields")
    void createSkill_missingFields() throws Exception {
        mockMvc.perform(post("/api/v1/admin/skills")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillCode\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // POST /admin/import/skills/validate
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Validate endpoint returns validation result with errors")
    void validateImport_withErrors() throws Exception {
        // Build result using the new SMART_UPSERT SkillValidationResult.of() factory
        List<SkillImportRowPreview> rows = List.of(
                SkillImportRowPreview.error("01_COMMON_TECH_SKILLS", 3, null, null,
                        "skill_code is required", List.of("skill_code is required")),
                SkillImportRowPreview.error("01_COMMON_TECH_SKILLS", 4, null, null,
                        "skill_code is required", List.of("skill_code is required"))
        );
        SkillValidationResult result = SkillValidationResult.of(rows);

        when(skillImportService.validateOnly(any())).thenReturn(result);

        mockMvc.perform(multipart("/api/v1/admin/import/skills/validate")
                        .file("file", "skillCode,skillName\n,Missing Name".getBytes())
                        .with(user(adminPrincipal)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.errorRows").value(2));
    }

    // ---------------------------------------------------------------
    // GET /admin/skills/export
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can export skills — returns 200 with Excel content type and filename")
    void exportSkills_adminCan() throws Exception {
        byte[] fakeExcelBytes = new byte[]{1, 2, 3, 4};
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "Skill_Master_Export_20260101_120000.xlsx");
        org.springframework.http.ResponseEntity<byte[]> response =
                new org.springframework.http.ResponseEntity<>(fakeExcelBytes, headers, HttpStatus.OK);

        when(skillImportService.exportSkills(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/skills/export").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString("spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("Skill_Master_Export_20260101_120000.xlsx")));
    }

    @Test
    @DisplayName("ADMIN can export skills with filters — filters forwarded to service")
    void exportSkills_withFilters() throws Exception {
        byte[] fakeExcelBytes = new byte[]{1, 2, 3, 4};
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "Skill_Master_Export_20260101_120000.xlsx");
        org.springframework.http.ResponseEntity<byte[]> response =
                new org.springframework.http.ResponseEntity<>(fakeExcelBytes, headers, HttpStatus.OK);

        when(skillImportService.exportSkills(eq("java"), eq("TECHNICAL"), eq("GLOBAL"), any(), any(), any(), eq("ACTIVE")))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/skills/export")
                        .param("search", "java")
                        .param("categoryCode", "TECHNICAL")
                        .param("scopeCode", "GLOBAL")
                        .param("status", "ACTIVE")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 on skills export")
    void exportSkills_technicianForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/skills/export").with(user(techPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LEAD_MANAGER gets 403 on skills export")
    void exportSkills_leadForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/skills/export").with(user(leadPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated gets 401 on skills export")
    void exportSkills_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/skills/export"))
                .andExpect(status().isUnauthorized());
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
