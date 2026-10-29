package com.skillmatrix.assessments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatrix.assessments.dto.AssessmentCycleDto;
import com.skillmatrix.assessments.dto.CreateAssessmentCycleRequest;
import com.skillmatrix.assessments.service.AssessmentCycleService;
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

import java.time.LocalDate;
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

@WebMvcTest(value = AssessmentCycleController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import({SecurityConfig.class, AppConfig.class})
@DisplayName("AssessmentCycleController — MockMvc tests")
class AssessmentCycleControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AssessmentCycleService assessmentCycleService;
    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean RefreshTokenService refreshTokenService;
    @MockBean UserRepository userRepository;

    private UserPrincipal adminPrincipal;
    private UserPrincipal techPrincipal;
    private UserPrincipal leadPrincipal;
    private AssessmentCycleDto sampleCycle;

    @BeforeEach
    void setUp() {
        adminPrincipal = buildPrincipal("admin", "ADMIN");
        techPrincipal = buildPrincipal("tech1", "TECHNICIAN");
        leadPrincipal = buildPrincipal("lead1", "LEAD_MANAGER");

        sampleCycle = new AssessmentCycleDto(
                "pub-cycle-1", "pub-app-atlas", "ATLAS_DEZENTRAL", "ATLAS-deZentral",
                "pub-rv-1", "v1.0", "ATLAS FY26", null, null, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                "PLANNED", "Planned", true,
                LocalDateTime.now(), LocalDateTime.now(), "admin", "admin");
    }

    @Test
    @DisplayName("ADMIN can list assessment cycles")
    void getCycles_adminCan() throws Exception {
        PagedResponse<AssessmentCycleDto> paged = new PagedResponse<>(
                List.of(sampleCycle), 0, 20, 1, 1, false, false);
        when(assessmentCycleService.getCycles(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(paged);

        mockMvc.perform(get("/api/v1/admin/assessment-cycles").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].cycleName").value("ATLAS FY26"));
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 on assessment cycles list")
    void getCycles_technicianForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/assessment-cycles").with(user(techPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LEAD_MANAGER gets 403 on assessment cycles list (admin-only)")
    void getCycles_leadForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/assessment-cycles").with(user(leadPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated gets 401 on assessment cycles list")
    void getCycles_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/assessment-cycles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN can create cycle — returns 201")
    void createCycle_adminCan() throws Exception {
        when(assessmentCycleService.createCycle(any(), eq("admin"))).thenReturn(sampleCycle);

        CreateAssessmentCycleRequest req = new CreateAssessmentCycleRequest(
                "pub-app-atlas", "pub-rv-1", "ATLAS FY26", null, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        mockMvc.perform(post("/api/v1/admin/assessment-cycles")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cycleName").value("ATLAS FY26"));
    }

    @Test
    @DisplayName("POST /admin/assessment-cycles returns 400 for missing required fields")
    void createCycle_missingFields() throws Exception {
        mockMvc.perform(post("/api/v1/admin/assessment-cycles")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH open returns 409 when cycle is not PLANNED")
    void openCycle_conflict() throws Exception {
        when(assessmentCycleService.openCycle(any(), any()))
                .thenThrow(new BusinessException("Only PLANNED cycles can be opened", HttpStatus.CONFLICT));

        mockMvc.perform(patch("/api/v1/admin/assessment-cycles/pub-cycle-1/open")
                        .with(user(adminPrincipal)).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH close succeeds for ADMIN")
    void closeCycle_adminCan() throws Exception {
        when(assessmentCycleService.closeCycle(any(), any())).thenReturn(sampleCycle);

        mockMvc.perform(patch("/api/v1/admin/assessment-cycles/pub-cycle-1/close")
                        .with(user(adminPrincipal)).with(csrf()))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------
    // Reopen (CLOSED -> OPEN, M4 stabilization)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PATCH reopen succeeds for ADMIN with a valid reason")
    void reopenCycle_adminCan() throws Exception {
        AssessmentCycleDto reopened = new AssessmentCycleDto(
                "pub-cycle-1", "pub-app-atlas", "ATLAS_DEZENTRAL", "ATLAS-deZentral",
                "pub-rv-1", "v1.0", "ATLAS Q2 2026 Assessment", null, null, null,
                LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 26),
                "OPEN", "Open", true,
                LocalDateTime.now(), LocalDateTime.now(), "admin", "admin");
        when(assessmentCycleService.reopenCycle(any(), any(), eq("admin"))).thenReturn(reopened);

        mockMvc.perform(patch("/api/v1/admin/assessment-cycles/pub-cycle-1/reopen")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Closed accidentally during validation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusCode").value("OPEN"));
    }

    @Test
    @DisplayName("PATCH reopen returns 400 when reason is blank")
    void reopenCycle_blankReasonRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/assessment-cycles/pub-cycle-1/reopen")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH reopen returns 409 for an invalid lifecycle transition (e.g. not CLOSED)")
    void reopenCycle_invalidTransitionConflict() throws Exception {
        when(assessmentCycleService.reopenCycle(any(), any(), any()))
                .thenThrow(new BusinessException("Only CLOSED cycles can be reopened", HttpStatus.CONFLICT));

        mockMvc.perform(patch("/api/v1/admin/assessment-cycles/pub-cycle-1/reopen")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"reason\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TECHNICIAN cannot call reopen API (403)")
    void reopenCycle_technicianForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/assessment-cycles/pub-cycle-1/reopen")
                        .with(user(techPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"reason\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LEAD_MANAGER cannot call reopen API (403, ADMIN-only)")
    void reopenCycle_leadForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/assessment-cycles/pub-cycle-1/reopen")
                        .with(user(leadPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"reason\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated gets 401 on reopen API")
    void reopenCycle_unauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/assessment-cycles/pub-cycle-1/reopen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"reason\"}"))
                .andExpect(status().isUnauthorized());
    }

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
