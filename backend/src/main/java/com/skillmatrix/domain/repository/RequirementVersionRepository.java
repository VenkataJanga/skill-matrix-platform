package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.RequirementVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequirementVersionRepository extends JpaRepository<RequirementVersion, Long> {

    Optional<RequirementVersion> findByPublicId(String publicId);

    Optional<RequirementVersion> findByApplication_IdAndVersionCode(Long applicationId, String versionCode);

    boolean existsByApplication_IdAndVersionCode(Long applicationId, String versionCode);

    /** Application Master delete-safety guard: true if any requirement version exists for this application. */
    boolean existsByApplication_Id(Long applicationId);

    @Query("""
            SELECT rv FROM RequirementVersion rv
            JOIN FETCH rv.application a
            LEFT JOIN FETCH rv.statusLookup sl
            WHERE a.id = :applicationId
            ORDER BY rv.createdAt DESC
            """)
    List<RequirementVersion> findAllByApplicationId(@Param("applicationId") Long applicationId);

    @Query("""
            SELECT rv FROM RequirementVersion rv
            JOIN FETCH rv.application a
            LEFT JOIN FETCH rv.statusLookup sl
            WHERE a.publicId = :applicationPublicId
            ORDER BY rv.createdAt DESC
            """)
    List<RequirementVersion> findAllByApplicationPublicId(@Param("applicationPublicId") String applicationPublicId);

    /**
     * Returns only requirement versions that are eligible for Assessment Cycle
     * creation: active and NOT in a terminal/rejected state. DRAFT, PUBLISHED and
     * APPROVED remain eligible (unchanged M3.1 lifecycle); REJECTED and inactive
     * (soft-deleted) versions are excluded. Used to populate the Assessment Cycle
     * "Requirement Version" dropdown so Admins are never offered an ineligible
     * version — while leaving the full, unfiltered list (above) available for
     * other Admin screens (e.g. Skill Requirement Mapping) that intentionally
     * need to see every version regardless of status.
     */
    @Query("""
            SELECT rv FROM RequirementVersion rv
            JOIN FETCH rv.application a
            LEFT JOIN FETCH rv.statusLookup sl
            WHERE a.publicId = :applicationPublicId
              AND rv.active = true
              AND (sl.valueCode IS NULL OR sl.valueCode <> 'REJECTED')
            ORDER BY rv.createdAt DESC
            """)
    List<RequirementVersion> findEligibleByApplicationPublicId(@Param("applicationPublicId") String applicationPublicId);

    /**
     * Returns the most recent DRAFT version for an application, if one exists.
     * Used by the import flow to default to (or create) a DRAFT version.
     */
    @Query("""
            SELECT rv FROM RequirementVersion rv
            JOIN rv.application a
            JOIN rv.statusLookup sl
            WHERE a.id = :applicationId AND sl.valueCode = 'DRAFT'
            ORDER BY rv.createdAt DESC
            """)
    List<RequirementVersion> findDraftsByApplicationId(@Param("applicationId") Long applicationId);
}
