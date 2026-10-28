package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long>,
        JpaSpecificationExecutor<Skill> {

    Optional<Skill> findByPublicId(String publicId);

    Optional<Skill> findBySkillCode(String skillCode);

    boolean existsBySkillCode(String skillCode);

    boolean existsBySkillCodeAndActiveTrue(String skillCode);

    boolean existsBySkillCodeAndIdNot(String skillCode, Long id);

    /** Application Master delete-safety guard: true if any skill is scoped to this application. */
    boolean existsByApplication_Id(Long applicationId);

    /**
     * Check which skill codes in a given set already exist in the database.
     * Used during Excel validation.
     */
    @Query("SELECT s.skillCode FROM Skill s WHERE s.skillCode IN :codes")
    Set<String> findExistingSkillCodes(@Param("codes") List<String> codes);

    /**
     * Bulk-load full Skill entities for all given skill codes.
     * Used by SMART_UPSERT to perform DB-comparison in a single query.
     */
    List<Skill> findAllBySkillCodeIn(java.util.Collection<String> skillCodes);
}
