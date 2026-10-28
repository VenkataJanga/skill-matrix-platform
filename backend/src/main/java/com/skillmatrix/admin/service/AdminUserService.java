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
import com.skillmatrix.domain.entity.UserRole;
import com.skillmatrix.domain.repository.RoleRepository;
import com.skillmatrix.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminUserService {

    // Default password assigned to newly created users
    private static final String DEFAULT_PASSWORD = "Password@123";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // ---------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------

    public PagedResponse<UserSummaryDto> getUsers(
            int page, int size,
            String search, String roleCode, String status,
            String sortBy, String sortDirection) {

        Boolean activeFilter = parseActiveFilter(status);
        String cleanSearch = (search == null || search.isBlank()) ? null : search.trim();
        String cleanRole   = (roleCode == null || roleCode.isBlank()) ? null : roleCode.trim();

        Sort sort = buildSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> users = userRepository.searchUsers(cleanSearch, cleanRole, activeFilter, pageable);
        return PagedResponse.of(users.getContent().stream().map(UserSummaryDto::from).toList(), users);
    }

    public UserDetailDto getUserByPublicId(String publicId) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("User not found: " + publicId, HttpStatus.NOT_FOUND));
        return UserDetailDto.from(user);
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Transactional
    public UserDetailDto createUser(CreateUserRequest request, String actorUsername) {
        // Validate uniqueness
        if (userRepository.existsByUsername(request.username())) {
            log.warn("createUser rejected — username already exists: {}", request.username());
            throw new BusinessException("Username already exists: " + request.username(), HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.email())) {
            log.warn("createUser rejected — email already exists: {}", request.email());
            throw new BusinessException("Email already exists: " + request.email(), HttpStatus.CONFLICT);
        }

        Role role = roleRepository.findByRoleCode(request.roleCode())
                .orElseThrow(() -> new BusinessException("Invalid roleCode: " + request.roleCode()));

        User manager = null;
        if (request.managerPublicId() != null && !request.managerPublicId().isBlank()) {
            manager = userRepository.findByPublicId(request.managerPublicId())
                    .orElseThrow(() -> new BusinessException("Manager not found: " + request.managerPublicId()));
        }

        User user = User.builder()
                .publicId(UUID.randomUUID().toString())
                .username(request.username())
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .employeeId(request.employeeId())
                .primaryRole(role)
                .manager(manager)
                .mustChangePassword(request.mustChangePassword())
                .active(true)
                .createdBy(actorUsername)
                .updatedBy(actorUsername)
                .build();

        // Assign role in user_roles
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .assignedAt(LocalDateTime.now())
                .assignedBy(actorUsername)
                .active(true)
                .build();
        user.getUserRoles().add(userRole);

        User saved = userRepository.save(user);
        log.info("User created: publicId={} username={} role={} by={}",
                saved.getPublicId(), saved.getUsername(), role.getRoleCode(), actorUsername);

        auditService.log("CREATE_USER", AuditService.ENTITY_USER,
                null, actorUsername, saved.getPublicId(), null, null,
                "{\"username\":\"" + saved.getUsername() + "\",\"role\":\"" + role.getRoleCode() + "\"}");

        return UserDetailDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Transactional
    public UserDetailDto updateUser(String publicId, UpdateUserRequest request, String actorUsername) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("User not found: " + publicId, HttpStatus.NOT_FOUND));

        // Check email uniqueness if changed
        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmail(request.email())) {
            log.warn("updateUser rejected — email already exists: {} for publicId={}", request.email(), publicId);
            throw new BusinessException("Email already exists: " + request.email(), HttpStatus.CONFLICT);
        }

        Role role = roleRepository.findByRoleCode(request.roleCode())
                .orElseThrow(() -> new BusinessException("Invalid roleCode: " + request.roleCode()));

        User manager = null;
        if (request.managerPublicId() != null && !request.managerPublicId().isBlank()) {
            manager = userRepository.findByPublicId(request.managerPublicId())
                    .orElseThrow(() -> new BusinessException("Manager not found: " + request.managerPublicId()));
        }

        String oldValue = "{\"fullName\":\"" + user.getFullName() + "\",\"email\":\"" + user.getEmail() + "\"}";

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setEmployeeId(request.employeeId());
        user.setPrimaryRole(role);
        user.setManager(manager);
        user.setMustChangePassword(request.mustChangePassword());
        user.setUpdatedBy(actorUsername);

        // Update user_roles only when the role actually changes.
        // The user_roles table has UNIQUE (user_id, role_id), so always inserting
        // a new row for the same role would cause a constraint violation.
        Role currentPrimaryRole = user.getPrimaryRole();
        boolean roleChanged = currentPrimaryRole == null
                || !currentPrimaryRole.getId().equals(role.getId());

        if (roleChanged) {
            // Deactivate all existing active role assignments
            user.getUserRoles().forEach(ur -> ur.setActive(false));

            // Check if a (possibly deactivated) UserRole for the target role already
            // exists — reactivate it to avoid inserting a duplicate row.
            boolean reactivated = user.getUserRoles().stream()
                    .filter(ur -> ur.getRole().getId().equals(role.getId()))
                    .peek(ur -> {
                        ur.setActive(true);
                        ur.setAssignedAt(LocalDateTime.now());
                        ur.setAssignedBy(actorUsername);
                    })
                    .findFirst()
                    .isPresent();

            if (!reactivated) {
                // No prior UserRole record for the new role — create a fresh one
                UserRole newUserRole = UserRole.builder()
                        .user(user)
                        .role(role)
                        .assignedAt(LocalDateTime.now())
                        .assignedBy(actorUsername)
                        .active(true)
                        .build();
                user.getUserRoles().add(newUserRole);
            }
        }

        User saved = userRepository.save(user);
        log.info("User updated: publicId={} username={} by={}", publicId, saved.getUsername(), actorUsername);

        auditService.log("UPDATE_USER", AuditService.ENTITY_USER,
                null, actorUsername, publicId, null, null,
                "{\"old\":" + oldValue + ",\"new\":{\"fullName\":\"" + saved.getFullName() + "\"}}");

        return UserDetailDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Status toggle
    // ---------------------------------------------------------------

    @Transactional
    public UserDetailDto updateUserStatus(String publicId, UserStatusRequest request, String actorUsername) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException("User not found: " + publicId, HttpStatus.NOT_FOUND));

        user.setActive(request.active());
        user.setUpdatedBy(actorUsername);
        if (!request.active()) {
            user.setAccountLocked(false);
            user.setFailedLoginAttempts(0);
        }

        User saved = userRepository.save(user);

        String action = request.active() ? "ACTIVATE_USER" : "DEACTIVATE_USER";
        log.info("User status changed: publicId={} username={} active={} by={}",
                publicId, saved.getUsername(), request.active(), actorUsername);

        auditService.log(action, AuditService.ENTITY_USER,
                null, actorUsername, publicId, null, null,
                "{\"active\":" + request.active() + ",\"reason\":\"" + request.reason() + "\"}");

        return UserDetailDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Boolean parseActiveFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        if ("ACTIVE".equalsIgnoreCase(status)) return Boolean.TRUE;
        if ("INACTIVE".equalsIgnoreCase(status)) return Boolean.FALSE;
        return null;
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        String field = switch (sortBy == null ? "" : sortBy) {
            case "username" -> "username";
            case "email"    -> "email";
            case "fullName" -> "fullName";
            case "createdAt"-> "createdAt";
            default         -> "username";
        };
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
}
