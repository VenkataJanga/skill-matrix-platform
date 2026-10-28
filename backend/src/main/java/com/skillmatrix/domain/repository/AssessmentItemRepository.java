package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.AssessmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentItemRepository extends JpaRepository<AssessmentItem, Long> {

    Optional<AssessmentItem> findByPublicId(String publicId);

    @Query("""
            SELECT i FROM AssessmentItem i
            JOIN FETCH i.expectedRating er
            JOIN FETCH er.skill s
            LEFT JOIN FETCH s.skillCategory
            LEFT JOIN FETCH s.skillScopeLookup
            WHERE i.header.id = :headerId
            ORDER BY s.skillCode
            """)
    List<AssessmentItem> findAllByHeaderId(@Param("headerId") Long headerId);

    List<AssessmentItem> findAllByHeader_Id(Long headerId);
}
