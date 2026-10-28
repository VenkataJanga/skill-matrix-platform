package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.BulkDeleteRequest;
import com.skillmatrix.admin.dto.BulkDeleteResponse;
import com.skillmatrix.admin.dto.DeleteUserResult;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.config.UserManagementProperties;
import com.skillmatrix.domain.entity.Role;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserRole;
import com.skillmatrix.domain.repository.ImportExportHistoryRepository;
import com.skillmatrix.domain.repository.RefreshTokenRepository;
import com.skillmatrix.domain.repository.UserApplicationMappingRepository;
import com.skillmatrix.domain.repository.UserRepository;
import com.skillmatrix.domain.repository.UserTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserDeleteService — unit tests")
class AdminUserDeleteServiceTest {

    @Mock UserRepository                   userRepository;
    @Mock UserTeamRepository               userTeamRepository;
    @Mock RefreshTokenRepository           refreshTokenRepository;
    @Mock UserApplicationMappingRepository userApplicationMappingRepository;
    @Mock ImportExportHistoryRepository    importExportHistoryRepository;
    @Mock AuditService                     auditService;
    @Mock UserManagementProperties         userManagementProperties;
    @Mock JdbcTemplate                     jdbcTemplate;

    @InjectMocks AdminUserDeleteService service;

    private static final String ACTOR_USERNAME  = "admin";
    private static final String ACTOR_PUBLIC_ID = "actor-pub-1";
    private static final String TARGET_PUBLIC_ID = "target-pub-1";

    private User adminUser;
    private User techUser;
    private Role adminRole;
    private Role techRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder().id(1L).publicId("r-admin").roleCode("ADMIN")
                .roleName("Admin").active(true).rolePermissions(new HashSet<>()).build();
        techRole = Role.builder().id(2L).publicId("r-tech").roleCode("TECHNICIAN")
                .roleName("Technician").active(true).rolePermissions(new HashSet<>()).build();

        UserRole adminUserRole = UserRole.builder().id(1L).role(adminRole).active(true).build();
        UserRole techUserRole  = UserRole.builder().id(2L).role(techRole).active(true).build();

        adminUser = User.builder().id(1L).publicId(ACTOR_PUBLIC_ID).username(ACTOR_USERNAME)
                .fullName("Admin User").email("admin@x.com").passwordHash("$h")
                .active(true).mustChangePassword(false).primaryRole(adminRole)
                .userRoles(new HashSet<>(Set.of(adminUserRole))).build();
        adminUserRole.setUser(adminUser);

