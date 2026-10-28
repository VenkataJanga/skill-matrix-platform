package com.skillmatrix.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_export_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportExportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    /**
     * IMPORT or EXPORT
     */
    @Column(name = "operation_type", nullable = false, length = 20)
    private String operationType;

    /**
     * e.g. SKILLS_IMPORT, USERS_AND_SKILLS
     */
    @Column(name = "template_type", nullable = false, length = 100)
    private String templateType;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "success_rows")
    private Integer successRows;

    @Column(name = "failed_rows")
    private Integer failedRows;

    @Column(name = "inserted_rows")
    private Integer insertedRows;

    @Column(name = "updated_rows")
    private Integer updatedRows;

    @Column(name = "skipped_rows")
    private Integer skippedRows;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_lookup_id")
    private LookupValue statusLookup;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "import_mode", nullable = false, length = 20)
    private String importMode;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }
}
