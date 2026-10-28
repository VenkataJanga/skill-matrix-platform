-- ============================================================
-- V09 — Skill Master Import Hardening
-- Skill Matrix Platform
--
-- Changes:
--   1. Add scope FK columns to skills table
--      (application_type_id, bundle_id, application_id)
--   2. Add extended SKILL_SCOPE lookup values
--      (GLOBAL, APPLICATION_TYPE, BUNDLE)
--
-- DO NOT MODIFY this file after deployment.
-- ============================================================

-- ============================================================
-- 1. Add scope reference columns to skills
--    All nullable — only populated when skill_scope requires it
-- ============================================================

ALTER TABLE skills
    ADD COLUMN application_type_id BIGINT NULL
        AFTER skill_scope_lookup_id,
    ADD COLUMN bundle_id BIGINT NULL
        AFTER application_type_id,
    ADD COLUMN application_id BIGINT NULL
        AFTER bundle_id;

-- Foreign key constraints for the new columns
ALTER TABLE skills
    ADD CONSTRAINT fk_skills_application_type
        FOREIGN KEY (application_type_id) REFERENCES application_types (id),
    ADD CONSTRAINT fk_skills_bundle
        FOREIGN KEY (bundle_id) REFERENCES bundles (id),
    ADD CONSTRAINT fk_skills_application
        FOREIGN KEY (application_id) REFERENCES applications (id);

-- ============================================================
-- 2. Add extended SKILL_SCOPE lookup values
--    V02 only had APPLICATION and GENERIC.
--    M3 business model needs:
--      GLOBAL           — common / cross-platform skill
--      APPLICATION_TYPE — scoped to WEB / HOST / BATCH etc.
--      BUNDLE           — scoped to B06 / B12 etc.
--      APPLICATION      — already exists in V02
-- ============================================================

INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'GLOBAL',           'Global (cross-platform)',       3, 1, 1, 'system'
FROM lookup_type lt WHERE lt.type_code = 'SKILL_SCOPE';

INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'APPLICATION_TYPE', 'Application Type Specific',     4, 1, 1, 'system'
FROM lookup_type lt WHERE lt.type_code = 'SKILL_SCOPE';

INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'BUNDLE',           'Bundle Specific',               5, 1, 1, 'system'
FROM lookup_type lt WHERE lt.type_code = 'SKILL_SCOPE';

-- V09 complete