        techUser = User.builder().id(2L).publicId(TARGET_PUBLIC_ID).username("techuser")
                .fullName("Tech User").email("tech@x.com").passwordHash("$h")
                .active(true).mustChangePassword(false).primaryRole(techRole)
                .userRoles(new HashSet<>(Set.of(techUserRole))).build();
        techUserRole.setUser(techUser);
    }

    // ================================================================
    // 1. Hard delete disabled → BusinessException HTTP 423
    // ================================================================
    @Test
    @DisplayName("Hard delete disabled → throws BusinessException (LOCKED)")
    void hardDeleteDisabled_throwsLocked() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.deleteUser(TARGET_PUBLIC_ID, ACTOR_USERNAME, ACTOR_PUBLIC_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Hard delete is disabled");
    }

    // ================================================================
    // 2. ADMIN can delete a user with no dependencies
    // ================================================================
    @Test
    @DisplayName("ADMIN can delete a user with no protected dependencies")
    void adminCanDeleteUserNoDependencies() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(true);
        when(userRepository.findByPublicId(TARGET_PUBLIC_ID)).thenReturn(Optional.of(techUser));
        when(userApplicationMappingRepository.existsByUser_Id(techUser.getId())).thenReturn(false);
        when(importExportHistoryRepository.existsByInitiatedById(techUser.getId())).thenReturn(false);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(0);

        DeleteUserResult result = service.deleteUser(TARGET_PUBLIC_ID, ACTOR_USERNAME, ACTOR_PUBLIC_ID, "test cleanup");

        assertThat(result.status()).isEqualTo("DELETED");
        assertThat(result.userPublicId()).isEqualTo(TARGET_PUBLIC_ID);
        verify(userRepository).delete(techUser);
        verify(auditService).log(eq(AuditService.USER_DELETE), anyString(), isNull(), eq(ACTOR_USERNAME),
                eq(TARGET_PUBLIC_ID), isNull(), isNull(), anyString());
    }

    // ================================================================
    // 3. Actor cannot delete themselves
    // ================================================================
    @Test
    @DisplayName("Actor cannot delete their own account")
    void cannotDeleteSelf() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(true);
        when(userRepository.findByPublicId(ACTOR_PUBLIC_ID)).thenReturn(Optional.of(adminUser));

        DeleteUserResult result = service.deleteUser(ACTOR_PUBLIC_ID, ACTOR_USERNAME, ACTOR_PUBLIC_ID, null);

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.reason()).contains("own account");
        verify(userRepository, never()).delete(any());
    }

    // ================================================================
    // 4. Last active ADMIN cannot be deleted
    // ================================================================
    @Test
    @DisplayName("Last active ADMIN cannot be deleted")
    void lastAdminCannotBeDeleted() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(true);
        // Make target a second admin (different publicId from actor)
        User secondAdmin = User.builder().id(3L).publicId("second-admin").username("admin2")
                .fullName("Admin 2").email("admin2@x.com").passwordHash("$h")
                .active(true).mustChangePassword(false).primaryRole(adminRole)
                .userRoles(new HashSet<>(Set.of(
                        UserRole.builder().id(10L).role(adminRole).active(true).build())))
                .build();
        when(userRepository.findByPublicId("second-admin")).thenReturn(Optional.of(secondAdmin));
        when(userRepository.countByActiveAndPrimaryRole_RoleCode(true, "ADMIN")).thenReturn(1L);

        DeleteUserResult result = service.deleteUser("second-admin", ACTOR_USERNAME, ACTOR_PUBLIC_ID, null);

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.reason()).contains("last active ADMIN");
        verify(userRepository, never()).delete(any());
    }

    // ================================================================
    // 5. User with application assignment → BLOCKED
    // ================================================================
    @Test
    @DisplayName("User with application assignment is BLOCKED")
    void userWithAssignment_isBlocked() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(true);
        when(userRepository.findByPublicId(TARGET_PUBLIC_ID)).thenReturn(Optional.of(techUser));
        when(userApplicationMappingRepository.existsByUser_Id(techUser.getId())).thenReturn(true);

        DeleteUserResult result = service.deleteUser(TARGET_PUBLIC_ID, ACTOR_USERNAME, ACTOR_PUBLIC_ID, null);

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.reason()).contains("application assignment");
        verify(userRepository, never()).delete(any());
    }

    // ================================================================
    // 6. User roles and refresh tokens are cleaned up before delete
    // ================================================================
    @Test
    @DisplayName("Refresh tokens and team memberships are removed before user delete")
    void preDeleteCleanup_removesSessionAndTeamData() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(true);
        when(userRepository.findByPublicId(TARGET_PUBLIC_ID)).thenReturn(Optional.of(techUser));
        when(userApplicationMappingRepository.existsByUser_Id(techUser.getId())).thenReturn(false);
        when(importExportHistoryRepository.existsByInitiatedById(techUser.getId())).thenReturn(false);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(0);

        service.deleteUser(TARGET_PUBLIC_ID, ACTOR_USERNAME, ACTOR_PUBLIC_ID, null);

        verify(refreshTokenRepository).deleteAllByUser(techUser);
        verify(userTeamRepository).deleteAllByUser_Id(techUser.getId());
        verify(userRepository).nullifyManagerFor(techUser.getId());
        verify(userRepository).delete(techUser);
    }

    // ================================================================
    // 7. Unknown publicId → NOT_FOUND exception
    // ================================================================
    @Test
    @DisplayName("Unknown publicId → BusinessException NOT_FOUND")
    void unknownPublicId_throwsNotFound() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(true);
        when(userRepository.findByPublicId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUser("unknown", ACTOR_USERNAME, ACTOR_PUBLIC_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
    }

    // ================================================================
    // 8. Bulk delete: 2 eligible, 1 blocked → 2 deleted, 1 blocked
    // ================================================================
    @Test
    @DisplayName("Bulk delete: eligible users deleted, blocked users retained")
    void bulkDelete_eligibleDeletedBlockedRetained() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(true);

        User user1 = buildTechUser(10L, "pub-10", "tech10");
        User user2 = buildTechUser(11L, "pub-11", "tech11");
        User user3 = buildTechUser(12L, "pub-12", "tech12"); // has assignment

        when(userRepository.findByPublicId("pub-10")).thenReturn(Optional.of(user1));
        when(userRepository.findByPublicId("pub-11")).thenReturn(Optional.of(user2));
        when(userRepository.findByPublicId("pub-12")).thenReturn(Optional.of(user3));

        when(userApplicationMappingRepository.existsByUser_Id(10L)).thenReturn(false);
        when(userApplicationMappingRepository.existsByUser_Id(11L)).thenReturn(false);
        when(userApplicationMappingRepository.existsByUser_Id(12L)).thenReturn(true);

        when(importExportHistoryRepository.existsByInitiatedById(10L)).thenReturn(false);
        when(importExportHistoryRepository.existsByInitiatedById(11L)).thenReturn(false);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(11L))).thenReturn(0);

        BulkDeleteRequest request = new BulkDeleteRequest(List.of("pub-10", "pub-11", "pub-12"), "test cleanup");
        BulkDeleteResponse response = service.bulkDeleteUsers(request, ACTOR_USERNAME, ACTOR_PUBLIC_ID);

        assertThat(response.requestedCount()).isEqualTo(3);
        assertThat(response.deletedCount()).isEqualTo(2);
        assertThat(response.blockedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(0);
        assertThat(response.results()).hasSize(1); // only non-DELETED in results
        assertThat(response.results().get(0).status()).isEqualTo("BLOCKED");
        verify(userRepository, times(2)).delete(any(User.class));
    }

    // ================================================================
    // 9. Audit record is written on single delete
    // ================================================================
    @Test
    @DisplayName("Audit record is written for user delete")
    void auditRecordWrittenOnDelete() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(true);
        when(userRepository.findByPublicId(TARGET_PUBLIC_ID)).thenReturn(Optional.of(techUser));
        when(userApplicationMappingRepository.existsByUser_Id(techUser.getId())).thenReturn(false);
        when(importExportHistoryRepository.existsByInitiatedById(techUser.getId())).thenReturn(false);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(0);

        service.deleteUser(TARGET_PUBLIC_ID, ACTOR_USERNAME, ACTOR_PUBLIC_ID, "cleanup");

        verify(auditService).log(
                eq(AuditService.USER_DELETE),
                eq(AuditService.ENTITY_USER),
                isNull(),
                eq(ACTOR_USERNAME),
                eq(TARGET_PUBLIC_ID),
                isNull(), isNull(),
                anyString());
    }

    // ================================================================
    // 10. Audit record is written on bulk delete
    // ================================================================
    @Test
    @DisplayName("Audit record is written for bulk delete operation")
    void auditRecordWrittenOnBulkDelete() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(true);
        User user1 = buildTechUser(20L, "pub-20", "tech20");
        when(userRepository.findByPublicId("pub-20")).thenReturn(Optional.of(user1));
        when(userApplicationMappingRepository.existsByUser_Id(20L)).thenReturn(false);
        when(importExportHistoryRepository.existsByInitiatedById(20L)).thenReturn(false);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(20L))).thenReturn(0);

        service.bulkDeleteUsers(new BulkDeleteRequest(List.of("pub-20"), "bulk test"), ACTOR_USERNAME,
                ACTOR_PUBLIC_ID);

        verify(auditService).log(
                eq(AuditService.USER_BULK_DELETE),
                eq(AuditService.ENTITY_USER),
                isNull(),
                eq(ACTOR_USERNAME),
                isNull(), isNull(), isNull(),
                anyString());
    }

    // ================================================================
    // 11. Import history blocks delete
    // ================================================================
    @Test
    @DisplayName("User with import/export history is BLOCKED")
    void userWithImportHistory_isBlocked() {
        when(userManagementProperties.isHardDeleteEnabled()).thenReturn(true);
        when(userRepository.findByPublicId(TARGET_PUBLIC_ID)).thenReturn(Optional.of(techUser));
        when(userApplicationMappingRepository.existsByUser_Id(techUser.getId())).thenReturn(false);
        when(importExportHistoryRepository.existsByInitiatedById(techUser.getId())).thenReturn(true);

        DeleteUserResult result = service.deleteUser(TARGET_PUBLIC_ID, ACTOR_USERNAME, ACTOR_PUBLIC_ID, null);

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.reason()).contains("import/export history");
        verify(userRepository, never()).delete(any());
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private User buildTechUser(Long id, String publicId, String username) {
        UserRole ur = UserRole.builder().id(id * 10).role(techRole).active(true).build();
        User u = User.builder().id(id).publicId(publicId).username(username)
                .fullName(username).email(username + "@x.com").passwordHash("$h")
                .active(true).mustChangePassword(false).primaryRole(techRole)
                .userRoles(new HashSet<>(Set.of(ur))).build();
        ur.setUser(u);
        return u;
    }
}
