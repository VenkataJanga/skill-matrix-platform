package com.skillmatrix.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService — unit tests")
class JwtServiceTest {

    private static final String SECRET =
            "TestSecretKeyForJwtServiceUnitTests-MustBe256BitsLongAtMinimum!";
    private static final long EXPIRY_MS = 900_000L; // 15 min

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRY_MS);
    }

    @Test
    @DisplayName("generateToken produces a non-blank JWT")
    void generateToken_returnsNonBlankJwt() {
        String token = jwtService.generateToken("pub-001", "alice", List.of("ADMIN"));
        assertThat(token).isNotBlank();
        // JWT has 3 dot-separated parts
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("validateAndGetClaims returns correct subject and claims")
    void validateAndGetClaims_returnsCorrectClaims() {
        String token = jwtService.generateToken("pub-001", "alice", List.of("ADMIN"));

        Claims claims = jwtService.validateAndGetClaims(token);

        assertThat(claims.getSubject()).isEqualTo("pub-001");
        assertThat(claims.get("username", String.class)).isEqualTo("alice");
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        assertThat(roles).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("extractPublicId returns the token subject")
    void extractPublicId_returnsSubject() {
        String token = jwtService.generateToken("pub-xyz", "bob", List.of("TECHNICIAN"));
        assertThat(jwtService.extractPublicId(token)).isEqualTo("pub-xyz");
    }

    @Test
    @DisplayName("isTokenValid returns true for a fresh valid token")
    void isTokenValid_trueForFreshToken() {
        String token = jwtService.generateToken("pub-001", "alice", List.of("ADMIN"));
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false for a tampered token")
    void isTokenValid_falseForTamperedToken() {
        String token = jwtService.generateToken("pub-001", "alice", List.of("ADMIN"));
        // Tamper the signature
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsig";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid returns false for a blank token")
    void isTokenValid_falseForBlankToken() {
        assertThat(jwtService.isTokenValid("")).isFalse();
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateAndGetClaims throws JwtException for an expired token")
    void validateAndGetClaims_throwsForExpiredToken() {
        JwtService shortLivedService = new JwtService(SECRET, -1L); // already expired
        String token = shortLivedService.generateToken("pub-001", "alice", List.of("ADMIN"));

        assertThatThrownBy(() -> jwtService.validateAndGetClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("getExpirationMs returns configured value")
    void getExpirationMs_returnsConfigured() {
        assertThat(jwtService.getExpirationMs()).isEqualTo(EXPIRY_MS);
    }
}
