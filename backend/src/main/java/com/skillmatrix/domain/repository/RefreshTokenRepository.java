package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.RefreshToken;
import com.skillmatrix.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByPublicId(String publicId);

    /** Revoke all active tokens for a user (used on logout and password change). */
    @Modifying
    @Query("""
            UPDATE RefreshToken rt SET rt.revoked = true, rt.updatedAt = :now
            WHERE rt.user = :user AND rt.revoked = false AND rt.deleted = false
            """)
    int revokeAllByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /** Soft-delete expired tokens (cleanup job can use this). */
    @Modifying
    @Query("""
            UPDATE RefreshToken rt SET rt.deleted = true, rt.updatedAt = :now
            WHERE rt.expiresAt < :now AND rt.deleted = false
            """)
    int softDeleteExpired(@Param("now") LocalDateTime now);
}
