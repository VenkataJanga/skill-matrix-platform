package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.ImportExportHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ImportExportHistoryRepository extends JpaRepository<ImportExportHistory, Long> {

    Optional<ImportExportHistory> findByPublicId(String publicId);

    /** Used by delete guard: true if the user has initiated any import/export history record. */
    boolean existsByInitiatedById(Long userId);

    @Query("""
            SELECT h FROM ImportExportHistory h
            JOIN FETCH h.initiatedBy u
            LEFT JOIN FETCH h.statusLookup sl
            WHERE (:templateType IS NULL OR :templateType = '' OR h.templateType = :templateType)
              AND (:statusCode IS NULL OR :statusCode = ''
                   OR (sl IS NOT NULL AND sl.valueCode = :statusCode))
              AND (:uploadedBy IS NULL OR :uploadedBy = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%',:uploadedBy,'%')))
              AND (:fromDate IS NULL OR h.startedAt >= :fromDate)
              AND (:toDate IS NULL OR h.startedAt <= :toDate)
            ORDER BY h.startedAt DESC
            """)
    Page<ImportExportHistory> searchHistory(
            @Param("templateType") String templateType,
            @Param("statusCode")   String statusCode,
            @Param("uploadedBy")   String uploadedBy,
            @Param("fromDate")     LocalDateTime fromDate,
            @Param("toDate")       LocalDateTime toDate,
            Pageable pageable);
}
