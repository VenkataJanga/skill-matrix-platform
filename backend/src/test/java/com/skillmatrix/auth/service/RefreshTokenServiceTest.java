package com.skillmatrix.auth.service;

import com.skillmatrix.common.exception.AuthException;
import com.skillmatrix.domain.entity.RefreshToken;
import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService — unit tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604_800_000L);
    }

    // ----------------------------------------------------------------
    // createRefreshToken
    // ----------------------------------------------------------------

    @Test
    @DisplayName("createRefreshToken saves token and returns raw UUID string")
    void createRefreshToken_savesAndReturnsRawToken() {
        User user = userWith(1L, "alice");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(user);

        assertThat(rawToken).isNotBlank();
        // UUID format
        assertThat(rawToken).matches("[0-9a-f-]{36}");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);  // hash ≠ raw
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("createRefreshToken stores SHA-256 hash of the raw token")
    void createRefreshToken_storesSha256Hash() {
        User user = userWith(1L, "alice");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(user);
        String expectedHash = RefreshTokenService.sha256(rawToken);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getTokenHash()).isEqualTo(expectedHash);
    }

    // ----------------------------------------------------------------
    // validateRefreshToken
    // ----------------------------------------------------------------

    @Test
    @DisplayName("validateRefreshToken returns token when valid")
    void validateRefreshToken_returnsValidToken() {
        String raw = "test-raw-token";
        String hash = RefreshTokenService.sha256(raw);

        RefreshToken stored = RefreshToken.builder()
                .tokenHash(hash)
                .revoked(false)
                .deleted(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        RefreshToken result = refreshTokenService.validateRefreshToken(raw);
        assertThat(result).isEqualTo(stored);
    }

    @Test
    @DisplayName("validateRefreshToken throws AuthException for unknown token")
    void validateRefreshToken_throwsForUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("unknown"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("validateRefreshToken throws AuthException for revoked token")
    void validateRefreshToken_throwsForRevokedToken() {
        String raw = "revoked-token";
        RefreshToken revoked = RefreshToken.builder()
                .tokenHash(RefreshTokenService.sha256(raw))
                .revoked(true)
                .deleted(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(raw))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("expired or revoked");
    }

    @Test
    @DisplayName("validateRefreshToken throws AuthException for expired token")
    void validateRefreshToken_throwsForExpiredToken() {
        String raw = "expired-token";
        RefreshToken expired = RefreshToken.builder()
                .tokenHash(RefreshTokenService.sha256(raw))
                .revoked(false)
                .deleted(false)
                .expiresAt(LocalDateTime.now().minusDays(1))  // in the past
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(raw))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("expired or revoked");
    }

    // ----------------------------------------------------------------
    // revokeAllForUser
    // ----------------------------------------------------------------

    @Test
    @DisplayName("revokeAllForUser calls repository bulk revoke")
    void revokeAllForUser_callsRepository() {
        User user = userWith(1L, "alice");
        when(refreshTokenRepository.revokeAllByUser(eq(user), any())).thenReturn(2);

        refreshTokenService.revokeAllForUser(user);

        verify(refreshTokenRepository).revokeAllByUser(eq(user), any(LocalDateTime.class));
    }

    // ----------------------------------------------------------------
    // sha256 helper
    // ----------------------------------------------------------------

    @Test
    @DisplayName("sha256 is deterministic and produces 64-char hex")
    void sha256_isDeterministicAndProduces64CharHex() {
        String hash1 = RefreshTokenService.sha256("hello");
        String hash2 = RefreshTokenService.sha256("hello");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64).matches("[0-9a-f]+");
        assertThat(RefreshTokenService.sha256("hello"))
                .isNotEqualTo(RefreshTokenService.sha256("world"));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private User userWith(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPublicId("pub-" + id);
        return u;
    }
}
