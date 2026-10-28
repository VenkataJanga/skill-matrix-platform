package com.skillmatrix.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity for the {@code skills} table.
 * V09 adds application_type_id, bundle_id, application_id for scope-based FKs.
 */
@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_category_id", nullable = false)
    private SkillCategory skillCategory;

    @Column(name = "skill_code", nullable = false, unique = true, length = 100)
    private String skillCode;

    @Column(name = "skill_name", nullable = false, length = 200)
    private String skillName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Lookup value for SKILL_TYPE (TECHNICAL / NON_TECHNICAL).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_type_lookup_id")
    private LookupValue skillTypeLookup;

    /**
     * Lookup value for SKILL_SCOPE (GLOBAL / APPLICATION_TYPE / BUNDLE / APPLICATION / GENERIC).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_scope_lookup_id")
    private LookupValue skillScopeLookup;

    /**
     * Populated when scope = APPLICATION_TYPE.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_type_id")
    private ApplicationType applicationType;

    /**
     * Populated when scope = BUNDLE.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id")
    private Bundle bundle;

    /**
     * Populated when scope = APPLICATION.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

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
