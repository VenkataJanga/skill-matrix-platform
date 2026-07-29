-- ============================================================
-- Skill Matrix Platform — Database Verification Queries
-- Run after Flyway migrations to verify setup is correct
-- Usage: mysql -u skillmatrix_user -p skill_matrix_db < database/verification_queries.sql
-- ============================================================

-- ============================================================
-- 1. FLYWAY MIGRATION HISTORY
-- ============================================================
SELECT '=== FLYWAY MIGRATION HISTORY ===' AS section;
SELECT installed_rank, version, description, type, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- ============================================================
-- 2. TABLE COUNT VERIFICATION (expect 30 tables)
-- ============================================================
SELECT '=== TABLE COUNT ===' AS section;
SELECT COUNT(*) AS total_tables FROM information_schema.tables
WHERE table_schema = 'skill_matrix_db' AND table_type = 'BASE TABLE';

-- ============================================================
-- 3. ALL TABLE NAMES
-- ============================================================
SELECT '=== ALL TABLES ===' AS section;
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = 'skill_matrix_db' AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- ============================================================
-- 4. LOOKUP TYPES (expect 17 types seeded)
-- ============================================================
SELECT '=== LOOKUP TYPES ===' AS section;
SELECT type_code, type_name, is_system, is_active FROM lookup_type ORDER BY type_code;

-- ============================================================
-- 5. LOOKUP VALUE COUNTS PER TYPE
-- ============================================================
SELECT '=== LOOKUP VALUE COUNTS ===' AS section;
SELECT lt.type_code, lt.type_name, COUNT(lv.id) AS value_count
FROM lookup_type lt
LEFT JOIN lookup_value lv ON lv.lookup_type_id = lt.id AND lv.is_active = 1
GROUP BY lt.id, lt.type_code, lt.type_name
ORDER BY lt.type_code;

-- ============================================================
-- 6. RATING SCALE (expect 5 levels)
-- ============================================================
SELECT '=== RATING SCALE ===' AS section;
SELECT level_value, level_name, level_meaning FROM rating_scale ORDER BY level_value;

-- ============================================================
-- 7. ROLES (expect ADMIN, LEAD_MANAGER, TECHNICIAN)
-- ============================================================
SELECT '=== ROLES ===' AS section;
SELECT role_code, role_name, is_active FROM roles ORDER BY role_code;

-- ============================================================
-- 8. ACCOUNTS (expect at least 1 demo account in dev)
-- ============================================================
SELECT '=== ACCOUNTS ===' AS section;
SELECT account_code, account_name, is_active FROM accounts;

-- ============================================================
-- 9. APPLICATION TYPES (expect WEB, HOST)
-- ============================================================
SELECT '=== APPLICATION TYPES ===' AS section;
SELECT type_code, type_name, is_active FROM application_types ORDER BY type_code;

-- ============================================================
-- 10. BUNDLES (expect B06, B12, B20)
-- ============================================================
SELECT '=== BUNDLES ===' AS section;
SELECT bundle_code, bundle_name, is_active FROM bundles ORDER BY bundle_code;

-- ============================================================
-- 11. APPLICATION PORTFOLIOS
-- ============================================================
SELECT '=== APPLICATION PORTFOLIOS ===' AS section;
SELECT ap.portfolio_code, ap.portfolio_name, at2.type_code, b.bundle_code, ap.is_active
FROM application_portfolios ap
JOIN application_types at2 ON at2.id = ap.application_type_id
JOIN bundles b ON b.id = ap.bundle_id
ORDER BY ap.portfolio_code;

-- ============================================================
-- 12. APPLICATIONS COUNT BY PORTFOLIO
-- ============================================================
SELECT '=== APPLICATIONS BY PORTFOLIO ===' AS section;
SELECT ap.portfolio_code, COUNT(a.id) AS app_count
FROM application_portfolios ap
LEFT JOIN applications a ON a.portfolio_id = ap.id AND a.is_active = 1
GROUP BY ap.id, ap.portfolio_code
ORDER BY ap.portfolio_code;

-- ============================================================
-- 13. SKILL CATEGORIES
-- ============================================================
SELECT '=== SKILL CATEGORIES ===' AS section;
SELECT category_code, category_name, display_order, is_active
FROM skill_categories ORDER BY display_order;

-- ============================================================
-- 14. USERS (dev demo data check)
-- ============================================================
SELECT '=== USERS ===' AS section;
SELECT u.username, u.full_name, u.email, r.role_code AS primary_role, u.is_active
FROM users u
LEFT JOIN roles r ON r.id = u.primary_role_id
ORDER BY u.username;

-- ============================================================
-- 15. FOREIGN KEY CONSTRAINT VERIFICATION
-- ============================================================
SELECT '=== FOREIGN KEYS ===' AS section;
SELECT TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'skill_matrix_db'
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

-- ============================================================
-- 16. INDEX VERIFICATION (key indexes)
-- ============================================================
SELECT '=== INDEXES ===' AS section;
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'skill_matrix_db'
  AND INDEX_NAME != 'PRIMARY'
ORDER BY TABLE_NAME, INDEX_NAME;

-- ============================================================
-- 17. UNIQUE CONSTRAINT CHECK
-- ============================================================
SELECT '=== UNIQUE CONSTRAINTS ===' AS section;
SELECT TABLE_NAME, CONSTRAINT_NAME
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'skill_matrix_db'
  AND CONSTRAINT_TYPE = 'UNIQUE'
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

-- ============================================================
-- 18. CHARSET VERIFICATION
-- ============================================================
SELECT '=== TABLE CHARSETS ===' AS section;
SELECT table_name, table_collation
FROM information_schema.tables
WHERE table_schema = 'skill_matrix_db' AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- ============================================================
-- END OF VERIFICATION
-- ============================================================
SELECT '=== VERIFICATION COMPLETE ===' AS section;
