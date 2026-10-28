package com.skillmatrix.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity for the {@code technician_assessments} table.
 * Represents ONE skill rating within a technician's assessment
 * (one row per header + expected_rating). M4 — Technician Self-Assessment.
 *
 * Legacy columns {@code cycle_id}, {@code user_id}, {@code status_lookup_id}
 * and {@code submitted_at} remain on the underlying table for backward
 * compatibility with any pre-M4 data, but are NOT written by new M4 code —
 * the {@link TechnicianAssessmentHeader} is now authoritative for status
 * and submission timestamp.
 */
@Entity
@Table(name = "technician_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_header_id", nullable = false)
    private TechnicianAssessmentHeader header;

    /**
     * Legacy column — kept in sync with header.cycle for existing FK/unique
     * constraint compatibility, but never read as source of truth by M4 code.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private AssessmentCycle cycle;

    /** Legacy column — see class javadoc. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Resolves the skill, expected level (server-side only) and mandatory
     * flag for this item. NEVER expose this association via a
     * technician-facing DTO.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expected_rating_id", nullable = false)
    private ExpectedRating expectedRating;

    @Column(name = "self_rating")
    private Integer selfRating;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "technician_comment", columnDefinition = "TEXT")
    private String technicianComment;

    /** Legacy column — see class javadoc; unused by new M4 writes. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_lookup_id")
    private LookupValue statusLookup;

    /** Legacy column — see class javadoc; unused by new M4 writes. */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

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
