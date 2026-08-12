-- ============================================================
-- V06 — Add must_change_password to users
-- Skill Matrix Platform — Milestone 1: Auth
-- Adds flag to force password change on first login or admin reset.
-- DO NOT MODIFY this file after deployment.
-- ============================================================

ALTER TABLE users
    ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'Forces user to change password on next login'
    AFTER is_active;
