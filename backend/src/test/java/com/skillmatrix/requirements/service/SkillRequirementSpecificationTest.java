package com.skillmatrix.requirements.service;

import com.skillmatrix.domain.entity.*;
import com.skillmatrix.domain.repository.ApplicationPortfolioRepository;
import com.skillmatrix.domain.repository.ExpectedRatingRepository;
import com.skillmatrix.domain.repository.SkillCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regression test for the M3.1 Skill Requirement Mapping defect where
 * GET /api/v1/admin/skill-requirements returned HTTP 500 with:
 * "Requested join fetch with association [skill] with 'inner' join type,
 *  but found existing join fetch with 'left outer' join type."
 *
 * Root cause: {@code root.fetch("skill", LEFT)} registered a LEFT OUTER
 * fetch join, but subsequent predicates called {@code root.get("skill")}
 * again instead of reusing the Fetch/Join handle — causing Hibernate to
 * attempt re-resolving the same association path with default (INNER)
 * join semantics, which conflicted with the already-registered LEFT fetch.
 *
 * This is a REAL JPA/Hibernate integration test (uses an actual
 * EntityManager against the test datasource) because the defect could not
 * be reproduced with mocked repositories — it is purely a Hibernate
 * criteria-construction defect that only manifests when Spring Data JPA
 * actually translates the Specification into a query.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
class SkillRequirementSpecificationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ExpectedRatingRepository expectedRatingRepository;

    @Autowired
    private ApplicationPortfolioRepository portfolioRepository;

    @Autowired
    private SkillCategoryRepository skillCategoryRepository;

    @Test
    @DisplayName("Zero expected_ratings for a valid Requirement Version returns HTTP-200-equivalent empty page, not a Hibernate join exception")
    void build_zeroExpectedRatings_returnsEmptyPageWithoutException() {
        RequirementVersion rv = persistApplicationAndDraftVersion("VTS", "v1.0");

        var spec = SkillRequirementSpecification.build(rv.getId(), null, null, null, null, null);

        assertThatCode(() -> {
            Page<ExpectedRating> page = expectedRatingRepository.findAll(spec, PageRequest.of(0, 20));
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Search filter (triggers root.get(skill) predicate) does not conflict with the LEFT JOIN FETCH on skill")
    void build_withSearchFilter_doesNotThrowJoinFetchConflict() {
        RequirementVersion rv = persistApplicationAndDraftVersion("VTS", "v1.0");

        var spec = SkillRequirementSpecification.build(rv.getId(), null, "java", null, null, null);

        assertThatCode(() -> expectedRatingRepository.findAll(spec, PageRequest.of(0, 20)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SkillGroup filter (triggers root.get(skill) predicate) does not conflict with the LEFT JOIN FETCH on skill")
    void build_withSkillGroupFilter_doesNotThrowJoinFetchConflict() {
        RequirementVersion rv = persistApplicationAndDraftVersion("VTS", "v1.0");

        var spec = SkillRequirementSpecification.build(rv.getId(), null, null, "COMMON_TECH", null, null);

        assertThatCode(() -> expectedRatingRepository.findAll(spec, PageRequest.of(0, 20)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Existing non-empty requirement version (e.g. ATLAS-equivalent) still loads correctly after the fix")
    void build_nonEmptyVersion_returnsExpectedRating() {
        RequirementVersion rv = persistApplicationAndDraftVersion("ATLAS", "v1.0");
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Skill skill = persistSkill("TECH_JAVA_" + uniqueSuffix, "Java Programming " + uniqueSuffix);

        ExpectedRating er = ExpectedRating.builder()
                .publicId(UUID.randomUUID().toString())
                .requirementVersion(rv)
                .skill(skill)
                .expectedLevel(3)
                .mandatory(true)
                .active(true)
                .createdBy("admin")
                .updatedBy("admin")
                .build();
        em.persist(er);
        em.flush();

        var spec = SkillRequirementSpecification.build(rv.getId(), null, null, null, null, null);
        Page<ExpectedRating> page = expectedRatingRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getSkill().getSkillCode()).isEqualTo(skill.getSkillCode());
    }

    @Test
    @DisplayName("Count query path (used for pagination totals) also does not throw the join conflict")
    void build_countQuery_doesNotThrowJoinFetchConflict() {
        RequirementVersion rv = persistApplicationAndDraftVersion("VTS", "v1.0");
        var spec = SkillRequirementSpecification.build(rv.getId(), null, "search-term", "COMMON_TECH", true, true);

        // PageRequest triggers both the data query AND a count query internally.
        assertThatCode(() -> expectedRatingRepository.findAll(spec, PageRequest.of(0, 20)))
                .doesNotThrowAnyException();
    }

    // ----------------------------------------------------------------
    // Fixture helpers
    // ----------------------------------------------------------------

    private RequirementVersion persistApplicationAndDraftVersion(String appCode, String versionCode) {
        // Use unique, randomly-suffixed codes to avoid unique-constraint
        // collisions with pre-existing seed data in the shared dev database
        // this test connects to. Reuse an EXISTING, already-valid
        // ApplicationPortfolio from the seeded dev data (rather than
        // constructing one from scratch) to avoid depending on schema
        // columns (e.g. account_id) not represented in this codebase's
        // entity mapping but required by the live dev schema.
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ApplicationPortfolio portfolio = portfolioRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Test precondition failed: no ApplicationPortfolio exists in the dev database to reuse."));

        Application app = Application.builder()
                .publicId(UUID.randomUUID().toString())
                .portfolio(portfolio)
                .applicationCode(appCode + "_" + uniqueSuffix)
                .applicationName(appCode + " Application")
                .active(true)
                .createdBy("admin")
                .updatedBy("admin")
                .build();
        em.persist(app);

        RequirementVersion rv = RequirementVersion.builder()
                .publicId(UUID.randomUUID().toString())
                .application(app)
                .versionCode(versionCode)
                .versionName(appCode + " Requirements " + versionCode)
                .active(true)
                .createdBy("admin")
                .updatedBy("admin")
                .build();
        em.persist(rv);
        em.flush();
        return rv;
    }

    private Skill persistSkill(String code, String name) {
        SkillCategory category = skillCategoryRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Test precondition failed: no SkillCategory exists in the dev database to reuse."));

        Skill skill = Skill.builder()
                .publicId(UUID.randomUUID().toString())
                .skillCode(code)
                .skillName(name)
                .skillCategory(category)
                .active(true)
                .createdBy("admin")
                .updatedBy("admin")
                .build();
        em.persist(skill);
        em.flush();
        return skill;
    }
}
