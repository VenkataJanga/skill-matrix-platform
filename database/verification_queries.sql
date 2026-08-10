-- ============================================================
-- Skill Matrix Platform — Database Verification Queries
-- Run after Flyway migrations to verify setup is correct
-- Usage: mysql -u skillmatrix_user -p skill_matrix_db < database/verification_queries.sql
-- ============================================================

-- ============================================================
-- FLYWAY MIGRATION HISTORY
-- ============================================================
SELECT '=== FLYWAY MIGRATION HISTORY ===' AS section;
SELECT installed_rank, version, description, type, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- ============================================================
-- 1. TABLE COUNT (expect 31 tables: 30 base + refresh_tokens)
-- ============================================================
SELECT '=== 1. TABLE COUNT ===' AS section;
SELECT COUNT(*) AS total_tables
FROM information_schema.tables
WHERE table_schema = 'skill_matrix_db'
  AND table_type = 'BASE TABLE';

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'skill_matrix_db'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- ============================================================
-- 2. ROLE COUNT (expect 3: ADMIN, LEAD_MANAGER, TECHNICIAN)
-- ============================================================
SELECT '=== 2. ROLE COUNT ===' AS section;
SELECT COUNT(*) AS total_roles FROM roles WHERE is_active = 1;
SELECT role_code, role_name, is_active FROM roles ORDER BY role_code;

-- ============================================================
-- 3. LOOKUP TYPE AND VALUE COUNT
-- ============================================================
SELECT '=== 3. LOOKUP TYPE COUNT (expect 20) ===' AS section;
SELECT COUNT(*) AS total_lookup_types FROM lookup_type WHERE is_active = 1;

SELECT '=== 3b. LOOKUP VALUE COUNTS PER TYPE ===' AS section;
SELECT lt.type_code, lt.type_name, COUNT(lv.id) AS value_count
FROM lookup_type lt
LEFT JOIN lookup_value lv ON lv.lookup_type_id = lt.id AND lv.is_active = 1
GROUP BY lt.id, lt.type_code, lt.type_name
ORDER BY lt.display_order;

SELECT '=== 3c. TOTAL LOOKUP VALUES ===' AS section;
SELECT COUNT(*) AS total_lookup_values FROM lookup_value WHERE is_active = 1;

-- ============================================================
-- 4. APPLICATION TYPE COUNT (expect 5: WEB, HOST, BATCH, API, MOBILE)
-- ============================================================
SELECT '=== 4. APPLICATION TYPE COUNT ===' AS section;
SELECT COUNT(*) AS total_application_types FROM application_types WHERE is_active = 1;
SELECT type_code, type_name, is_active FROM application_types ORDER BY type_code;

-- ============================================================
-- 5. BUNDLE COUNT (expect 3: B06, B12, B20)
-- ============================================================
SELECT '=== 5. BUNDLE COUNT ===' AS section;
SELECT COUNT(*) AS total_bundles FROM bundles WHERE is_active = 1;
SELECT bundle_code, bundle_name, is_active FROM bundles ORDER BY bundle_code;

-- ============================================================
-- 6. APPLICATION PORTFOLIO COUNT
-- ============================================================
SELECT '=== 6. APPLICATION PORTFOLIO COUNT ===' AS section;
SELECT COUNT(*) AS total_portfolios FROM application_portfolios WHERE is_active = 1;
SELECT ap.portfolio_code, ap.portfolio_name,
       a.account_code, at2.type_code, b.bundle_code,
       ap.is_active
FROM application_portfolios ap
JOIN accounts a        ON a.id = ap.account_id
JOIN application_types at2 ON at2.id = ap.application_type_id
JOIN bundles b         ON b.id = ap.bundle_id
ORDER BY ap.portfolio_code;

-- ============================================================
-- 7. APPLICATION COUNT
-- ============================================================
SELECT '=== 7. APPLICATION COUNT ===' AS section;
SELECT COUNT(*) AS total_applications FROM applications WHERE is_active = 1;
SELECT ap.portfolio_code, a.application_code, a.application_name, a.is_active
FROM applications a
JOIN application_portfolios ap ON ap.id = a.portfolio_id
ORDER BY ap.portfolio_code, a.application_code;

-- ============================================================
-- 8. SKILL CATEGORY COUNT (expect 10)
-- ============================================================
SELECT '=== 8. SKILL CATEGORY COUNT ===' AS section;
SELECT COUNT(*) AS total_skill_categories FROM skill_categories WHERE is_active = 1;
SELECT category_code, category_name, display_order, is_active
FROM skill_categories ORDER BY display_order;

