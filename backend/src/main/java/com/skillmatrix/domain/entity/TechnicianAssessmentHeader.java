package com.skillmatrix.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code technician_assessment_headers} table.
 * Represents ONE technician's self-assessment for ONE cycle as a whole
 * (authoritative status/submitted_at/concurrency token).
 * M4 — Technician Self-Assessment.
 *
 * application_id / requirement_version_id are intentionally NOT duplicated
 * here — always resolved via {@code cycle.getApplication()} /
 * {@code cycle.getRequirementVersion()} per architecture decision.
 */
@Entity
@Table(name = "technician_assessment_headers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianAssessmentHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AssessmentCycle cycle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Lookup value for ASSESSMENT_STATUS. M4 uses DRAFT, SUBMITTED only.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_lookup_id")
    private LookupValue statusLookup;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** Optimistic locking token — protects concurrent draft saves. */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @OneToMany(mappedBy = "header", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssessmentItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
