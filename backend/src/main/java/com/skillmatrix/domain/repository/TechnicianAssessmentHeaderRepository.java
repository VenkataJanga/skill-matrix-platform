package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.TechnicianAssessmentHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianAssessmentHeaderRepository extends JpaRepository<TechnicianAssessmentHeader, Long> {

    Optional<TechnicianAssessmentHeader> findByPublicId(String publicId);

    Optional<TechnicianAssessmentHeader> findByCycle_IdAndUser_Id(Long cycleId, Long userId);

    boolean existsByCycle_IdAndUser_Id(Long cycleId, Long userId);

    @Query("""
            SELECT h FROM TechnicianAssessmentHeader h
            JOIN FETCH h.cycle c
            JOIN FETCH c.application a
            JOIN FETCH c.requirementVersion rv
            LEFT JOIN FETCH h.statusLookup sl
            WHERE h.publicId = :publicId
            """)
    Optional<TechnicianAssessmentHeader> findDetailByPublicId(@Param("publicId") String publicId);

    @Query("""
            SELECT h FROM TechnicianAssessmentHeader h
            JOIN FETCH h.cycle c
            JOIN FETCH c.application a
            LEFT JOIN FETCH h.statusLookup sl
            WHERE h.user.id = :userId
            ORDER BY c.cycleStartDate DESC
            """)
    List<TechnicianAssessmentHeader> findAllByUserId(@Param("userId") Long userId);

    /** True if ANY header exists for this cycle — used to lock app/requirement version after first start. */
    boolean existsByCycle_Id(Long cycleId);
}
