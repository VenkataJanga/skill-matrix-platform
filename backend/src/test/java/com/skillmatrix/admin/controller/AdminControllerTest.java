package com.skillmatrix.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatrix.admin.dto.ApplicationAssignmentDto;
import com.skillmatrix.admin.dto.AssignmentRequest;
import com.skillmatrix.admin.dto.BulkStatusRequest;
import com.skillmatrix.admin.dto.BulkStatusResponse;
import com.skillmatrix.admin.dto.CreateUserRequest;
import com.skillmatrix.admin.dto.DuplicateCheckResponse;
import com.skillmatrix.admin.dto.UserDetailDto;
import com.skillmatrix.admin.dto.UserSummaryDto;
import com.skillmatrix.admin.service.AdminAssignmentService;
import com.skillmatrix.admin.service.AdminUserService;
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

import java.math.BigDecimal;
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

@WebMvcTest(value = AdminController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import({SecurityConfig.class, AppConfig.class})
@DisplayName("AdminController — MockMvc tests")
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AdminUserService adminUserService;
    @MockBean AdminAssignmentService adminAssignmentService;
    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean RefreshTokenService refreshTokenService;
    @MockBean UserRepository userRepository;

    private UserPrincipal adminPrincipal;
    private UserPrincipal techPrincipal;
    private UserPrincipal leadPrincipal;

    private UserSummaryDto sampleUser;
    private ApplicationAssignmentDto sampleAssignment;

    @BeforeEach
    void setUp() {
        adminPrincipal  = buildPrincipal("admin", "ADMIN");
        techPrincipal   = buildPrincipal("deepak_mishra", "TECHNICIAN");
        leadPrincipal   = buildPrincipal("dumitru_baboiu", "LEAD_MANAGER");

        sampleUser = new UserSummaryDto(
                "pub-admin", "admin", "System Admin",
                "admin@nttdata.com", true, List.of("ADMIN"));

        sampleAssignment = new ApplicationAssignmentDto(
                "pub-map-1", "pub-tech-1", "deepak_mishra", "Deepak Mishra",
                "pub-atlas", "ATLAS", "ATLAS-deZentral",
                "WEB", "B06", BigDecimal.valueOf(100),
                LocalDate.of(2026, 1, 1), null, true);
    }

    // ---------------------------------------------------------------
    // GET /admin/users — role access
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can get user list")
    void getUsers_adminCan() throws Exception {
        PagedResponse<UserSummaryDto> paged = new PagedResponse<>(
                List.of(sampleUser), 0, 20, 1, 1, false, false);
        when(adminUserService.getUsers(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(paged);

        mockMvc.perform(get("/api/v1/admin/users").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("admin"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 when accessing user list")
    void getUsers_technicianForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").with(user(techPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LEAD_MANAGER gets 403 when accessing user list")
    void getUsers_leadForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").with(user(leadPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request gets 401")
    void getUsers_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // POST /admin/users — create user
    // ---------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can create user — returns 201")
    void createUser_adminCanCreate() throws Exception {
        UserDetailDto detail = new UserDetailDto(
                "pub-new", "newuser", "New User", "new@nttdata.com",
                null, "TECHNICIAN", null, null,
                true, false, false, List.of("TECHNICIAN"),
                LocalDateTime.now(), LocalDateTime.now(), "admin", "admin");

        when(adminUserService.createUser(any(), eq("admin"))).thenReturn(detail);

        CreateUserRequest req = new CreateUserRequest(
                "newuser", "New User", "new@nttdata.com", "TECHNICIAN",
                null, null, false);

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    @DisplayName("POST /admin/users returns 409 for duplicate username")
    void createUser_duplicateReturns409() throws Exception {
        when(adminUserService.createUser(any(), any()))
                .thenThrow(new BusinessException("Username already exists", HttpStatus.CONFLICT));

        CreateUserRequest req = new CreateUserRequest(
                "admin", "Admin Again", "admin2@nttdata.com",
                "ADMIN", null, null, false);

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /admin/users returns 400 for missing required fields")
    void createUser_invalidBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // POST /admin/application-assignments — duplicate check
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /application-assignments returns 409 for duplicate")
    void createAssignment_duplicateReturns409() throws Exception {
        when(adminAssignmentService.createAssignment(any(), any()))
                .thenThrow(new BusinessException(
                        "This technician is already assigned to the selected application.",
                        HttpStatus.CONFLICT));

        AssignmentRequest req = new AssignmentRequest(
                "pub-tech-1", "pub-atlas",
                BigDecimal.valueOf(100), LocalDate.now(), null);

        mockMvc.perform(post("/api/v1/admin/application-assignments")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "This technician is already assigned to the selected application."));
    }

    // ---------------------------------------------------------------
    // GET /admin/application-assignments/check-duplicate
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /check-duplicate returns isDuplicate=true for existing assignment")
    void checkDuplicate_existingReturnsTrue() throws Exception {
        when(adminAssignmentService.checkDuplicate("pub-tech-1", "pub-atlas"))
                .thenReturn(DuplicateCheckResponse.duplicate("pub-map-1"));

        mockMvc.perform(get("/api/v1/admin/application-assignments/check-duplicate")
                        .with(user(adminPrincipal))
                        .param("userPublicId", "pub-tech-1")
                        .param("applicationPublicId", "pub-atlas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDuplicate").value(true))
                .andExpect(jsonPath("$.data.existingAssignmentPublicId").value("pub-map-1"));
    }

    // ---------------------------------------------------------------
    // PATCH /admin/application-assignments/bulk-status
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PATCH /bulk-status returns operation summary")
    void bulkStatus_returnsSummary() throws Exception {
        BulkStatusResponse resp = new BulkStatusResponse(3, 2, 1,
                List.of(new BulkStatusResponse.BulkFailureDetail("bad-id", "Not found")));

        when(adminAssignmentService.bulkUpdateStatus(any(), any())).thenReturn(resp);

        BulkStatusRequest req = new BulkStatusRequest(
                List.of("id1", "id2", "bad-id"), false, "Bulk remove");

        mockMvc.perform(patch("/api/v1/admin/application-assignments/bulk-status")
                        .with(user(adminPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedCount").value(3))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.failures[0].publicId").value("bad-id"));
    }

    @Test
    @DisplayName("LEAD_MANAGER cannot access assignment management")
    void assignments_leadForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/application-assignments")
                        .with(user(leadPrincipal)))
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

        User user = User.builder()
                .id(1L).publicId("pub-" + username)
                .username(username).email(username + "@nttdata.com")
                .fullName(username).passwordHash("$hash")
                .active(true).accountLocked(false).mustChangePassword(false)
                .userRoles(new HashSet<>(Set.of(userRole))).build();

        userRole.setUser(user);
        return new UserPrincipal(user);
    }
}
