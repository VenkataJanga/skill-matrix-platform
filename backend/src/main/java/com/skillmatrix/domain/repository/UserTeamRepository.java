package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.UserTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {

    /** Returns the active team memberships for a given user. */
    @Query("SELECT ut FROM UserTeam ut JOIN FETCH ut.team WHERE ut.user.id = :userId AND ut.active = true")
    List<UserTeam> findActiveByUserId(@Param("userId") Long userId);

    /** Returns the first active team for a user (for export: primary team name). */
    @Query("SELECT ut FROM UserTeam ut JOIN FETCH ut.team WHERE ut.user.id = :userId AND ut.active = true ORDER BY ut.joinedAt ASC")
    Optional<UserTeam> findFirstActiveByUserId(@Param("userId") Long userId);

    /** Checks if user already has an active membership for a specific team. */
    @Query("SELECT COUNT(ut) > 0 FROM UserTeam ut WHERE ut.user.id = :userId AND ut.team.id = :teamId AND ut.active = true")
    boolean existsActiveByUserIdAndTeamId(@Param("userId") Long userId, @Param("teamId") Long teamId);

    /** Pre-delete cleanup: remove all team memberships for a user being permanently deleted. */
    void deleteAllByUser_Id(Long userId);
}
