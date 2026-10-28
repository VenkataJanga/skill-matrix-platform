package com.skillmatrix.requirements.service;

import com.skillmatrix.domain.entity.ExpectedRating;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic ExpectedRating (skill requirement) filtering.
 * Mirrors SkillSpecification's pattern for consistency.
 *
 * <p>IMPORTANT: when a LEFT JOIN FETCH is registered for an association (e.g.
 * "skill"/"requirementVersion"), any subsequent predicate that needs to
 * navigate into that same association MUST reuse the {@link Fetch}/{@link Path}
 * object already created by {@code root.fetch(...)} — NOT call
 * {@code root.get(...)} again. Calling {@code root.get(...)} on an
 * association that already has a registered LEFT JOIN FETCH causes Hibernate
 * to attempt to re-resolve the same path with default (INNER) join
 * semantics, which conflicts with the existing LEFT OUTER fetch and throws:
 * "Requested join fetch with association [x] with 'inner' join type, but
 * found existing join fetch with 'left outer' join type." This previously
 * caused HTTP 500 on every /api/v1/admin/skill-requirements call that
 * resolved to a non-count query, REGARDLESS of how many rows existed —
 * including for an Application/Requirement Version with zero requirements.
 */
public final class SkillRequirementSpecification {

    private SkillRequirementSpecification() {}

    public static Specification<ExpectedRating> build(
            Long requirementVersionId,
            String applicationPublicId,
            String search,
            String skillGroup,
            Boolean mandatoryFilter,
            Boolean activeFilter) {

        return (root, query, cb) -> {
            boolean isDataQuery = query != null && !query.getResultType().equals(Long.class)
                    && !query.getResultType().equals(long.class);

            // Reusable join/path handles — populated once per branch below and
            // reused for ALL subsequent predicate navigation into the same
            // association, whether this is a data query (fetch-joined) or a
            // count query (plain join, no fetch).
            Path<Object> skillPath;
            Path<Object> requirementVersionPath;

            if (isDataQuery) {
                Fetch<Object, Object> skillFetch = root.fetch("skill", JoinType.LEFT);
                Fetch<Object, Object> versionFetch = root.fetch("requirementVersion", JoinType.LEFT);
                root.fetch("criticalityLookup", JoinType.LEFT);
                skillPath = (Path<Object>) skillFetch;
                requirementVersionPath = (Path<Object>) versionFetch;
            } else {
                // Count query: no fetch needed, plain LEFT JOIN avoids excluding
                // rows whose optional associations are null, and keeps join
                // semantics consistent with the data query above.
                skillPath = root.join("skill", JoinType.LEFT);
                requirementVersionPath = root.join("requirementVersion", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (requirementVersionId != null) {
                predicates.add(cb.equal(requirementVersionPath.get("id"), requirementVersionId));
            }

            if (applicationPublicId != null && !applicationPublicId.isBlank()) {
                predicates.add(cb.equal(
                        requirementVersionPath.get("application").get("publicId"), applicationPublicId));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(skillPath.get("skillCode")), pattern),
                        cb.like(cb.lower(skillPath.get("skillName")), pattern)
                ));
            }

            if (skillGroup != null && !skillGroup.isBlank()) {
                predicates.add(cb.and(
                        cb.isNotNull(skillPath.get("skillScopeLookup")),
                        cb.equal(skillPath.get("skillScopeLookup").get("valueCode"), skillGroup)));
            }

            if (mandatoryFilter != null) {
                predicates.add(cb.equal(root.get("mandatory"), mandatoryFilter));
            }

            if (activeFilter != null) {
                predicates.add(cb.equal(root.get("active"), activeFilter));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