-- ============================================================
-- 9. SKILL COUNT
-- ============================================================
SELECT '=== 9. SKILL COUNT ===' AS section;
SELECT COUNT(*) AS total_skills FROM skills WHERE is_active = 1;
SELECT sc.category_code, s.skill_code, s.skill_name, s.is_active
FROM skills s
JOIN skill_categories sc ON sc.id = s.skill_category_id
ORDER BY sc.category_code, s.skill_code;

-- ============================================================
-- 10. USER COUNT (expect 5 demo users in DEV)
-- ============================================================
SELECT '=== 10. USER COUNT ===' AS section;
SELECT COUNT(*) AS total_users FROM users WHERE is_active = 1;
SELECT u.username, u.full_name, u.email, r.role_code AS primary_role,
       u.employee_id, u.is_active
FROM users u
LEFT JOIN roles r ON r.id = u.primary_role_id
ORDER BY u.username;

-- ============================================================
-- 11. ASSIGNMENT COUNT (user_application_mapping)
-- ============================================================
SELECT '=== 11. ASSIGNMENT COUNT (user_application_mapping) ===' AS section;
SELECT COUNT(*) AS total_assignments FROM user_application_mapping WHERE is_active = 1;
SELECT u.username, a.application_code, uam.allocation_percentage,
       uam.effective_from, uam.is_active
FROM user_application_mapping uam
JOIN users u       ON u.id = uam.user_id
JOIN applications a ON a.id = uam.application_id
ORDER BY u.username, a.application_code;

-- ============================================================
-- 12. REQUIREMENT VERSION COUNT
-- ============================================================
SELECT '=== 12. REQUIREMENT VERSION COUNT ===' AS section;
SELECT COUNT(*) AS total_requirement_versions FROM requirement_versions WHERE is_active = 1;
SELECT a.application_code, rv.version_code, rv.version_name,
       lv.value_label AS status, rv.approved_at
FROM requirement_versions rv
JOIN applications a ON a.id = rv.application_id
LEFT JOIN lookup_value lv ON lv.id = rv.status_lookup_id
ORDER BY a.application_code, rv.version_code;

-- ============================================================
-- 13. EXPECTED RATING COUNT
-- ============================================================
SELECT '=== 13. EXPECTED RATING COUNT ===' AS section;
SELECT COUNT(*) AS total_expected_ratings FROM expected_ratings WHERE is_active = 1;
SELECT rv.version_code, s.skill_code, er.expected_level,
       crit.value_label AS criticality, er.min_people_required, er.is_mandatory
FROM expected_ratings er
JOIN requirement_versions rv ON rv.id = er.requirement_version_id
JOIN skills s ON s.id = er.skill_id
LEFT JOIN lookup_value crit ON crit.id = er.criticality_lookup_id
ORDER BY rv.version_code, s.skill_code;

-- ============================================================
-- 14. ASSESSMENT CYCLE COUNT
-- ============================================================
SELECT '=== 14. ASSESSMENT CYCLE COUNT ===' AS section;
SELECT COUNT(*) AS total_assessment_cycles FROM assessment_cycles WHERE is_active = 1;
SELECT a.application_code, ac.cycle_name,
       ac.cycle_start_date, ac.cycle_end_date,
       lv.value_label AS status
FROM assessment_cycles ac
JOIN applications a ON a.id = ac.application_id
LEFT JOIN lookup_value lv ON lv.id = ac.status_lookup_id
ORDER BY a.application_code, ac.cycle_name;

-- ============================================================
-- 15. TECHNICIAN ASSESSMENT COUNT
-- ============================================================
SELECT '=== 15. TECHNICIAN ASSESSMENT COUNT ===' AS section;
SELECT COUNT(*) AS total_technician_assessments FROM technician_assessments WHERE is_active = 1;
SELECT u.username, s.skill_code, ta.self_rating,
       lv.value_label AS status, ta.submitted_at
FROM technician_assessments ta
JOIN users u ON u.id = ta.user_id
JOIN expected_ratings er ON er.id = ta.expected_rating_id
JOIN skills s ON s.id = er.skill_id
LEFT JOIN lookup_value lv ON lv.id = ta.status_lookup_id
ORDER BY u.username, s.skill_code;

-- ============================================================
-- 16. LEAD REVIEW COUNT
-- ============================================================
SELECT '=== 16. LEAD REVIEW COUNT ===' AS section;
SELECT COUNT(*) AS total_lead_reviews FROM lead_reviews WHERE is_active = 1;
SELECT reviewer.username AS reviewer,
       tech.username AS technician,
       s.skill_code,
       lr.manager_rating, lr.final_rating,
       dec.value_label AS row_decision,
       lr.reviewed_at
