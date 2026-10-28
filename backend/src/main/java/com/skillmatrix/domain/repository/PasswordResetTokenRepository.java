package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.PasswordResetToken;
import com.skillmatrix.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByPublicId(String publicId);

    /**
     * Invalidate all previously-issued active tokens for a user before issuing
     * a new one — ensures only the most recently requested reset link is usable.
     */
    @Modifying
    @Query("""
            UPDATE PasswordResetToken t SET t.active = false
            WHERE t.user = :user AND t.active = true AND t.usedAt IS NULL
            """)
    int invalidateAllActiveForUser(@Param("user") User user);

    /** Soft-cleanup helper for expired tokens (housekeeping job could use this). */
    @Modifying
    @Query("""
            UPDATE PasswordResetToken t SET t.active = false
            WHERE t.expiresAt < :now AND t.active = true
            """)
    int deactivateExpired(@Param("now") LocalDateTime now);

    /**
     * Pre-delete cleanup: permanently remove ALL reset tokens for a user
     * that is being hard-deleted. Reset tokens are not business history.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
