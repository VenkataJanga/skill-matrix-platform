package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    boolean existsByPortfolio_IdAndApplicationCodeIgnoreCase(Long portfolioId, String applicationCode);

    Optional<Application> findByPortfolio_IdAndApplicationCodeIgnoreCase(Long portfolioId, String applicationCode);

    /**
     * Admin Application Master — paginated search/filter.
     * Filters by free-text search (code/name), type/bundle/portfolio code, lifecycle
     * status code and active/inactive — all optional (null/blank skips the filter).
     */
    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.portfolio p
            JOIN FETCH p.applicationType at
            JOIN FETCH p.bundle b
            LEFT JOIN FETCH a.lifecycleStatusLookup ls
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(a.applicationCode) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(a.applicationName) LIKE LOWER(CONCAT('%',:search,'%')))
              AND (:typeCode IS NULL OR :typeCode = '' OR at.typeCode = :typeCode)
              AND (:bundleCode IS NULL OR :bundleCode = '' OR b.bundleCode = :bundleCode)
              AND (:portfolioCode IS NULL OR :portfolioCode = '' OR p.portfolioCode = :portfolioCode)
              AND (:lifecycleStatusCode IS NULL OR :lifecycleStatusCode = ''
                   OR (ls IS NOT NULL AND ls.valueCode = :lifecycleStatusCode))
              AND (:activeFilter IS NULL OR a.active = :activeFilter)
            ORDER BY a.applicationCode
            """)
    Page<Application> searchApplications(
            @Param("search") String search,
            @Param("typeCode") String typeCode,
            @Param("bundleCode") String bundleCode,
            @Param("portfolioCode") String portfolioCode,
            @Param("lifecycleStatusCode") String lifecycleStatusCode,
            @Param("activeFilter") Boolean activeFilter,
            Pageable pageable);

    /** Export — same filters as search, but unpaged (all matching rows). */
    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.portfolio p
            JOIN FETCH p.applicationType at
            JOIN FETCH p.bundle b
            LEFT JOIN FETCH a.lifecycleStatusLookup ls
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(a.applicationCode) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(a.applicationName) LIKE LOWER(CONCAT('%',:search,'%')))
              AND (:typeCode IS NULL OR :typeCode = '' OR at.typeCode = :typeCode)
              AND (:bundleCode IS NULL OR :bundleCode = '' OR b.bundleCode = :bundleCode)
              AND (:portfolioCode IS NULL OR :portfolioCode = '' OR p.portfolioCode = :portfolioCode)
              AND (:lifecycleStatusCode IS NULL OR :lifecycleStatusCode = ''
                   OR (ls IS NOT NULL AND ls.valueCode = :lifecycleStatusCode))
              AND (:activeFilter IS NULL OR a.active = :activeFilter)
            ORDER BY a.applicationCode
            """)
    List<Application> searchApplicationsForExport(
            @Param("search") String search,
            @Param("typeCode") String typeCode,
            @Param("bundleCode") String bundleCode,
            @Param("portfolioCode") String portfolioCode,
            @Param("lifecycleStatusCode") String lifecycleStatusCode,
            @Param("activeFilter") Boolean activeFilter);

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
