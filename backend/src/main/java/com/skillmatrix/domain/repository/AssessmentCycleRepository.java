package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.AssessmentCycle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentCycleRepository extends JpaRepository<AssessmentCycle, Long> {

    Optional<AssessmentCycle> findByPublicId(String publicId);

    boolean existsByApplication_IdAndCycleName(Long applicationId, String cycleName);

    /** Application Master delete-safety guard: true if any assessment cycle exists for this application. */
    boolean existsByApplication_Id(Long applicationId);

    /**
     * Case-insensitive lookup used to both detect duplicates AND retrieve the
     * conflicting cycle's details for a friendly error message. MySQL's default
     * utf8mb4_unicode_ci collation already makes the underlying UNIQUE(application_id,
     * cycle_name) constraint case-insensitive — this query makes the Java-level
     * pre-check consistent with that DB behavior.
     */
    Optional<AssessmentCycle> findByApplication_IdAndCycleNameIgnoreCase(Long applicationId, String cycleName);

    @Query("""
            SELECT ac FROM AssessmentCycle ac
            JOIN FETCH ac.application a
            JOIN FETCH ac.requirementVersion rv
            LEFT JOIN FETCH ac.statusLookup sl
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(ac.cycleName) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(a.applicationName) LIKE LOWER(CONCAT('%',:search,'%')))
              AND (:applicationPublicId IS NULL OR :applicationPublicId = '' OR a.publicId = :applicationPublicId)
              AND (:statusCode IS NULL OR :statusCode = '' OR (sl IS NOT NULL AND sl.valueCode = :statusCode))
            ORDER BY ac.createdAt DESC
            """)
    Page<AssessmentCycle> searchCycles(
            @Param("search") String search,
            @Param("applicationPublicId") String applicationPublicId,
            @Param("statusCode") String statusCode,
            Pageable pageable);

    /**
     * Cycles with OPEN status for a specific application — used to determine
     * technician-eligible cycles when combined with an active assignment check.
     */
    @Query("""
            SELECT ac FROM AssessmentCycle ac
            JOIN FETCH ac.application a
            JOIN FETCH ac.requirementVersion rv
            JOIN ac.statusLookup sl
            WHERE a.id IN :applicationIds
            ORDER BY ac.cycleStartDate DESC
            """)
    List<AssessmentCycle> findAllByApplicationIds(@Param("applicationIds") List<Long> applicationIds);

    /**
     * Counts downstream finalized business records (M5 Assessment Approval) for a
     * cycle. Used by {@code reopenCycle} as a defensive safety check — today this
     * always returns 0 because no M4 code path writes to {@code assessment_approvals},
     * but the check will correctly block reopening the moment M5 starts finalizing
     * approvals for a cycle. Implemented as a native query since no JPA entity exists
     * for this M5-future table yet (intentionally not introducing one here).
     */
    @Query(value = "SELECT COUNT(*) FROM assessment_approvals WHERE cycle_id = :cycleId", nativeQuery = true)
    long countFinalizedApprovalsForCycle(@Param("cycleId") Long cycleId);
}
