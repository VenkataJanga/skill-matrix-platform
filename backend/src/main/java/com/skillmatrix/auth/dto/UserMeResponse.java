package com.skillmatrix.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response for GET /api/v1/auth/me — never exposes internal numeric IDs.
 */
@Getter
@Builder
public class UserMeResponse {

    private String publicId;
    private String username;
    private String email;
    private String fullName;
    private String employeeId;
    private List<String> roles;
    private List<String> permissions;
    private boolean mustChangePassword;
    private LocalDateTime lastLogin;
}
