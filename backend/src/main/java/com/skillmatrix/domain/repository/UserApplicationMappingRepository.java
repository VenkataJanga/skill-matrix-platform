package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.UserApplicationMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserApplicationMappingRepository extends JpaRepository<UserApplicationMapping, Long> {

    Optional<UserApplicationMapping> findByPublicId(String publicId);

    List<UserApplicationMapping> findAllByActiveTrue();

    List<UserApplicationMapping> findByUser_IdAndActiveTrue(Long userId);

    boolean existsByUser_IdAndApplication_IdAndActiveTrue(Long userId, Long applicationId);

    @Query("""
            SELECT uam FROM UserApplicationMapping uam
            JOIN FETCH uam.user u
            JOIN FETCH uam.application a
            JOIN FETCH a.portfolio p
            JOIN FETCH p.applicationType
            JOIN FETCH p.bundle
            WHERE uam.active = true
            ORDER BY u.username, a.applicationName
            """)
    List<UserApplicationMapping> findAllActiveMappingsWithDetails();

    @Query("""
            SELECT uam FROM UserApplicationMapping uam
            JOIN FETCH uam.user u
            JOIN FETCH uam.application a
            WHERE u.id = :userId AND uam.active = true
            ORDER BY a.applicationName
            """)
    List<UserApplicationMapping> findActiveByUserId(@Param("userId") Long userId);

    /**
     * Paginated search on assignments.
     * Supports search by application name/code, technician username/fullName/email.
     * Optionally filter by applicationPublicId, technicianPublicId, typeCode, bundleCode, active status.
     */
    @Query("""
            SELECT uam FROM UserApplicationMapping uam
            JOIN uam.user u
            JOIN uam.application a
            JOIN a.portfolio p
            JOIN p.applicationType at
            JOIN p.bundle b
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(a.applicationName) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(a.applicationCode) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(u.username)        LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(u.fullName)        LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(u.email)           LIKE LOWER(CONCAT('%',:search,'%')))
              AND (:appPublicId IS NULL OR :appPublicId = '' OR a.publicId = :appPublicId)
              AND (:techPublicId IS NULL OR :techPublicId = '' OR u.publicId = :techPublicId)
              AND (:typeCode IS NULL OR :typeCode = '' OR at.typeCode = :typeCode)
              AND (:bundleCode IS NULL OR :bundleCode = '' OR b.bundleCode = :bundleCode)
              AND (:activeFilter IS NULL OR uam.active = :activeFilter)
            ORDER BY u.username, a.applicationName
            """)
    Page<UserApplicationMapping> searchAssignments(
            @Param("search") String search,
            @Param("appPublicId") String appPublicId,
            @Param("techPublicId") String techPublicId,
            @Param("typeCode") String typeCode,
            @Param("bundleCode") String bundleCode,
            @Param("activeFilter") Boolean activeFilter,
            Pageable pageable);

    /**
     * Find the active mapping (if any) for a given user+application pair.
     * Used by duplicate check endpoint.
     */
    @Query("""
            SELECT uam FROM UserApplicationMapping uam
            WHERE uam.user.publicId = :userPublicId
              AND uam.application.publicId = :appPublicId
              AND uam.active = true
            """)
    Optional<UserApplicationMapping> findActiveByUserPublicIdAndAppPublicId(
            @Param("userPublicId") String userPublicId,
            @Param("appPublicId") String appPublicId);
}
