-- V11: Support partial import mode for User Master bulk import
-- 1. Add import_mode column to import_export_history
--    Default STRICT preserves existing row behaviour.
-- 2. Add PARTIAL_SUCCESS status lookup value for IMPORT_EXPORT_STATUS.

ALTER TABLE import_export_history
    ADD COLUMN import_mode VARCHAR(20) NOT NULL DEFAULT 'STRICT';

INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label,
                          display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'PARTIAL_SUCCESS', 'Partial Success', 5, 1, 1, 'system'
FROM lookup_type lt
WHERE lt.type_code = 'IMPORT_EXPORT_STATUS';
