package com.skillmatrix.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity for the {@code requirement_versions} table.
 * Represents a versioned set of skill requirements for one application.
 * M3.1 — Application Skill Requirement Mapping.
 */
@Entity
@Table(name = "requirement_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "version_code", nullable = false, length = 100)
    private String versionCode;

    @Column(name = "version_name", nullable = false, length = 200)
    private String versionName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Lookup value for REQUIREMENT_STATUS (DRAFT / PUBLISHED / APPROVED / REJECTED).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_lookup_id")
    private LookupValue statusLookup;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

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
