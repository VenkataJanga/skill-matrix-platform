-- V10: Add inserted/updated/skipped count columns to import_export_history
-- These columns align with the SMART_UPSERT SkillImportResult DTO fields.
-- Default 0 so existing rows remain valid.

ALTER TABLE import_export_history
    ADD COLUMN inserted_rows INT NOT NULL DEFAULT 0,
    ADD COLUMN updated_rows  INT NOT NULL DEFAULT 0,
    ADD COLUMN skipped_rows  INT NOT NULL DEFAULT 0;
