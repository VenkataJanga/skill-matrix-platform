package com.skillmatrix.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity for the {@code assessment_cycles} table.
 * Represents a time-boxed assessment window for one application,
 * bound to an exact RequirementVersion (never "latest").
 * M4 — Assessment Cycle Management.
 */
@Entity
@Table(name = "assessment_cycles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_version_id", nullable = false)
    private RequirementVersion requirementVersion;

    @Column(name = "cycle_name", nullable = false, length = 100)
    private String cycleName;

    @Column(name = "cycle_start_date", nullable = false)
    private LocalDate cycleStartDate;

    @Column(name = "cycle_end_date", nullable = false)
    private LocalDate cycleEndDate;

    /**
     * Lookup value for ASSESSMENT_CYCLE_STATUS (PLANNED / OPEN / IN_REVIEW / CLOSED / CANCELLED).
     * M4 uses PLANNED, OPEN, CLOSED only.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_lookup_id")
    private LookupValue statusLookup;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

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
