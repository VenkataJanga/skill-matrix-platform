package com.skillmatrix.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Returned by the /refresh endpoint — new access token only.
 */
@Getter
@Builder
public class TokenResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;   // seconds
}