FROM lead_reviews lr
JOIN technician_assessments ta ON ta.id = lr.technician_assessment_id
JOIN users tech ON tech.id = ta.user_id
JOIN users reviewer ON reviewer.id = lr.reviewer_user_id
JOIN expected_ratings er ON er.id = ta.expected_rating_id
JOIN skills s ON s.id = er.skill_id
LEFT JOIN lookup_value dec ON dec.id = lr.row_decision_lookup_id
ORDER BY tech.username, s.skill_code;

-- ============================================================
-- 17. GAP SNAPSHOT COUNT
-- ============================================================
SELECT '=== 17. GAP SNAPSHOT COUNT ===' AS section;
SELECT COUNT(*) AS total_gap_snapshots FROM skill_gap_snapshots WHERE is_active = 1;
SELECT u.username, s.skill_code, a.application_code,
       sgs.expected_level, sgs.final_approved_rating, sgs.gap_value,
       sev.value_label AS severity
FROM skill_gap_snapshots sgs
JOIN users u ON u.id = sgs.user_id
JOIN skills s ON s.id = sgs.skill_id
JOIN applications a ON a.id = sgs.application_id
LEFT JOIN lookup_value sev ON sev.id = sgs.severity_lookup_id
ORDER BY sgs.gap_value DESC, s.skill_code;

-- ============================================================
-- 18. TRAINING RECOMMENDATION COUNT
-- ============================================================
SELECT '=== 18. TRAINING RECOMMENDATION COUNT ===' AS section;
SELECT COUNT(*) AS total_training_recommendations FROM training_recommendations WHERE is_active = 1;
SELECT s.skill_code, a.application_code,
       tt.value_label AS training_type,
       pri.value_label AS priority,
       sta.value_label AS status,
       tr.target_date
FROM training_recommendations tr
JOIN skills s ON s.id = tr.skill_id
JOIN applications a ON a.id = tr.application_id
LEFT JOIN lookup_value tt  ON tt.id  = tr.training_type_lookup_id
LEFT JOIN lookup_value pri ON pri.id = tr.priority_lookup_id
LEFT JOIN lookup_value sta ON sta.id = tr.status_lookup_id
ORDER BY s.skill_code;

-- ============================================================
-- 19. AUDIT LOG COUNT
-- ============================================================
SELECT '=== 19. AUDIT LOG COUNT ===' AS section;
SELECT COUNT(*) AS total_audit_log_entries FROM audit_log;
SELECT action, entity_type, actor_username, occurred_at
FROM audit_log
ORDER BY occurred_at
LIMIT 20;

-- ============================================================
-- 20. NOTIFICATION LOG COUNT
-- ============================================================
SELECT '=== 20. NOTIFICATION LOG COUNT ===' AS section;
SELECT COUNT(*) AS total_notification_log_entries FROM notification_log;
SELECT u.username AS recipient, nl.event_type,
       ch.value_label AS channel,
       st.value_label AS status,
       nl.subject, nl.sent_at
FROM notification_log nl
JOIN users u ON u.id = nl.recipient_user_id
LEFT JOIN lookup_value ch ON ch.id = nl.channel_lookup_id
LEFT JOIN lookup_value st ON st.id = nl.status_lookup_id
ORDER BY nl.created_at;

-- ============================================================
-- 21. IMPORT/EXPORT HISTORY COUNT
-- ============================================================
SELECT '=== 21. IMPORT/EXPORT HISTORY COUNT ===' AS section;
SELECT COUNT(*) AS total_import_export_records FROM import_export_history;
SELECT operation_type, template_type, file_name,
       total_rows, success_rows, failed_rows,
       lv.value_label AS status,
       u.username AS initiated_by,
       started_at, completed_at
FROM import_export_history ieh
JOIN users u ON u.id = ieh.initiated_by
LEFT JOIN lookup_value lv ON lv.id = ieh.status_lookup_id
ORDER BY started_at;

-- ============================================================
-- PERMISSIONS AND ROLE_PERMISSIONS VERIFICATION
-- ============================================================
SELECT '=== PERMISSION COUNT (expect 26) ===' AS section;
SELECT COUNT(*) AS total_permissions FROM permissions WHERE is_active = 1;

SELECT '=== ROLE_PERMISSION COUNTS ===' AS section;
SELECT r.role_code, COUNT(rp.id) AS permission_count
FROM roles r
LEFT JOIN role_permissions rp ON rp.role_id = r.id
GROUP BY r.id, r.role_code
ORDER BY r.role_code;

