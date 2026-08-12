package com.skillmatrix.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Stateless JWT utility — generates and validates HS256 signed access tokens.
 *
 * <p>Token subject = user {@code public_id} (never the internal numeric id).
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a signed JWT access token.
     *
     * @param publicId  subject (user public_id)
     * @param username  stored in claims
     * @param roles     list of ROLE_CODE strings
     */
    public String generateToken(String publicId, String username, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(publicId)
                .claims(Map.of(
                        "username", username,
                        "roles", roles
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate token and return all claims.
     *
     * @throws JwtException if the token is invalid or expired
     */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract subject (public_id) from a validated token.
     */
    public String extractPublicId(String token) {
        return validateAndGetClaims(token).getSubject();
    }

    /**
     * Check token validity without throwing — returns false on any error.
     */
    public boolean isTokenValid(String token) {
        try {
            validateAndGetClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
