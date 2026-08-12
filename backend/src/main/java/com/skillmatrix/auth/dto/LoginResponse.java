package com.skillmatrix.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;          // seconds until access token expires
    private String userPublicId;
    private String username;
    private String fullName;
    private List<String> roles;
    private boolean mustChangePassword;
}
