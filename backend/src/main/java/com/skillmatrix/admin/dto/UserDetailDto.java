package com.skillmatrix.admin.dto;

import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserRole;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full user profile returned by GET /admin/users/{publicId}.
 */
public record UserDetailDto(
        String publicId,
        String username,
        String fullName,
        String email,
        String employeeId,
        String primaryRoleCode,
        String managerPublicId,
        String managerUsername,
        boolean active,
        boolean mustChangePassword,
        boolean accountLocked,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public static UserDetailDto from(User user) {
        List<String> roleNames = user.getUserRoles() == null ? List.of() :
                user.getUserRoles().stream()
                        .filter(UserRole::isActive)
                        .map(ur -> ur.getRole().getRoleCode())
                        .sorted()
                        .toList();
        return new UserDetailDto(
                user.getPublicId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getEmployeeId(),
                user.getPrimaryRole() != null ? user.getPrimaryRole().getRoleCode() : null,
                user.getManager() != null ? user.getManager().getPublicId() : null,
                user.getManager() != null ? user.getManager().getUsername() : null,
                user.isActive(),
                user.isMustChangePassword(),
                user.isAccountLocked(),
                roleNames,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getCreatedBy(),
                user.getUpdatedBy()
        );
    }
}
