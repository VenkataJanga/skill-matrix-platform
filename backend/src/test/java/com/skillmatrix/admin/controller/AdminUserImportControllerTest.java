package com.skillmatrix.admin.controller;

import com.skillmatrix.admin.dto.UserImportResult;
import com.skillmatrix.admin.dto.UserImportRowPreview;
import com.skillmatrix.admin.dto.UserValidationResult;
import com.skillmatrix.admin.service.AdminAssignmentService;
import com.skillmatrix.admin.service.AdminUserDeleteService;
import com.skillmatrix.admin.service.AdminUserImportService;
import com.skillmatrix.admin.service.AdminUserService;
import com.skillmatrix.admin.service.AssignmentImportService;
import com.skillmatrix.auth.service.PasswordResetService;
import com.skillmatrix.auth.service.AuthService;
import com.skillmatrix.auth.service.JwtService;
import com.skillmatrix.auth.service.RefreshTokenService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
@DisplayName("AdminController — User Import/Export endpoints")
class AdminUserImportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdminUserService adminUserService;
    @MockBean AdminAssignmentService adminAssignmentService;
    @MockBean AdminUserImportService adminUserImportService;
    @MockBean AdminUserDeleteService adminUserDeleteService;
    @MockBean AssignmentImportService assignmentImportService;
    @MockBean PasswordResetService passwordResetService;
    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean RefreshTokenService refreshTokenService;
    @MockBean UserRepository userRepository;

    private UserPrincipal adminPrincipal;
    private UserPrincipal techPrincipal;
    private UserPrincipal leadPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal = buildPrincipal("admin", "ADMIN");
        techPrincipal  = buildPrincipal("deepak_mishra", "TECHNICIAN");
        leadPrincipal  = buildPrincipal("dumitru_baboiu", "LEAD_MANAGER");
    }

    // ----------------------------------------------------------------
    // GET /admin/users/template
    // ----------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can download user import template")
    void downloadTemplate_adminCan() throws Exception {
        when(adminUserImportService.generateTemplate()).thenReturn(
                ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user_import_template.xlsx")
                        .contentType(MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(new byte[]{1, 2, 3}));

        mockMvc.perform(get("/api/v1/admin/users/template").with(user(adminPrincipal)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 on download template")
    void downloadTemplate_technicianForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/template").with(user(techPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LEAD_MANAGER gets 403 on download template")
    void downloadTemplate_leadForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/template").with(user(leadPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated gets 401 on download template")
    void downloadTemplate_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/template"))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    // GET /admin/users/export
    // ----------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can export users")
    void exportUsers_adminCan() throws Exception {
        when(adminUserImportService.exportUsers(any(), any(), any())).thenReturn(
                ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users_export.xlsx")
                        .contentType(MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(new byte[]{10, 20, 30}));

        mockMvc.perform(get("/api/v1/admin/users/export").with(user(adminPrincipal)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 on export users")
    void exportUsers_technicianForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/export").with(user(techPrincipal)))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------------
    // POST /admin/users/import/validate — use byte[] pattern from SkillControllerTest
    // ----------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can validate user import file")
    void validateImport_adminCan() throws Exception {
        UserValidationResult result = new UserValidationResult(
                true, 2, 2, 0, 0, 0,
                List.of(
                        UserImportRowPreview.of(2, "u1", "User1", "u1@x.com", "TECHNICIAN", "INSERT", "New"),
                        UserImportRowPreview.of(3, "u2", "User2", "u2@x.com", "ADMIN", "INSERT", "New")));
        when(adminUserImportService.validateOnly(any())).thenReturn(result);

        mockMvc.perform(multipart("/api/v1/admin/users/import/validate")
                        .file("file", "username,full_name,email,role_code".getBytes())
                        .with(user(adminPrincipal)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.insertRows").value(2))
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 on validate import")
    void validateImport_technicianForbidden() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/users/import/validate")
                        .file("file", "username".getBytes())
                        .with(user(techPrincipal)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------------
    // POST /admin/users/import/confirm
    // ----------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can confirm user import")
    void confirmImport_adminCan() throws Exception {
        UserImportResult result = new UserImportResult(
                true, 2, 2, 0, 0, 0, "hist-pub-1",
                "Import successful: 2 inserted, 0 updated, 0 skipped.", "STRICT", false);
        when(adminUserImportService.confirmImport(any(), anyString(), anyString())).thenReturn(result);

        mockMvc.perform(multipart("/api/v1/admin/users/import/confirm")
                        .file("file", "username,full_name,email,role_code".getBytes())
                        .with(user(adminPrincipal)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.insertedRows").value(2))
                .andExpect(jsonPath("$.data.importHistoryPublicId").value("hist-pub-1"));
    }

    @Test
    @DisplayName("LEAD_MANAGER gets 403 on confirm import")
    void confirmImport_leadForbidden() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/users/import/confirm")
                        .file("file", "username".getBytes())
                        .with(user(leadPrincipal)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------------
    // GET /admin/users/import/history
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Unauthenticated gets 401 on import history")
    void importHistory_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/import/history"))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

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
