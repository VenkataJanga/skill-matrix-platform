package com.skillmatrix.assessments.controller;

import com.skillmatrix.assessments.dto.SaveDraftRequest;
import com.skillmatrix.assessments.dto.TechnicianAssessmentDto;
import com.skillmatrix.assessments.dto.TechnicianAssessmentItemDto;
import com.skillmatrix.assessments.service.TechnicianAssessmentService;
import com.skillmatrix.auth.service.AuthService;
import com.skillmatrix.auth.service.JwtService;
import com.skillmatrix.auth.service.RefreshTokenService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TechnicianAssessmentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import({SecurityConfig.class, AppConfig.class})
@DisplayName("TechnicianAssessmentController — MockMvc tests")
class TechnicianAssessmentControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TechnicianAssessmentService technicianAssessmentService;
    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean RefreshTokenService refreshTokenService;
    @MockBean UserRepository userRepository;

    private UserPrincipal techPrincipal;
    private UserPrincipal adminPrincipal;
    private TechnicianAssessmentDto sampleAssessment;

    @BeforeEach
    void setUp() {
        techPrincipal = buildPrincipal("tech_john", "TECHNICIAN");
        adminPrincipal = buildPrincipal("admin", "ADMIN");

        TechnicianAssessmentItemDto item = new TechnicianAssessmentItemDto(
                "pub-item-1", "pub-skill-java", "TECH_JAVA", "Java",
                "TECHNICAL", "Technical", "COMMON_TECH", true, null, null, null);

        sampleAssessment = new TechnicianAssessmentDto(
                "pub-header-1", "ATLAS FY26", "ATLAS_DEZENTRAL", "ATLAS-deZentral",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                "DRAFT", "Draft", null, List.of(item), 0L);
    }

    @Test
    @DisplayName("TECHNICIAN can list own assessments")
    void getMyAssessments_technicianCan() throws Exception {
        when(technicianAssessmentService.getMyAssessments("tech_john")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/technician/assessments").with(user(techPrincipal)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN gets 403 on technician assessments endpoint (technician-only)")
    void getMyAssessments_adminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/technician/assessments").with(user(adminPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated gets 401")
    void getMyAssessments_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/technician/assessments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Technician-facing assessment detail response does NOT contain expectedLevel/expectedRating fields")
    void getAssessment_responseExcludesExpectedLevel() throws Exception {
        when(technicianAssessmentService.getAssessment("pub-header-1", "tech_john")).thenReturn(sampleAssessment);

        mockMvc.perform(get("/api/v1/technician/assessments/pub-header-1").with(user(techPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].skillCode").value("TECH_JAVA"))
                .andExpect(jsonPath("$.data.items[0].expectedLevel").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].expectedRating").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].targetLevel").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].requiredLevel").doesNotExist());
    }

    @Test
    @DisplayName("Start assessment returns the assessment for an OPEN cycle")
    void startAssessment_success() throws Exception {
        when(technicianAssessmentService.startAssessment("pub-cycle-1", "tech_john")).thenReturn(sampleAssessment);

        mockMvc.perform(post("/api/v1/technician/assessment-cycles/pub-cycle-1/start")
                        .with(user(techPrincipal)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cycleName").value("ATLAS FY26"));
    }

    @Test
    @DisplayName("Start assessment returns 403 when technician not assigned")
    void startAssessment_forbidden() throws Exception {
        when(technicianAssessmentService.startAssessment(any(), any()))
                .thenThrow(new BusinessException("You are not assigned to this application.", HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/api/v1/technician/assessment-cycles/pub-cycle-1/start")
                        .with(user(techPrincipal)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Submit returns 409 when assessment already submitted")
    void submit_conflict() throws Exception {
        when(technicianAssessmentService.submit(any(), any()))
                .thenThrow(new BusinessException("This assessment has already been submitted.", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/technician/assessments/pub-header-1/submit")
                        .with(user(techPrincipal)).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Save draft returns 404 when accessing another technician's assessment (IDOR)")
    void saveDraft_idorBlocked() throws Exception {
        when(technicianAssessmentService.saveDraft(any(), any(), any()))
                .thenThrow(new BusinessException("Assessment not found", HttpStatus.NOT_FOUND));

        SaveDraftRequest req = new SaveDraftRequest(0L, List.of());

        mockMvc.perform(patch("/api/v1/technician/assessments/pub-other-header/draft")
                        .with(user(techPrincipal)).with(csrf())
                        .contentType("application/json")
                        .content("{\"version\":0,\"items\":[]}"))
                .andExpect(status().isNotFound());
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
