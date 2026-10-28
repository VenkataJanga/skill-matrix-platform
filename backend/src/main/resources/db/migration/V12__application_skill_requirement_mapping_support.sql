-- ============================================================
-- V12 — Application Skill Requirement Mapping Support (M3.1)
-- Skill Matrix Platform
--
-- requirement_versions and expected_ratings already exist (V01)
-- and already fully support the M3.1 business model:
--   Application -> Requirement Version -> Expected Rating (skill + level + mandatory)
--
-- This migration ONLY adds optional forward-compatible columns:
--   - role_lookup_id  on expected_ratings (nullable, FK to lookup_value)
--   - grade_lookup_id on expected_ratings (nullable, FK to lookup_value)
-- These support "Role / Grade where applicable" from the M3.1 spec
-- without forcing any existing row or API caller to supply them.
--
-- No new tables are created. No existing migration is modified.
-- ============================================================

ALTER TABLE expected_ratings
    ADD COLUMN role_lookup_id  BIGINT NULL AFTER criticality_lookup_id,
    ADD COLUMN grade_lookup_id BIGINT NULL AFTER role_lookup_id;

ALTER TABLE expected_ratings
    ADD CONSTRAINT fk_er_role_lookup
        FOREIGN KEY (role_lookup_id) REFERENCES lookup_value (id),
    ADD CONSTRAINT fk_er_grade_lookup
        FOREIGN KEY (grade_lookup_id) REFERENCES lookup_value (id);

-- V12 complete
