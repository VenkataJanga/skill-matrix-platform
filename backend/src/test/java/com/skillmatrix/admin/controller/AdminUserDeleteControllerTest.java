package com.skillmatrix.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmatrix.admin.dto.BulkDeleteRequest;
import com.skillmatrix.admin.dto.BulkDeleteResponse;
import com.skillmatrix.admin.dto.DeleteUserResult;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
@DisplayName("AdminController — User Delete endpoints")
class AdminUserDeleteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AdminUserService         adminUserService;
    @MockBean AdminAssignmentService   adminAssignmentService;
    @MockBean AdminUserImportService   adminUserImportService;
    @MockBean AdminUserDeleteService   adminUserDeleteService;
    @MockBean AssignmentImportService  assignmentImportService;
    @MockBean PasswordResetService     passwordResetService;
    @MockBean AuthService              authService;
    @MockBean JwtService               jwtService;
    @MockBean RefreshTokenService      refreshTokenService;
    @MockBean UserRepository           userRepository;

    private UserPrincipal adminPrincipal;
    private UserPrincipal techPrincipal;
    private UserPrincipal leadPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal = buildPrincipal("admin",          "ADMIN");
        techPrincipal  = buildPrincipal("deepak_mishra",  "TECHNICIAN");
        leadPrincipal  = buildPrincipal("dumitru_baboiu", "LEAD_MANAGER");
    }

    // ----------------------------------------------------------------
    // DELETE /admin/users/{publicId}
    // ----------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can delete a user — returns DELETED result")
    void deleteUser_adminCan() throws Exception {
        DeleteUserResult result = DeleteUserResult.deleted("pub-1", "testuser");
        when(adminUserDeleteService.deleteUser(eq("pub-1"), anyString(), anyString(), any()))
                .thenReturn(result);

        mockMvc.perform(delete("/api/v1/admin/users/pub-1")
                        .with(user(adminPrincipal)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"))
                .andExpect(jsonPath("$.data.userPublicId").value("pub-1"));
    }

    @Test
    @DisplayName("ADMIN gets BLOCKED result when user has dependencies")
    void deleteUser_adminGetsBlocked() throws Exception {
        DeleteUserResult result = DeleteUserResult.blocked("pub-2", "depuser",
                "User has application assignment(s). Deactivate the user instead.");
        when(adminUserDeleteService.deleteUser(eq("pub-2"), anyString(), anyString(), any()))
                .thenReturn(result);

        mockMvc.perform(delete("/api/v1/admin/users/pub-2")
                        .with(user(adminPrincipal)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"))
                .andExpect(jsonPath("$.data.reason").value(
                        "User has application assignment(s). Deactivate the user instead."));
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 on delete user")
    void deleteUser_technicianForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/pub-1")
                        .with(user(techPrincipal)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LEAD_MANAGER gets 403 on delete user")
    void deleteUser_leadForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/pub-1")
                        .with(user(leadPrincipal)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated gets 401 on delete user")
    void deleteUser_unauthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/pub-1"))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    // POST /admin/users/bulk-delete
    // ----------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can bulk delete users")
    void bulkDelete_adminCan() throws Exception {
        BulkDeleteResponse response = new BulkDeleteResponse(3, 2, 1, 0,
                List.of(DeleteUserResult.blocked("pub-3", "blockeduser",
                        "User has application assignment(s). Deactivate the user instead.")));
        when(adminUserDeleteService.bulkDeleteUsers(any(BulkDeleteRequest.class), anyString(), anyString()))
                .thenReturn(response);

        BulkDeleteRequest request = new BulkDeleteRequest(
                List.of("pub-1", "pub-2", "pub-3"), "cleanup test users");

        mockMvc.perform(post("/api/v1/admin/users/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(adminPrincipal)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedCount").value(3))
                .andExpect(jsonPath("$.data.deletedCount").value(2))
                .andExpect(jsonPath("$.data.blockedCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.results[0].status").value("BLOCKED"));
    }

    @Test
    @DisplayName("TECHNICIAN gets 403 on bulk delete")
    void bulkDelete_technicianForbidden() throws Exception {
        BulkDeleteRequest request = new BulkDeleteRequest(List.of("pub-1"), null);
        mockMvc.perform(post("/api/v1/admin/users/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(techPrincipal)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LEAD_MANAGER gets 403 on bulk delete")
    void bulkDelete_leadForbidden() throws Exception {
        BulkDeleteRequest request = new BulkDeleteRequest(List.of("pub-1"), null);
        mockMvc.perform(post("/api/v1/admin/users/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(leadPrincipal)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated gets 401 on bulk delete")
    void bulkDelete_unauthenticated() throws Exception {
        BulkDeleteRequest request = new BulkDeleteRequest(List.of("pub-1"), null);
        mockMvc.perform(post("/api/v1/admin/users/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
