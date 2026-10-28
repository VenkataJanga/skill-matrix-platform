package com.skillmatrix.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private ApplicationPortfolio portfolio;

    @Column(name = "application_code", nullable = false, length = 100)
    private String applicationCode;

    @Column(name = "application_name", nullable = false, length = 200)
    private String applicationName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Lookup value for APPLICATION_LIFECYCLE_STATUS (ACTIVE / INACTIVE / DEPRECATED / FUTURE).
     * Column already existed in the DB schema (V01) — this JPA mapping was added during
     * the M4 Application Master stabilization enhancement with no schema change required.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lifecycle_status_lookup_id")
    private LookupValue lifecycleStatusLookup;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

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
