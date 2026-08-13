package com.skillmatrix.admin.service;

import com.skillmatrix.admin.dto.CreateUserRequest;
import com.skillmatrix.admin.dto.UpdateUserRequest;
import com.skillmatrix.admin.dto.UserDetailDto;
import com.skillmatrix.admin.dto.UserStatusRequest;
import com.skillmatrix.admin.dto.UserSummaryDto;
import com.skillmatrix.audit.service.AuditService;
import com.skillmatrix.common.dto.PagedResponse;
import com.skillmatrix.common.exception.BusinessException;
import com.skillmatrix.domain.entity.Role;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.RoleRepository;
import com.skillmatrix.domain.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserService unit tests")
class AdminUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;

    @InjectMocks AdminUserService service;

    private User adminUser;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder()
                .id(1L).publicId(UUID.randomUUID().toString())
                .roleCode("ADMIN").roleName("Administrator").active(true).build();

        adminUser = User.builder()
                .id(1L).publicId("pub-admin")
                .username("admin").email("admin@nttdata.com")
                .fullName("System Admin").passwordHash("$hash")
                .primaryRole(adminRole).active(true).userRoles(new HashSet<>())
                .build();
    }

    // ---------------------------------------------------------------
    // Search / list
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getUsers returns paged results")
    void getUsers_returnsPaged() {
        Page<User> page = new PageImpl<>(List.of(adminUser));
        when(userRepository.searchUsers(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        PagedResponse<UserSummaryDto> result = service.getUsers(0, 10, null, null, null, "username", "asc");

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getUsers with search keyword passes it to repository")
    void getUsers_withSearch() {
        Page<User> page = new PageImpl<>(List.of(adminUser));
        when(userRepository.searchUsers(eq("admin"), any(), any(), any(Pageable.class))).thenReturn(page);

        service.getUsers(0, 10, "admin", null, "ACTIVE", "username", "asc");

        verify(userRepository).searchUsers(eq("admin"), isNull(), eq(Boolean.TRUE), any(Pageable.class));
    }

    @Test
    @DisplayName("getUserByPublicId returns detail DTO")
    void getUserByPublicId_found() {
        when(userRepository.findByPublicId("pub-admin")).thenReturn(Optional.of(adminUser));

        UserDetailDto dto = service.getUserByPublicId("pub-admin");

        assertThat(dto.username()).isEqualTo("admin");
        assertThat(dto.active()).isTrue();
    }

    @Test
    @DisplayName("getUserByPublicId throws 404 when not found")
    void getUserByPublicId_notFound() {
        when(userRepository.findByPublicId("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserByPublicId("bad-id"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createUser succeeds with valid request")
    void createUser_success() {
        CreateUserRequest req = new CreateUserRequest(
                "newuser", "New User", "new@nttdata.com",
                "ADMIN", null, null, false);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@nttdata.com")).thenReturn(false);
        when(roleRepository.findByRoleCode("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(any())).thenReturn("$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            u.setPublicId(UUID.randomUUID().toString());
            return u;
        });

        UserDetailDto dto = service.createUser(req, "admin");

        assertThat(dto.username()).isEqualTo("newuser");
        verify(auditService).log(eq("CREATE_USER"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createUser rejects duplicate username with 409")
    void createUser_duplicateUsername() {
        CreateUserRequest req = new CreateUserRequest(
                "admin", "Another Admin", "other@nttdata.com",
                "ADMIN", null, null, false);

        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(req, "superadmin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("createUser rejects duplicate email with 409")
    void createUser_duplicateEmail() {
        CreateUserRequest req = new CreateUserRequest(
                "newuser2", "New User 2", "admin@nttdata.com",
                "ADMIN", null, null, false);

        when(userRepository.existsByUsername("newuser2")).thenReturn(false);
        when(userRepository.existsByEmail("admin@nttdata.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(req, "superadmin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateUser changes fullName and email")
    void updateUser_success() {
        UpdateUserRequest req = new UpdateUserRequest(
                "Updated Name", "updated@nttdata.com",
                "ADMIN", "EMP001", null, false);

        when(userRepository.findByPublicId("pub-admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.existsByEmail("updated@nttdata.com")).thenReturn(false);
        when(roleRepository.findByRoleCode("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDetailDto dto = service.updateUser("pub-admin", req, "admin");

        assertThat(dto.fullName()).isEqualTo("Updated Name");
        assertThat(dto.email()).isEqualTo("updated@nttdata.com");
    }

    @Test
    @DisplayName("updateUser rejects email already used by another user")
    void updateUser_duplicateEmail() {
        UpdateUserRequest req = new UpdateUserRequest(
                "Admin", "other@nttdata.com", "ADMIN", null, null, false);

        when(userRepository.findByPublicId("pub-admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.existsByEmail("other@nttdata.com")).thenReturn(true);

        assertThatThrownBy(() -> service.updateUser("pub-admin", req, "admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---------------------------------------------------------------
    // Status
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateUserStatus deactivates a user")
    void updateUserStatus_deactivate() {
        when(userRepository.findByPublicId("pub-admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDetailDto dto = service.updateUserStatus("pub-admin",
                new UserStatusRequest(false, "Leaving project"), "admin");

        assertThat(dto.active()).isFalse();
        verify(auditService).log(eq("DEACTIVATE_USER"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateUserStatus activates a user")
    void updateUserStatus_activate() {
        adminUser.setActive(false);
        when(userRepository.findByPublicId("pub-admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDetailDto dto = service.updateUserStatus("pub-admin",
                new UserStatusRequest(true, "Rejoining"), "admin");

        assertThat(dto.active()).isTrue();
        verify(auditService).log(eq("ACTIVATE_USER"), any(), any(), any(), any(), any(), any(), any());
    }
}
