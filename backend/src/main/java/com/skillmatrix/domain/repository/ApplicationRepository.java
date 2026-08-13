package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findAllByActiveTrue();

    Optional<Application> findByPublicId(String publicId);

    Optional<Application> findByApplicationCode(String applicationCode);

    /**
     * Filter active applications by application type code and/or bundle code.
     * Either parameter can be null to skip that filter.
     */
    @Query("""
            SELECT a FROM Application a
            JOIN a.portfolio p
            JOIN p.applicationType at
            JOIN p.bundle b
            WHERE a.active = true
              AND (:typeCode IS NULL OR at.typeCode = :typeCode)
              AND (:bundleCode IS NULL OR b.bundleCode = :bundleCode)
            ORDER BY a.applicationName
            """)
    List<Application> findActiveByTypeCodeAndBundleCode(
            @Param("typeCode") String typeCode,
            @Param("bundleCode") String bundleCode);

    /**
     * Find all active applications assigned to a specific user.
     */
    @Query("""
            SELECT DISTINCT a FROM Application a
            JOIN UserApplicationMapping uam ON uam.application = a
            WHERE uam.user.id = :userId
              AND uam.active = true
              AND a.active = true
            ORDER BY a.applicationName
            """)
    List<Application> findActiveApplicationsByUserId(@Param("userId") Long userId);

    /**
     * Find all active applications in teams where the given user is a member.
     * Used for Lead Manager context view.
     */
    @Query("""
            SELECT DISTINCT a FROM Application a
            JOIN UserApplicationMapping uam ON uam.application = a
            WHERE uam.active = true
              AND a.active = true
              AND uam.user.id IN (
                  SELECT ut.user.id FROM UserTeam ut
                  WHERE ut.team.id IN (
                      SELECT ut2.team.id FROM UserTeam ut2
                      WHERE ut2.user.id = :leadUserId AND ut2.active = true
                  )
                  AND ut.active = true
              )
            ORDER BY a.applicationName
            """)
    List<Application> findApplicationsByLeadTeamMembership(@Param("leadUserId") Long leadUserId);
}
