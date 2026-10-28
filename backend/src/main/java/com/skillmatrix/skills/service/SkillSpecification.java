package com.skillmatrix.skills.service;

import com.skillmatrix.domain.entity.Skill;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic Skill filtering.
 * Replaces the old JPQL @Query searchSkills() to work reliably with Hibernate 6.5+.
 * Uses LEFT JOIN FETCH for skillCategory (always needed in SkillDto).
 */
public final class SkillSpecification {

    private SkillSpecification() {}

    /**
     * Build a Specification for paginated skill search.
     * All parameters are nullable — null means "no filter on this field".
     */
    public static Specification<Skill> build(
            String search,
            String categoryCode,
            String scopeCode,
            String appTypeCode,
            String bundleCode,
            String appPublicId,
            Boolean activeFilter) {

        return (root, query, cb) -> {
            // ---- Fetch associations eagerly for SkillDto mapping ----
            // Only apply fetch joins for data queries, not count queries
            if (query != null && !query.getResultType().equals(Long.class)
                    && !query.getResultType().equals(long.class)) {
                root.fetch("skillCategory", JoinType.LEFT);
                root.fetch("skillScopeLookup", JoinType.LEFT);
                root.fetch("skillTypeLookup", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            // ---- Text search ----
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("skillCode")),  pattern),
                        cb.like(cb.lower(root.get("skillName")),  pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            // ---- Category ----
            if (categoryCode != null && !categoryCode.isBlank()) {
                predicates.add(
                        cb.equal(root.get("skillCategory").get("categoryCode"), categoryCode));
            }

            // ---- Scope ----
            if (scopeCode != null && !scopeCode.isBlank()) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("skillScopeLookup")),
                        cb.equal(root.get("skillScopeLookup").get("valueCode"), scopeCode)));
            }

            // ---- Application Type ----
            if (appTypeCode != null && !appTypeCode.isBlank()) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("applicationType")),
                        cb.equal(root.get("applicationType").get("typeCode"), appTypeCode)));
            }

            // ---- Bundle ----
            if (bundleCode != null && !bundleCode.isBlank()) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("bundle")),
                        cb.equal(root.get("bundle").get("bundleCode"), bundleCode)));
            }

            // ---- Application ----
            if (appPublicId != null && !appPublicId.isBlank()) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("application")),
                        cb.equal(root.get("application").get("publicId"), appPublicId)));
            }

            // ---- Active filter ----
            if (activeFilter != null) {
                predicates.add(cb.equal(root.get("active"), activeFilter));
            }
            // null activeFilter = no restriction = show all

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
