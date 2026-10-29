-- ============================================================
-- V15 — Assessment Cycle Period Metadata (M4 — New Cycle UX)
-- Skill Matrix Platform
--
-- Adds structured period metadata to assessment_cycles so that
-- "Create Next Cycle" can calculate the next period deterministically
-- (Q4 2026 -> Q1 2027, December 2026 -> January 2027, H2 2026 -> H1 2027,
-- 2026 Annual -> 2027 Annual) WITHOUT parsing the free-text cycle_name.
--
-- All three columns are nullable so existing rows (created before this
-- migration) and any direct API callers remain valid. cycle_name remains
-- the required, persisted display name (generated client-side from these
-- structured values, but always sent/stored as cycleName).
--
-- No existing migration (V01-V14) is modified.
-- ============================================================

ALTER TABLE assessment_cycles
    ADD COLUMN period_type_code VARCHAR(20) NULL AFTER cycle_name,
    ADD COLUMN period_value     VARCHAR(50) NULL AFTER period_type_code,
    ADD COLUMN period_year      INT         NULL AFTER period_value;

-- V15 complete
