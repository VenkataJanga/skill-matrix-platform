-- ============================================================
-- Skill Matrix Platform — Drop Database (LOCAL DEV ONLY)
-- WARNING: This permanently deletes ALL data and schema.
-- NEVER run this on QA or PROD.
-- Run as: mysql -u root -p < database/local_setup/02_drop_database.sql
-- ============================================================

-- Safety check: only allow drop if running on localhost
-- (Uncomment the check below for extra protection)
-- SELECT IF(@@hostname NOT LIKE '%prod%' AND @@hostname NOT LIKE '%qa%',
--     'Safe to drop', 'BLOCKED - hostname looks like non-dev environment') AS safety_check;

DROP DATABASE IF EXISTS skill_matrix_db;

-- Optional: Drop the local dev user as well (uncomment if needed)
-- DROP USER IF EXISTS 'skillmatrix_user'@'localhost';
-- DROP USER IF EXISTS 'skillmatrix_user'@'%';

SELECT 'skill_matrix_db dropped successfully. Run 00_create_database.sql to recreate.' AS message;
