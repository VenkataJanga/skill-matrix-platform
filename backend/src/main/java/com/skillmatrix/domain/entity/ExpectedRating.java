package com.skillmatrix.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity for the {@code expected_ratings} table.
 * Represents one skill requirement row within a RequirementVersion:
 * skill + expected proficiency level + mandatory flag + criticality + role/grade.
 * M3.1 — Application Skill Requirement Mapping.
 */
@Entity
@Table(name = "expected_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpectedRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_version_id", nullable = false)
    private RequirementVersion requirementVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "expected_level", nullable = false)
    private Integer expectedLevel;

    /**
     * Lookup value for CRITICALITY (HIGH / MEDIUM / LOW). Optional.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criticality_lookup_id")
    private LookupValue criticalityLookup;

    /**
     * Optional role applicability (reserved for future use — forward-compatible column).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_lookup_id")
    private LookupValue roleLookup;

    /**
     * Optional grade applicability (reserved for future use — forward-compatible column).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_lookup_id")
    private LookupValue gradeLookup;

    @Column(name = "min_people_required", nullable = false)
    @Builder.Default
    private Integer minPeopleRequired = 1;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private boolean mandatory = true;

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
