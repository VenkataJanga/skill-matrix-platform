-- ============================================================
-- Skill Matrix Platform — Create Database
-- Run as: mysql -u root -p < database/local_setup/00_create_database.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS skill_matrix_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'skill_matrix_db';
