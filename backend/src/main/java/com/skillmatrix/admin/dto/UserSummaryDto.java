package com.skillmatrix.admin.dto;

import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserRole;

import java.util.List;

public record UserSummaryDto(
        String publicId,
        String username,
        String fullName,
        String email,
        boolean active,
        List<String> roles
) {
    public static UserSummaryDto from(User user) {
        List<String> roleNames = user.getUserRoles() == null ? List.of() :
                user.getUserRoles().stream()
                        .filter(UserRole::isActive)
                        .map(ur -> ur.getRole().getRoleCode())
                        .sorted()
                        .toList();
        return new UserSummaryDto(
                user.getPublicId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.isActive(),
                roleNames
        );
    }
}