-- ============================================================
-- RATING SCALE VERIFICATION
-- ============================================================
SELECT '=== RATING SCALE (expect 5 levels) ===' AS section;
SELECT level_value, level_name, level_meaning FROM rating_scale ORDER BY level_value;

-- ============================================================
-- ACCOUNTS VERIFICATION
-- ============================================================
SELECT '=== ACCOUNTS ===' AS section;
SELECT account_code, account_name, is_active FROM accounts ORDER BY account_code;

-- ============================================================
-- TEAMS VERIFICATION
-- ============================================================
SELECT '=== TEAMS ===' AS section;
SELECT t.team_code, t.team_name,
       COUNT(ut.user_id) AS member_count
FROM teams t
LEFT JOIN user_teams ut ON ut.team_id = t.id AND ut.is_active = 1
GROUP BY t.id, t.team_code, t.team_name
ORDER BY t.team_code;

-- ============================================================
-- FOREIGN KEY CONSTRAINT VERIFICATION
-- ============================================================
SELECT '=== FOREIGN KEY CONSTRAINTS ===' AS section;
SELECT TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'skill_matrix_db'
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

-- ============================================================
-- DATA INTEGRITY CHECKS
-- ============================================================
SELECT '=== DATA INTEGRITY: Orphaned lookup_value references ===' AS section;
-- Check no lookup_value references a non-existent lookup_type
SELECT COUNT(*) AS orphaned_lookup_values
FROM lookup_value lv
WHERE NOT EXISTS (SELECT 1 FROM lookup_type lt WHERE lt.id = lv.lookup_type_id);

SELECT '=== DATA INTEGRITY: Users with no roles ===' AS section;
SELECT u.username FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.is_active = 1);

SELECT '=== DATA INTEGRITY: Assessment gap check ===' AS section;
-- Verify gap_value = expected_level - final_approved_rating
SELECT sgs.id, sgs.expected_level, sgs.final_approved_rating,
       sgs.gap_value,
       (sgs.expected_level - sgs.final_approved_rating) AS computed_gap,
       CASE WHEN sgs.gap_value = (sgs.expected_level - sgs.final_approved_rating)
            THEN 'OK' ELSE 'MISMATCH' END AS gap_check
FROM skill_gap_snapshots sgs;

SELECT '=== DATA INTEGRITY: Rating scale coverage ===' AS section;
SELECT level_value FROM rating_scale
WHERE level_value NOT IN (1, 2, 3, 4, 5);

-- ============================================================
-- CHARSET VERIFICATION
-- ============================================================
SELECT '=== TABLE CHARSETS ===' AS section;
SELECT table_name, table_collation
FROM information_schema.tables
WHERE table_schema = 'skill_matrix_db' AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- ============================================================
-- END OF VERIFICATION
-- Expected results summary (DEV environment):
--   Tables:              31 (30 schema + refresh_tokens)
--   Roles:                3
--   Permissions:         26
--   Lookup types:        20
--   Lookup values:      ~57
--   Rating scale:         5 levels
--   Skill categories:    10
--   Accounts:             1 (NTT_DATA)
--   Application types:    5
--   Bundles:              3
--   Portfolios:           3 (DEV)
--   Applications:         5 (DEV)
--   Skills:               8 (DEV)
--   Users:                5 (DEV)
--   Assignments:          4 (DEV)
--   Requirement versions: 1 (DEV)
--   Expected ratings:     8 (DEV)
--   Assessment cycles:    1 (DEV)
--   Technician assess.:   8 (DEV)
--   Lead reviews:         8 (DEV)
--   Assessment approvals: 1 (DEV)
--   Gap snapshots:        4 (DEV)
--   Training recs:        4 (DEV)
--   Training participants:4 (DEV)
--   Audit log entries:    8 (DEV)
--   Notification log:     4 (DEV)
--   Import/Export hist.:  2 (DEV)
-- ============================================================
SELECT '=== VERIFICATION COMPLETE ===' AS section;




SELECT COUNT(*) AS application_table_count
FROM information_schema.tables
WHERE table_schema = 'skill_matrix_db'
  AND table_name <> 'flyway_schema_history';

SELECT installed_rank, version, description, script, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT 
    constraint_name,
    table_name,
    column_name,
    referenced_table_name,
    referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema = 'skill_matrix_db'
  AND table_name = 'refresh_tokens'
  AND referenced_table_name IS NOT NULL;
