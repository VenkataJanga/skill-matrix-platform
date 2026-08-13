-- ============================================================
-- V08 — Add unique constraint to user_application_mapping
-- Prevents concurrent duplicate active assignments for the
-- same (user_id, application_id) pair.
--
-- MySQL 8 does not support partial indexes (WHERE is_active = 1)
-- so we use a generated column trick: a nullable column
-- uam_active_guard that is user_id when is_active=1, else NULL.
-- A UNIQUE constraint on (user_id, application_id, uam_active_guard)
-- then only enforces uniqueness when is_active = 1.
-- ============================================================

-- Step 1: Add the generated guard column
ALTER TABLE user_application_mapping
    ADD COLUMN uam_active_guard BIGINT
        GENERATED ALWAYS AS (IF(is_active = 1, user_id, NULL)) VIRTUAL;

-- Step 2: Add the unique index using the guard column
CREATE UNIQUE INDEX uq_uam_active_user_app
    ON user_application_mapping (application_id, uam_active_guard);

-- V08 complete: duplicate active assignment now rejected at DB level
