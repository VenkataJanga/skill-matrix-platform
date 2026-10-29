package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.ExpectedRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpectedRatingRepository extends JpaRepository<ExpectedRating, Long>,
        JpaSpecificationExecutor<ExpectedRating> {

    Optional<ExpectedRating> findByPublicId(String publicId);

    Optional<ExpectedRating> findByRequirementVersion_IdAndSkill_Id(Long requirementVersionId, Long skillId);

    boolean existsByRequirementVersion_IdAndSkill_Id(Long requirementVersionId, Long skillId);

    boolean existsByRequirementVersion_IdAndSkill_IdAndIdNot(Long requirementVersionId, Long skillId, Long id);

    /**
     * Count of ACTIVE skill requirements for a Requirement Version. Used by the
     * Assessment Cycle "Requirement Version" dropdown to warn Admin BEFORE cycle
     * creation when a version has zero active requirements — the backend Open-time
     * safeguard in AssessmentCycleService.openCycle() remains the final authoritative
     * check and is unchanged by this query.
     */
    long countByRequirementVersion_IdAndActiveTrue(Long requirementVersionId);

    @Query("""
            SELECT er FROM ExpectedRating er
            JOIN FETCH er.skill s
            JOIN FETCH er.requirementVersion rv
            LEFT JOIN FETCH er.criticalityLookup
            LEFT JOIN FETCH er.roleLookup
            LEFT JOIN FETCH er.gradeLookup
            WHERE rv.id = :requirementVersionId
            ORDER BY s.skillCode
            """)
    List<ExpectedRating> findAllByRequirementVersionId(@Param("requirementVersionId") Long requirementVersionId);

    /**
     * Bulk-load existing ExpectedRating rows for a given version + a set of skill ids.
     * Used by SMART_UPSERT to compare DB state in a single query.
     */
    @Query("""
            SELECT er FROM ExpectedRating er
            JOIN er.requirementVersion rv
            WHERE rv.id = :requirementVersionId AND er.skill.id IN :skillIds
            """)
    List<ExpectedRating> findAllByRequirementVersionIdAndSkillIdIn(
            @Param("requirementVersionId") Long requirementVersionId,
            @Param("skillIds") Collection<Long> skillIds);
}
