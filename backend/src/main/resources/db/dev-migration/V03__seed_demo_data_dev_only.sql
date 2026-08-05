-- ============================================================
-- V03 — Seed Demo Data (DEV / LOCAL ONLY)
-- Skill Matrix Platform
-- WARNING: DO NOT run in QA or PROD environments.
-- Loaded only when spring.flyway.locations includes
--   classpath:db/dev-migration  (application-dev.yml only)
--
-- Demo password for all users: Password@123
-- BCrypt hash (cost 12): $2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lLqy
-- ============================================================

-- ============================================================
-- 1. APPLICATION PORTFOLIOS
--    NTT_DATA account + WEB type + B06 bundle = B06-WEB
--    NTT_DATA account + HOST type + B06 bundle = B06-HOST
-- ============================================================
INSERT INTO application_portfolios
    (public_id, account_id, application_type_id, bundle_id, portfolio_code, portfolio_name, description, is_active, created_by)
SELECT UUID(), a.id, at2.id, b.id,
       'B06-WEB', 'Bundle 06 Web Portfolio',
       'Web applications in Bundle 06 for NTT DATA Germany', 1, 'system'
FROM accounts a
JOIN application_types at2 ON at2.type_code = 'WEB'
JOIN bundles b              ON b.bundle_code  = 'B06'
WHERE a.account_code = 'NTT_DATA';

INSERT INTO application_portfolios
    (public_id, account_id, application_type_id, bundle_id, portfolio_code, portfolio_name, description, is_active, created_by)
SELECT UUID(), a.id, at2.id, b.id,
       'B06-HOST', 'Bundle 06 Host Portfolio',
       'Host applications in Bundle 06 for NTT DATA Germany', 1, 'system'
FROM accounts a
JOIN application_types at2 ON at2.type_code = 'HOST'
JOIN bundles b              ON b.bundle_code  = 'B06'
WHERE a.account_code = 'NTT_DATA';

INSERT INTO application_portfolios
    (public_id, account_id, application_type_id, bundle_id, portfolio_code, portfolio_name, description, is_active, created_by)
SELECT UUID(), a.id, at2.id, b.id,
       'B12-WEB', 'Bundle 12 Web Portfolio',
       'Web applications in Bundle 12 for NTT DATA Germany', 1, 'system'
FROM accounts a
JOIN application_types at2 ON at2.type_code = 'WEB'
JOIN bundles b              ON b.bundle_code  = 'B12'
WHERE a.account_code = 'NTT_DATA';

-- ============================================================
-- 2. APPLICATIONS
--    ATLAS-deZentral is the primary pilot application (B06-WEB)
-- ============================================================
INSERT INTO applications
    (public_id, portfolio_id, application_code, application_name, description, is_active, created_by)
SELECT UUID(), ap.id, 'ATLAS', 'ATLAS-deZentral',
    'Decentralised workflow and case management system', 1, 'system'
FROM application_portfolios ap WHERE ap.portfolio_code = 'B06-WEB';

INSERT INTO applications
    (public_id, portfolio_id, application_code, application_name, description, is_active, created_by)
SELECT UUID(), ap.id, 'AVUS', 'AVUS',
    'Automated vehicle registration and processing system', 1, 'system'
FROM application_portfolios ap WHERE ap.portfolio_code = 'B06-WEB';

INSERT INTO applications
    (public_id, portfolio_id, application_code, application_name, description, is_active, created_by)
SELECT UUID(), ap.id, 'BEPPO', 'BEPPO',
    'Business process and portfolio optimisation system', 1, 'system'
FROM application_portfolios ap WHERE ap.portfolio_code = 'B06-WEB';

INSERT INTO applications
    (public_id, portfolio_id, application_code, application_name, description, is_active, created_by)
SELECT UUID(), ap.id, 'HOST_APP_01', 'Host Application 01',
    'Legacy mainframe batch processing application 01', 1, 'system'
FROM application_portfolios ap WHERE ap.portfolio_code = 'B06-HOST';

INSERT INTO applications
    (public_id, portfolio_id, application_code, application_name, description, is_active, created_by)
SELECT UUID(), ap.id, 'HOST_APP_02', 'Host Application 02',
    'Legacy mainframe batch processing application 02', 1, 'system'
FROM application_portfolios ap WHERE ap.portfolio_code = 'B06-HOST';

-- ============================================================
-- 3. DEMO USERS
--    BCrypt of 'Password@123'
--    Hash: $2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lLqy
-- ============================================================

-- Admin user (no manager)
INSERT INTO users
    (public_id, username, email, password_hash, full_name, employee_id, primary_role_id, is_active, created_by)
SELECT UUID(), 'admin', 'admin@skillmatrix.local',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lLqy',
    'System Administrator', 'EMP001', r.id, 1, 'system'
FROM roles r WHERE r.role_code = 'ADMIN';

-- Lead Manager user (manager = admin)
INSERT INTO users
    (public_id, username, email, password_hash, full_name, employee_id, manager_id, primary_role_id, is_active, created_by)
SELECT UUID(), 'lead_manager', 'leadmanager@skillmatrix.local',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lLqy',
    'Demo Lead Manager', 'EMP002',
    (SELECT id FROM users WHERE username = 'admin'),
    r.id, 1, 'system'
FROM roles r WHERE r.role_code = 'LEAD_MANAGER';

-- Technician 1 — primary ATLAS technician (manager = lead_manager)
INSERT INTO users
    (public_id, username, email, password_hash, full_name, employee_id, manager_id, primary_role_id, is_active, created_by)
SELECT UUID(), 'tech_john', 'john.smith@skillmatrix.local',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lLqy',
    'John Smith', 'EMP003',
    (SELECT id FROM users WHERE username = 'lead_manager'),
    r.id, 1, 'system'
FROM roles r WHERE r.role_code = 'TECHNICIAN';

-- Technician 2 — secondary ATLAS technician (manager = lead_manager)
INSERT INTO users
    (public_id, username, email, password_hash, full_name, employee_id, manager_id, primary_role_id, is_active, created_by)
SELECT UUID(), 'tech_jane', 'jane.doe@skillmatrix.local',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lLqy',
    'Jane Doe', 'EMP004',
    (SELECT id FROM users WHERE username = 'lead_manager'),
    r.id, 1, 'system'
FROM roles r WHERE r.role_code = 'TECHNICIAN';

-- Technician 3 — AVUS technician
INSERT INTO users
    (public_id, username, email, password_hash, full_name, employee_id, manager_id, primary_role_id, is_active, created_by)
SELECT UUID(), 'tech_mike', 'mike.brown@skillmatrix.local',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lLqy',
    'Mike Brown', 'EMP005',
    (SELECT id FROM users WHERE username = 'lead_manager'),
    r.id, 1, 'system'
FROM roles r WHERE r.role_code = 'TECHNICIAN';

-- ============================================================
-- 4. USER ROLES
-- ============================================================
INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'system' FROM users u JOIN roles r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin';

INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'system' FROM users u JOIN roles r ON r.role_code = 'LEAD_MANAGER'
WHERE u.username = 'lead_manager';

INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'system' FROM users u JOIN roles r ON r.role_code = 'TECHNICIAN'
WHERE u.username = 'tech_john';

INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'system' FROM users u JOIN roles r ON r.role_code = 'TECHNICIAN'
WHERE u.username = 'tech_jane';

INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'system' FROM users u JOIN roles r ON r.role_code = 'TECHNICIAN'
WHERE u.username = 'tech_mike';

-- ============================================================
-- 5. TEAMS
-- ============================================================
INSERT INTO teams (public_id, team_code, team_name, description, is_active, created_by) VALUES
(UUID(), 'ATLAS_TEAM',  'ATLAS Support Team',  'Team responsible for ATLAS-deZentral application support', 1, 'system'),
(UUID(), 'AVUS_TEAM',   'AVUS Support Team',   'Team responsible for AVUS application support',            1, 'system'),
(UUID(), 'HOST_TEAM',   'Host Systems Team',   'Team responsible for host and mainframe applications',     1, 'system');

-- ============================================================
-- 6. USER TEAMS
-- ============================================================
INSERT INTO user_teams (user_id, team_id)
SELECT u.id, t.id FROM users u JOIN teams t ON t.team_code = 'ATLAS_TEAM'
WHERE u.username = 'lead_manager';

INSERT INTO user_teams (user_id, team_id)
SELECT u.id, t.id FROM users u JOIN teams t ON t.team_code = 'ATLAS_TEAM'
WHERE u.username = 'tech_john';

INSERT INTO user_teams (user_id, team_id)
SELECT u.id, t.id FROM users u JOIN teams t ON t.team_code = 'ATLAS_TEAM'
WHERE u.username = 'tech_jane';

INSERT INTO user_teams (user_id, team_id)
SELECT u.id, t.id FROM users u JOIN teams t ON t.team_code = 'AVUS_TEAM'
WHERE u.username = 'tech_mike';

-- ============================================================
-- 7. SKILLS (ATLAS-deZentral specific + generic)
-- ============================================================
INSERT INTO skills
    (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'ATLAS_APP_KNOWLEDGE', 'ATLAS Application Knowledge',
    'Functional knowledge of ATLAS-deZentral modules, workflows and business processes', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'APP_KNOWLEDGE';

INSERT INTO skills
    (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'ATLAS_CONFIGURATION', 'ATLAS Configuration and Administration',
    'Ability to configure ATLAS workflows, user permissions and system parameters', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'APP_KNOWLEDGE';

INSERT INTO skills
    (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'INCIDENT_MANAGEMENT', 'Incident Management',
    'Ability to manage and resolve P1/P2 application incidents end to end', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'SUPPORT_PROCESS';

INSERT INTO skills
    (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'CHANGE_MANAGEMENT', 'Change Management Process',
    'Knowledge of ITIL change management process for deploying fixes and enhancements', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'SUPPORT_PROCESS';

INSERT INTO skills
    (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'SQL_QUERY', 'SQL Query and Database Analysis',
    'Ability to write and analyse SQL queries for troubleshooting and reporting', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'DATABASE';

INSERT INTO skills
    (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'ATLAS_MONITORING', 'ATLAS System Monitoring',
    'Ability to monitor ATLAS application health, logs and performance metrics', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'MONITORING';

INSERT INTO skills
    (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'ATLAS_DEPLOYMENT', 'ATLAS Deployment and Release',
    'Knowledge of ATLAS deployment pipeline, release process and rollback procedures', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'DEPLOYMENT';

INSERT INTO skills
    (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'JIRA_SERVICEDESK', 'JIRA Service Management',
    'Ability to use JIRA Service Management for incident and change ticket management', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'TOOLS';

-- ============================================================
-- 8. USER APPLICATION MAPPING
--    Assign technicians to ATLAS-deZentral
-- ============================================================
INSERT INTO user_application_mapping
    (public_id, user_id, application_id, allocation_percentage, effective_from, is_active, created_by)
SELECT UUID(), u.id, a.id, 100.00, '2026-01-01', 1, 'system'
FROM users u JOIN applications a ON a.application_code = 'ATLAS'
WHERE u.username = 'tech_john';

INSERT INTO user_application_mapping
    (public_id, user_id, application_id, allocation_percentage, effective_from, is_active, created_by)
SELECT UUID(), u.id, a.id, 80.00, '2026-01-01', 1, 'system'
FROM users u JOIN applications a ON a.application_code = 'ATLAS'
WHERE u.username = 'tech_jane';

INSERT INTO user_application_mapping
    (public_id, user_id, application_id, allocation_percentage, effective_from, is_active, created_by)
SELECT UUID(), u.id, a.id, 100.00, '2026-01-01', 1, 'system'
FROM users u JOIN applications a ON a.application_code = 'AVUS'
WHERE u.username = 'tech_mike';

-- lead_manager is assigned to ATLAS as reviewer
INSERT INTO user_application_mapping
    (public_id, user_id, application_id, allocation_percentage, effective_from, is_active, created_by)
SELECT UUID(), u.id, a.id, 50.00, '2026-01-01', 1, 'system'
FROM users u JOIN applications a ON a.application_code = 'ATLAS'
WHERE u.username = 'lead_manager';

-- ============================================================
-- 9. REQUIREMENT VERSIONS for ATLAS-deZentral
-- ============================================================
INSERT INTO requirement_versions
    (public_id, application_id, version_code, version_name, description,
     status_lookup_id, published_at, approved_at, approved_by, is_active, created_by)
SELECT
    UUID(),
    a.id,
    'v1.0',
    'ATLAS Skill Requirements v1.0',
    'Initial skill requirements for ATLAS-deZentral support team — Q3 2026',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'REQUIREMENT_STATUS' AND lv.value_code = 'APPROVED'),
    '2026-06-01 09:00:00',
    '2026-06-15 10:00:00',
    'admin',
    1,
    'system'
FROM applications a WHERE a.application_code = 'ATLAS';

-- ============================================================
-- 10. EXPECTED RATINGS for ATLAS v1.0
--     skill_id resolved by skill_code
--     criticality_lookup_id resolved by lookup value
-- ============================================================

-- ATLAS_APP_KNOWLEDGE: expected level 4, HIGH criticality, mandatory
INSERT INTO expected_ratings
    (public_id, requirement_version_id, skill_id, expected_level, criticality_lookup_id,
     min_people_required, is_mandatory, is_active, created_by)
SELECT UUID(), rv.id, s.id, 4,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'HIGH'),
    2, 1, 1, 'system'
FROM requirement_versions rv
JOIN applications a ON a.id = rv.application_id
JOIN skills s ON s.skill_code = 'ATLAS_APP_KNOWLEDGE'
WHERE a.application_code = 'ATLAS' AND rv.version_code = 'v1.0';

-- ATLAS_CONFIGURATION: expected level 3, HIGH criticality, mandatory
INSERT INTO expected_ratings
    (public_id, requirement_version_id, skill_id, expected_level, criticality_lookup_id,
     min_people_required, is_mandatory, is_active, created_by)
SELECT UUID(), rv.id, s.id, 3,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'HIGH'),
    1, 1, 1, 'system'
FROM requirement_versions rv
JOIN applications a ON a.id = rv.application_id
JOIN skills s ON s.skill_code = 'ATLAS_CONFIGURATION'
WHERE a.application_code = 'ATLAS' AND rv.version_code = 'v1.0';

-- INCIDENT_MANAGEMENT: expected level 4, HIGH criticality, mandatory
INSERT INTO expected_ratings
    (public_id, requirement_version_id, skill_id, expected_level, criticality_lookup_id,
     min_people_required, is_mandatory, is_active, created_by)
SELECT UUID(), rv.id, s.id, 4,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'HIGH'),
    2, 1, 1, 'system'
FROM requirement_versions rv
JOIN applications a ON a.id = rv.application_id
JOIN skills s ON s.skill_code = 'INCIDENT_MANAGEMENT'
WHERE a.application_code = 'ATLAS' AND rv.version_code = 'v1.0';

-- CHANGE_MANAGEMENT: expected level 3, MEDIUM criticality, mandatory
INSERT INTO expected_ratings
    (public_id, requirement_version_id, skill_id, expected_level, criticality_lookup_id,
     min_people_required, is_mandatory, is_active, created_by)
SELECT UUID(), rv.id, s.id, 3,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'MEDIUM'),
    1, 1, 1, 'system'
FROM requirement_versions rv
JOIN applications a ON a.id = rv.application_id
JOIN skills s ON s.skill_code = 'CHANGE_MANAGEMENT'
WHERE a.application_code = 'ATLAS' AND rv.version_code = 'v1.0';

-- SQL_QUERY: expected level 3, MEDIUM criticality, mandatory
INSERT INTO expected_ratings
    (public_id, requirement_version_id, skill_id, expected_level, criticality_lookup_id,
     min_people_required, is_mandatory, is_active, created_by)
SELECT UUID(), rv.id, s.id, 3,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'MEDIUM'),
    1, 1, 1, 'system'
FROM requirement_versions rv
JOIN applications a ON a.id = rv.application_id
JOIN skills s ON s.skill_code = 'SQL_QUERY'
WHERE a.application_code = 'ATLAS' AND rv.version_code = 'v1.0';

-- ATLAS_MONITORING: expected level 3, HIGH criticality, mandatory
INSERT INTO expected_ratings
    (public_id, requirement_version_id, skill_id, expected_level, criticality_lookup_id,
     min_people_required, is_mandatory, is_active, created_by)
SELECT UUID(), rv.id, s.id, 3,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'HIGH'),
    1, 1, 1, 'system'
FROM requirement_versions rv
JOIN applications a ON a.id = rv.application_id
JOIN skills s ON s.skill_code = 'ATLAS_MONITORING'
WHERE a.application_code = 'ATLAS' AND rv.version_code = 'v1.0';

-- ATLAS_DEPLOYMENT: expected level 2, LOW criticality, not mandatory
INSERT INTO expected_ratings
    (public_id, requirement_version_id, skill_id, expected_level, criticality_lookup_id,
     min_people_required, is_mandatory, is_active, created_by)
SELECT UUID(), rv.id, s.id, 2,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'LOW'),
    1, 0, 1, 'system'
FROM requirement_versions rv
JOIN applications a ON a.id = rv.application_id
JOIN skills s ON s.skill_code = 'ATLAS_DEPLOYMENT'
WHERE a.application_code = 'ATLAS' AND rv.version_code = 'v1.0';

-- JIRA_SERVICEDESK: expected level 3, MEDIUM criticality, mandatory
INSERT INTO expected_ratings
    (public_id, requirement_version_id, skill_id, expected_level, criticality_lookup_id,
     min_people_required, is_mandatory, is_active, created_by)
SELECT UUID(), rv.id, s.id, 3,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'MEDIUM'),
    1, 1, 1, 'system'
FROM requirement_versions rv
JOIN applications a ON a.id = rv.application_id
JOIN skills s ON s.skill_code = 'JIRA_SERVICEDESK'
WHERE a.application_code = 'ATLAS' AND rv.version_code = 'v1.0';

-- ============================================================
-- 11. ASSESSMENT CYCLE — ATLAS Q3 2026
-- ============================================================
INSERT INTO assessment_cycles
    (public_id, application_id, requirement_version_id, cycle_name,
     cycle_start_date, cycle_end_date, status_lookup_id, is_active, created_by)
SELECT
    UUID(), a.id, rv.id,
    'ATLAS Q3 2026 Assessment',
    '2026-07-01', '2026-08-31',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'ASSESSMENT_CYCLE_STATUS' AND lv.value_code = 'OPEN'),
    1, 'system'
FROM applications a
JOIN requirement_versions rv ON rv.application_id = a.id AND rv.version_code = 'v1.0'
WHERE a.application_code = 'ATLAS';

-- ============================================================
-- 12. TECHNICIAN ASSESSMENTS — tech_john self-ratings
--     Self-ratings for each expected_rating in the ATLAS cycle
-- ============================================================

-- ATLAS_APP_KNOWLEDGE: self_rating=3 (gap=1), submitted
INSERT INTO technician_assessments
    (public_id, cycle_id, user_id, expected_rating_id, self_rating,
     evidence, technician_comment, status_lookup_id, submitted_at, is_active, created_by)
SELECT UUID(), ac.id, u.id, er.id, 3,
    'Handled 25+ ATLAS incidents in Q2 2026. Familiar with main workflow modules.',
    'Strong on standard workflows; need more exposure to admin modules.',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'ASSESSMENT_STATUS' AND lv.value_code = 'SUBMITTED'),
    '2026-07-15 14:30:00', 1, 'system'
FROM assessment_cycles ac
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.username = 'tech_john'
JOIN expected_ratings er ON er.requirement_version_id = ac.requirement_version_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'ATLAS_APP_KNOWLEDGE';

-- ATLAS_CONFIGURATION: self_rating=2 (gap=1), submitted
INSERT INTO technician_assessments
    (public_id, cycle_id, user_id, expected_rating_id, self_rating,
     evidence, technician_comment, status_lookup_id, submitted_at, is_active, created_by)
SELECT UUID(), ac.id, u.id, er.id, 2,
    'Have done basic configuration changes under supervision.',
    'Need training on advanced configuration and permission management.',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'ASSESSMENT_STATUS' AND lv.value_code = 'SUBMITTED'),
    '2026-07-15 14:35:00', 1, 'system'
FROM assessment_cycles ac
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.username = 'tech_john'
JOIN expected_ratings er ON er.requirement_version_id = ac.requirement_version_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'ATLAS_CONFIGURATION';

-- INCIDENT_MANAGEMENT: self_rating=4 (gap=0), submitted
INSERT INTO technician_assessments
    (public_id, cycle_id, user_id, expected_rating_id, self_rating,
     evidence, technician_comment, status_lookup_id, submitted_at, is_active, created_by)
SELECT UUID(), ac.id, u.id, er.id, 4,
    'JIRA tickets ATLAS-1234, ATLAS-1567, ATLAS-1892 — all P1 resolved within SLA.',
    'Confident in incident management. Can guide juniors.',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'ASSESSMENT_STATUS' AND lv.value_code = 'SUBMITTED'),
    '2026-07-15 14:40:00', 1, 'system'
FROM assessment_cycles ac
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.username = 'tech_john'
JOIN expected_ratings er ON er.requirement_version_id = ac.requirement_version_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'INCIDENT_MANAGEMENT';

-- CHANGE_MANAGEMENT: self_rating=3 (gap=0), submitted
INSERT INTO technician_assessments
    (public_id, cycle_id, user_id, expected_rating_id, self_rating,
     evidence, technician_comment, status_lookup_id, submitted_at, is_active, created_by)
SELECT UUID(), ac.id, u.id, er.id, 3,
    'Completed 5 ATLAS change requests in Q2 2026 without issues.',
    'Comfortable with standard changes. Working on emergency change procedures.',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'ASSESSMENT_STATUS' AND lv.value_code = 'SUBMITTED'),
    '2026-07-15 14:45:00', 1, 'system'
FROM assessment_cycles ac
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.username = 'tech_john'
JOIN expected_ratings er ON er.requirement_version_id = ac.requirement_version_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'CHANGE_MANAGEMENT';

-- SQL_QUERY: self_rating=2 (gap=1), submitted
INSERT INTO technician_assessments
    (public_id, cycle_id, user_id, expected_rating_id, self_rating,
     evidence, technician_comment, status_lookup_id, submitted_at, is_active, created_by)
SELECT UUID(), ac.id, u.id, er.id, 2,
    'Can write basic SELECT queries and JOINs.',
    'Need training on complex SQL and query optimisation.',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'ASSESSMENT_STATUS' AND lv.value_code = 'SUBMITTED'),
    '2026-07-15 14:50:00', 1, 'system'
FROM assessment_cycles ac
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.username = 'tech_john'
JOIN expected_ratings er ON er.requirement_version_id = ac.requirement_version_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'SQL_QUERY';

-- ATLAS_MONITORING: self_rating=3 (gap=0), submitted
INSERT INTO technician_assessments
    (public_id, cycle_id, user_id, expected_rating_id, self_rating,
     evidence, technician_comment, status_lookup_id, submitted_at, is_active, created_by)
SELECT UUID(), ac.id, u.id, er.id, 3,
    'Regularly monitor ATLAS dashboards and alert thresholds.',
    'Confident in daily monitoring. Working on advanced alerting rules.',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'ASSESSMENT_STATUS' AND lv.value_code = 'SUBMITTED'),
    '2026-07-15 14:55:00', 1, 'system'
FROM assessment_cycles ac
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.username = 'tech_john'
JOIN expected_ratings er ON er.requirement_version_id = ac.requirement_version_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'ATLAS_MONITORING';

-- ATLAS_DEPLOYMENT: self_rating=1 (gap=1), submitted
INSERT INTO technician_assessments
    (public_id, cycle_id, user_id, expected_rating_id, self_rating,
     evidence, technician_comment, status_lookup_id, submitted_at, is_active, created_by)
SELECT UUID(), ac.id, u.id, er.id, 1,
    'Only observed deployments, not performed independently.',
    'Need hands-on deployment training.',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'ASSESSMENT_STATUS' AND lv.value_code = 'SUBMITTED'),
    '2026-07-15 15:00:00', 1, 'system'
FROM assessment_cycles ac
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.username = 'tech_john'
JOIN expected_ratings er ON er.requirement_version_id = ac.requirement_version_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'ATLAS_DEPLOYMENT';

-- JIRA_SERVICEDESK: self_rating=3 (gap=0), submitted
INSERT INTO technician_assessments
    (public_id, cycle_id, user_id, expected_rating_id, self_rating,
     evidence, technician_comment, status_lookup_id, submitted_at, is_active, created_by)
SELECT UUID(), ac.id, u.id, er.id, 3,
    'Use JIRA daily for incident and change tracking.',
    'Proficient in JIRA. Exploring automation rules.',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'ASSESSMENT_STATUS' AND lv.value_code = 'SUBMITTED'),
    '2026-07-15 15:05:00', 1, 'system'
FROM assessment_cycles ac
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.username = 'tech_john'
JOIN expected_ratings er ON er.requirement_version_id = ac.requirement_version_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'JIRA_SERVICEDESK';

-- ============================================================
-- 13. LEAD REVIEWS — lead_manager reviews tech_john assessments
-- ============================================================

-- ATLAS_APP_KNOWLEDGE: manager_rating=3, final_rating=3, GAP_IDENTIFIED
INSERT INTO lead_reviews
    (public_id, technician_assessment_id, reviewer_user_id,
     manager_rating, final_rating, row_decision_lookup_id, manager_comment, reviewed_at, is_active, created_by)
SELECT UUID(), ta.id,
    (SELECT id FROM users WHERE username = 'lead_manager'),
    3, 3,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'REVIEW_ROW_DECISION' AND lv.value_code = 'GAP_IDENTIFIED'),
    'Agreed rating 3. Gap of 1 against expected level 4. Training recommended.',
    '2026-07-25 10:00:00', 1, 'system'
FROM technician_assessments ta
JOIN expected_ratings er ON er.id = ta.expected_rating_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'ATLAS_APP_KNOWLEDGE'
JOIN users u ON u.id = ta.user_id AND u.username = 'tech_john';

-- ATLAS_CONFIGURATION: manager_rating=2, final_rating=2, GAP_IDENTIFIED
INSERT INTO lead_reviews
    (public_id, technician_assessment_id, reviewer_user_id,
     manager_rating, final_rating, row_decision_lookup_id, manager_comment, reviewed_at, is_active, created_by)
SELECT UUID(), ta.id,
    (SELECT id FROM users WHERE username = 'lead_manager'),
    2, 2,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'REVIEW_ROW_DECISION' AND lv.value_code = 'GAP_IDENTIFIED'),
    'Confirmed rating 2. Significant gap against expected level 3. Priority training needed.',
    '2026-07-25 10:10:00', 1, 'system'
FROM technician_assessments ta
JOIN expected_ratings er ON er.id = ta.expected_rating_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'ATLAS_CONFIGURATION'
JOIN users u ON u.id = ta.user_id AND u.username = 'tech_john';

-- INCIDENT_MANAGEMENT: manager_rating=4, final_rating=4, APPROVED
INSERT INTO lead_reviews
    (public_id, technician_assessment_id, reviewer_user_id,
     manager_rating, final_rating, row_decision_lookup_id, manager_comment, reviewed_at, is_active, created_by)
SELECT UUID(), ta.id,
    (SELECT id FROM users WHERE username = 'lead_manager'),
    4, 4,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'REVIEW_ROW_DECISION' AND lv.value_code = 'APPROVED'),
    'Excellent incident management skills. Meets expected level.',
    '2026-07-25 10:15:00', 1, 'system'
FROM technician_assessments ta
JOIN expected_ratings er ON er.id = ta.expected_rating_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'INCIDENT_MANAGEMENT'
JOIN users u ON u.id = ta.user_id AND u.username = 'tech_john';

-- CHANGE_MANAGEMENT: manager_rating=3, final_rating=3, APPROVED
INSERT INTO lead_reviews
    (public_id, technician_assessment_id, reviewer_user_id,
     manager_rating, final_rating, row_decision_lookup_id, manager_comment, reviewed_at, is_active, created_by)
SELECT UUID(), ta.id,
    (SELECT id FROM users WHERE username = 'lead_manager'),
    3, 3,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'REVIEW_ROW_DECISION' AND lv.value_code = 'APPROVED'),
    'Meets expected level. No gap identified.',
    '2026-07-25 10:20:00', 1, 'system'
FROM technician_assessments ta
JOIN expected_ratings er ON er.id = ta.expected_rating_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'CHANGE_MANAGEMENT'
JOIN users u ON u.id = ta.user_id AND u.username = 'tech_john';

-- SQL_QUERY: manager_rating=2, final_rating=2, GAP_IDENTIFIED
INSERT INTO lead_reviews
    (public_id, technician_assessment_id, reviewer_user_id,
     manager_rating, final_rating, row_decision_lookup_id, manager_comment, reviewed_at, is_active, created_by)
SELECT UUID(), ta.id,
    (SELECT id FROM users WHERE username = 'lead_manager'),
    2, 2,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'REVIEW_ROW_DECISION' AND lv.value_code = 'GAP_IDENTIFIED'),
    'Gap of 1 vs expected level 3. SQL training recommended.',
    '2026-07-25 10:25:00', 1, 'system'
FROM technician_assessments ta
JOIN expected_ratings er ON er.id = ta.expected_rating_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'SQL_QUERY'
JOIN users u ON u.id = ta.user_id AND u.username = 'tech_john';

-- ATLAS_MONITORING: manager_rating=3, final_rating=3, APPROVED
INSERT INTO lead_reviews
    (public_id, technician_assessment_id, reviewer_user_id,
     manager_rating, final_rating, row_decision_lookup_id, manager_comment, reviewed_at, is_active, created_by)
SELECT UUID(), ta.id,
    (SELECT id FROM users WHERE username = 'lead_manager'),
    3, 3,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'REVIEW_ROW_DECISION' AND lv.value_code = 'APPROVED'),
    'Meets expected level for monitoring.',
    '2026-07-25 10:30:00', 1, 'system'
FROM technician_assessments ta
JOIN expected_ratings er ON er.id = ta.expected_rating_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'ATLAS_MONITORING'
JOIN users u ON u.id = ta.user_id AND u.username = 'tech_john';

-- ATLAS_DEPLOYMENT: manager_rating=1, final_rating=1, GAP_IDENTIFIED
INSERT INTO lead_reviews
    (public_id, technician_assessment_id, reviewer_user_id,
     manager_rating, final_rating, row_decision_lookup_id, manager_comment, reviewed_at, is_active, created_by)
SELECT UUID(), ta.id,
    (SELECT id FROM users WHERE username = 'lead_manager'),
    1, 1,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'REVIEW_ROW_DECISION' AND lv.value_code = 'GAP_IDENTIFIED'),
    'Gap of 1 vs expected level 2. On-the-job deployment training scheduled.',
    '2026-07-25 10:35:00', 1, 'system'
FROM technician_assessments ta
JOIN expected_ratings er ON er.id = ta.expected_rating_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'ATLAS_DEPLOYMENT'
JOIN users u ON u.id = ta.user_id AND u.username = 'tech_john';

-- JIRA_SERVICEDESK: manager_rating=3, final_rating=3, APPROVED
INSERT INTO lead_reviews
    (public_id, technician_assessment_id, reviewer_user_id,
     manager_rating, final_rating, row_decision_lookup_id, manager_comment, reviewed_at, is_active, created_by)
SELECT UUID(), ta.id,
    (SELECT id FROM users WHERE username = 'lead_manager'),
    3, 3,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'REVIEW_ROW_DECISION' AND lv.value_code = 'APPROVED'),
    'Meets expected JIRA level.',
    '2026-07-25 10:40:00', 1, 'system'
FROM technician_assessments ta
JOIN expected_ratings er ON er.id = ta.expected_rating_id
JOIN skills s ON s.id = er.skill_id AND s.skill_code = 'JIRA_SERVICEDESK'
JOIN users u ON u.id = ta.user_id AND u.username = 'tech_john';

-- ============================================================
-- 14. ASSESSMENT APPROVAL — overall APPROVED for tech_john
-- ============================================================
INSERT INTO assessment_approvals
    (public_id, cycle_id, technician_user_id, reviewer_user_id,
     overall_decision_lookup_id, decision_note, approved_at, is_active, created_by)
SELECT UUID(),
    ac.id,
    (SELECT id FROM users WHERE username = 'tech_john'),
    (SELECT id FROM users WHERE username = 'lead_manager'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'OVERALL_DECISION' AND lv.value_code = 'APPROVED'),
    'Overall assessment approved. 4 skill gaps identified. Training plan initiated.',
    '2026-07-30 11:00:00',
    1, 'system'
FROM assessment_cycles ac
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
WHERE ac.cycle_name = 'ATLAS Q3 2026 Assessment';

-- ============================================================
-- 15. SKILL GAP SNAPSHOTS — gaps for tech_john
--     Only skills with gap_value > 0 require snapshots
-- ============================================================

-- ATLAS_APP_KNOWLEDGE: expected=4, final_approved=3, gap=1, MEDIUM severity
INSERT INTO skill_gap_snapshots
    (public_id, assessment_approval_id, user_id, skill_id, application_id, cycle_id,
     expected_level, final_approved_rating, gap_value, severity_lookup_id, criticality_lookup_id,
     snapshot_date, is_active)
SELECT UUID(), aa.id, u.id, s.id, a.id, ac.id,
    4, 3, 1,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'GAP_SEVERITY' AND lv.value_code = 'MEDIUM'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'HIGH'),
    '2026-07-30 11:00:00', 1
FROM assessment_approvals aa
JOIN assessment_cycles ac ON ac.id = aa.cycle_id
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.id = aa.technician_user_id AND u.username = 'tech_john'
JOIN skills s ON s.skill_code = 'ATLAS_APP_KNOWLEDGE';

-- ATLAS_CONFIGURATION: expected=3, final_approved=2, gap=1, MEDIUM severity
INSERT INTO skill_gap_snapshots
    (public_id, assessment_approval_id, user_id, skill_id, application_id, cycle_id,
     expected_level, final_approved_rating, gap_value, severity_lookup_id, criticality_lookup_id,
     snapshot_date, is_active)
SELECT UUID(), aa.id, u.id, s.id, a.id, ac.id,
    3, 2, 1,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'GAP_SEVERITY' AND lv.value_code = 'MEDIUM'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'HIGH'),
    '2026-07-30 11:00:00', 1
FROM assessment_approvals aa
JOIN assessment_cycles ac ON ac.id = aa.cycle_id
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.id = aa.technician_user_id AND u.username = 'tech_john'
JOIN skills s ON s.skill_code = 'ATLAS_CONFIGURATION';

-- SQL_QUERY: expected=3, final_approved=2, gap=1, MEDIUM severity
INSERT INTO skill_gap_snapshots
    (public_id, assessment_approval_id, user_id, skill_id, application_id, cycle_id,
     expected_level, final_approved_rating, gap_value, severity_lookup_id, criticality_lookup_id,
     snapshot_date, is_active)
SELECT UUID(), aa.id, u.id, s.id, a.id, ac.id,
    3, 2, 1,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'GAP_SEVERITY' AND lv.value_code = 'MEDIUM'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'MEDIUM'),
    '2026-07-30 11:00:00', 1
FROM assessment_approvals aa
JOIN assessment_cycles ac ON ac.id = aa.cycle_id
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.id = aa.technician_user_id AND u.username = 'tech_john'
JOIN skills s ON s.skill_code = 'SQL_QUERY';

-- ATLAS_DEPLOYMENT: expected=2, final_approved=1, gap=1, MEDIUM severity
INSERT INTO skill_gap_snapshots
    (public_id, assessment_approval_id, user_id, skill_id, application_id, cycle_id,
     expected_level, final_approved_rating, gap_value, severity_lookup_id, criticality_lookup_id,
     snapshot_date, is_active)
SELECT UUID(), aa.id, u.id, s.id, a.id, ac.id,
    2, 1, 1,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'GAP_SEVERITY' AND lv.value_code = 'MEDIUM'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'CRITICALITY' AND lv.value_code = 'LOW'),
    '2026-07-30 11:00:00', 1
FROM assessment_approvals aa
JOIN assessment_cycles ac ON ac.id = aa.cycle_id
JOIN applications a ON a.id = ac.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.id = aa.technician_user_id AND u.username = 'tech_john'
JOIN skills s ON s.skill_code = 'ATLAS_DEPLOYMENT';

-- ============================================================
-- 16. TRAINING RECOMMENDATIONS — one per gap
-- ============================================================

-- ATLAS_APP_KNOWLEDGE gap: E-Learning, HIGH priority
INSERT INTO training_recommendations
    (public_id, gap_snapshot_id, skill_id, application_id,
     training_type_lookup_id, priority_lookup_id, target_date, status_lookup_id,
     notes, is_active, created_by)
SELECT UUID(), sgs.id, s.id, a.id,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_TYPE' AND lv.value_code = 'E_LEARNING'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_PRIORITY' AND lv.value_code = 'HIGH'),
    '2026-08-31',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_STATUS' AND lv.value_code = 'CONFIRMED'),
    'Complete ATLAS advanced modules e-learning course on internal LMS.',
    1, 'system'
FROM skill_gap_snapshots sgs
JOIN skills s ON s.id = sgs.skill_id AND s.skill_code = 'ATLAS_APP_KNOWLEDGE'
JOIN applications a ON a.id = sgs.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.id = sgs.user_id AND u.username = 'tech_john';

-- ATLAS_CONFIGURATION gap: Mentoring, HIGH priority
INSERT INTO training_recommendations
    (public_id, gap_snapshot_id, skill_id, application_id,
     training_type_lookup_id, priority_lookup_id, target_date, status_lookup_id,
     notes, is_active, created_by)
SELECT UUID(), sgs.id, s.id, a.id,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_TYPE' AND lv.value_code = 'MENTORING'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_PRIORITY' AND lv.value_code = 'HIGH'),
    '2026-09-30',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_STATUS' AND lv.value_code = 'RECOMMENDED'),
    'Shadow lead_manager during 3 ATLAS configuration sessions.',
    1, 'system'
FROM skill_gap_snapshots sgs
JOIN skills s ON s.id = sgs.skill_id AND s.skill_code = 'ATLAS_CONFIGURATION'
JOIN applications a ON a.id = sgs.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.id = sgs.user_id AND u.username = 'tech_john';

-- SQL_QUERY gap: Classroom, MEDIUM priority
INSERT INTO training_recommendations
    (public_id, gap_snapshot_id, skill_id, application_id,
     training_type_lookup_id, priority_lookup_id, target_date, status_lookup_id,
     notes, is_active, created_by)
SELECT UUID(), sgs.id, s.id, a.id,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_TYPE' AND lv.value_code = 'CLASSROOM'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_PRIORITY' AND lv.value_code = 'MEDIUM'),
    '2026-10-31',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_STATUS' AND lv.value_code = 'RECOMMENDED'),
    'Enroll in internal SQL intermediate classroom course.',
    1, 'system'
FROM skill_gap_snapshots sgs
JOIN skills s ON s.id = sgs.skill_id AND s.skill_code = 'SQL_QUERY'
JOIN applications a ON a.id = sgs.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.id = sgs.user_id AND u.username = 'tech_john';

-- ATLAS_DEPLOYMENT gap: On the Job Training, MEDIUM priority
INSERT INTO training_recommendations
    (public_id, gap_snapshot_id, skill_id, application_id,
     training_type_lookup_id, priority_lookup_id, target_date, status_lookup_id,
     notes, is_active, created_by)
SELECT UUID(), sgs.id, s.id, a.id,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_TYPE' AND lv.value_code = 'ON_THE_JOB'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_PRIORITY' AND lv.value_code = 'MEDIUM'),
    '2026-10-31',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'TRAINING_STATUS' AND lv.value_code = 'RECOMMENDED'),
    'Participate as co-deployer in next 2 ATLAS deployment windows.',
    1, 'system'
FROM skill_gap_snapshots sgs
JOIN skills s ON s.id = sgs.skill_id AND s.skill_code = 'ATLAS_DEPLOYMENT'
JOIN applications a ON a.id = sgs.application_id AND a.application_code = 'ATLAS'
JOIN users u ON u.id = sgs.user_id AND u.username = 'tech_john';

-- ============================================================
-- 17. TRAINING PARTICIPANTS
-- ============================================================
INSERT INTO training_participants (public_id, training_recommendation_id, user_id, enrolled_at, is_active)
SELECT UUID(), tr.id, u.id, '2026-08-01 09:00:00', 1
FROM training_recommendations tr
JOIN skills s ON s.id = tr.skill_id AND s.skill_code = 'ATLAS_APP_KNOWLEDGE'
JOIN users u ON u.username = 'tech_john';

INSERT INTO training_participants (public_id, training_recommendation_id, user_id, enrolled_at, is_active)
SELECT UUID(), tr.id, u.id, '2026-08-01 09:00:00', 1
FROM training_recommendations tr
JOIN skills s ON s.id = tr.skill_id AND s.skill_code = 'ATLAS_CONFIGURATION'
JOIN users u ON u.username = 'tech_john';

INSERT INTO training_participants (public_id, training_recommendation_id, user_id, enrolled_at, is_active)
SELECT UUID(), tr.id, u.id, '2026-08-15 09:00:00', 1
FROM training_recommendations tr
JOIN skills s ON s.id = tr.skill_id AND s.skill_code = 'SQL_QUERY'
JOIN users u ON u.username = 'tech_john';

INSERT INTO training_participants (public_id, training_recommendation_id, user_id, enrolled_at, is_active)
SELECT UUID(), tr.id, u.id, '2026-09-01 09:00:00', 1
FROM training_recommendations tr
JOIN skills s ON s.id = tr.skill_id AND s.skill_code = 'ATLAS_DEPLOYMENT'
JOIN users u ON u.username = 'tech_john';

-- ============================================================
-- 18. AUDIT LOG — sample system events
-- ============================================================
INSERT INTO audit_log
    (public_id, actor_username, action, entity_type, entity_id, new_value, ip_address, occurred_at)
VALUES
(UUID(), 'system',       'CREATE',   'ASSESSMENT_CYCLE',    '1', '{"cycle_name":"ATLAS Q3 2026 Assessment","status":"OPEN"}',          '127.0.0.1', '2026-07-01 08:00:00'),
(UUID(), 'tech_john',    'SUBMIT',   'TECHNICIAN_ASSESSMENT','1', '{"self_rating":3,"skill":"ATLAS_APP_KNOWLEDGE","status":"SUBMITTED"}','10.0.1.15',  '2026-07-15 14:30:00'),
(UUID(), 'tech_john',    'SUBMIT',   'TECHNICIAN_ASSESSMENT','2', '{"self_rating":4,"skill":"INCIDENT_MANAGEMENT","status":"SUBMITTED"}','10.0.1.15',  '2026-07-15 14:40:00'),
(UUID(), 'lead_manager', 'REVIEW',   'LEAD_REVIEW',         '1', '{"final_rating":3,"decision":"GAP_IDENTIFIED","skill":"ATLAS_APP_KNOWLEDGE"}','10.0.1.20','2026-07-25 10:00:00'),
(UUID(), 'lead_manager', 'APPROVE',  'ASSESSMENT_APPROVAL', '1', '{"decision":"APPROVED","technician":"tech_john"}',                  '10.0.1.20', '2026-07-30 11:00:00'),
(UUID(), 'system',       'GENERATE', 'SKILL_GAP_SNAPSHOT',  '1', '{"gaps_found":4,"user":"tech_john","application":"ATLAS"}',         '127.0.0.1', '2026-07-30 11:01:00'),
(UUID(), 'lead_manager', 'CREATE',   'TRAINING_RECOMMENDATION','1','{"skill":"ATLAS_APP_KNOWLEDGE","type":"E_LEARNING","priority":"HIGH"}','10.0.1.20','2026-07-31 09:00:00'),
(UUID(), 'admin',        'LOGIN',    'USER',                'EMP001', '{"username":"admin","result":"SUCCESS"}',                       '10.0.0.1',  '2026-08-01 08:00:00');

-- ============================================================
-- 19. NOTIFICATION LOG — sample notifications
-- ============================================================
INSERT INTO notification_log
    (public_id, recipient_user_id, event_type, channel_lookup_id, status_lookup_id,
     subject, message_body, entity_type, entity_pubid, sent_at)
SELECT UUID(), u.id, 'ASSESSMENT_CYCLE_OPENED',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'NOTIFICATION_CHANNEL' AND lv.value_code = 'IN_APP'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'NOTIFICATION_STATUS' AND lv.value_code = 'SENT'),
    'New Assessment Cycle: ATLAS Q3 2026',
    'A new assessment cycle has been opened for ATLAS-deZentral. Please complete your self-assessment by 2026-08-31.',
    'ASSESSMENT_CYCLE', NULL, '2026-07-01 08:05:00'
FROM users u WHERE u.username = 'tech_john';

INSERT INTO notification_log
    (public_id, recipient_user_id, event_type, channel_lookup_id, status_lookup_id,
     subject, message_body, entity_type, entity_pubid, sent_at)
SELECT UUID(), u.id, 'ASSESSMENT_CYCLE_OPENED',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'NOTIFICATION_CHANNEL' AND lv.value_code = 'IN_APP'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'NOTIFICATION_STATUS' AND lv.value_code = 'SENT'),
    'New Assessment Cycle: ATLAS Q3 2026',
    'A new assessment cycle has been opened for ATLAS-deZentral. Please complete your self-assessment by 2026-08-31.',
    'ASSESSMENT_CYCLE', NULL, '2026-07-01 08:05:00'
FROM users u WHERE u.username = 'tech_jane';

INSERT INTO notification_log
    (public_id, recipient_user_id, event_type, channel_lookup_id, status_lookup_id,
     subject, message_body, entity_type, entity_pubid, sent_at, read_at)
SELECT UUID(), u.id, 'ASSESSMENT_APPROVED',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'NOTIFICATION_CHANNEL' AND lv.value_code = 'IN_APP'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'NOTIFICATION_STATUS' AND lv.value_code = 'READ'),
    'Your Q3 2026 Assessment Has Been Approved',
    'Your ATLAS-deZentral assessment has been approved. 4 skill gaps were identified and training recommendations have been created.',
    'ASSESSMENT_APPROVAL', NULL, '2026-07-30 11:05:00', '2026-07-30 14:00:00'
FROM users u WHERE u.username = 'tech_john';

INSERT INTO notification_log
    (public_id, recipient_user_id, event_type, channel_lookup_id, status_lookup_id,
     subject, message_body, entity_type, entity_pubid, sent_at)
SELECT UUID(), u.id, 'TRAINING_RECOMMENDED',
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'NOTIFICATION_CHANNEL' AND lv.value_code = 'EMAIL'),
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'NOTIFICATION_STATUS' AND lv.value_code = 'SENT'),
    'Training Recommendation: ATLAS Application Knowledge',
    'A training recommendation has been created for ATLAS Application Knowledge. Please complete the e-learning course by 2026-08-31.',
    'TRAINING_RECOMMENDATION', NULL, '2026-07-31 09:05:00'
FROM users u WHERE u.username = 'tech_john';

-- ============================================================
-- 20. IMPORT/EXPORT HISTORY — sample import record
-- ============================================================
INSERT INTO import_export_history
    (public_id, operation_type, template_type, file_name, file_path,
     total_rows, success_rows, failed_rows, status_lookup_id,
     initiated_by, started_at, completed_at)
SELECT UUID(), 'IMPORT', 'USERS_AND_SKILLS',
    'atlas_initial_skills_import_20260601.xlsx',
    '/uploads/imports/atlas_initial_skills_import_20260601.xlsx',
    15, 15, 0,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'IMPORT_EXPORT_STATUS' AND lv.value_code = 'SUCCESS'),
    (SELECT id FROM users WHERE username = 'admin'),
    '2026-06-01 09:00:00', '2026-06-01 09:01:30';

INSERT INTO import_export_history
    (public_id, operation_type, template_type, file_name, file_path,
     total_rows, success_rows, failed_rows, status_lookup_id,
     initiated_by, started_at, completed_at)
SELECT UUID(), 'EXPORT', 'SKILL_GAP_REPORT',
    'skill_gap_report_Q3_2026_20260730.xlsx',
    '/uploads/exports/skill_gap_report_Q3_2026_20260730.xlsx',
    4, 4, 0,
    (SELECT lv.id FROM lookup_value lv JOIN lookup_type lt ON lt.id = lv.lookup_type_id
     WHERE lt.type_code = 'IMPORT_EXPORT_STATUS' AND lv.value_code = 'SUCCESS'),
    (SELECT id FROM users WHERE username = 'lead_manager'),
    '2026-07-30 12:00:00', '2026-07-30 12:00:45';

-- ============================================================
-- V03 complete — DEV demo data seeded
-- Summary:
--    3 portfolios (B06-WEB, B06-HOST, B12-WEB)
--    5 applications (ATLAS, AVUS, BEPPO, HOST_APP_01, HOST_APP_02)
--    5 users (admin, lead_manager, tech_john, tech_jane, tech_mike)
--    3 teams, 4 user-team memberships
--    8 skills (ATLAS-specific + generic)
--    4 user-application mappings
--    1 requirement version (ATLAS v1.0) with 8 expected ratings
--    1 assessment cycle (ATLAS Q3 2026)
--    8 technician assessments (tech_john — all skills)
--    8 lead reviews (lead_manager reviewed all)
--    1 assessment approval (tech_john — APPROVED)
--    4 skill gap snapshots
--    4 training recommendations
--    4 training participants
--    8 audit log entries
--    4 notification log entries
--    2 import/export history records
--
-- Demo credentials (change on first login):
--   admin        / Password@123
--   lead_manager / Password@123
--   tech_john    / Password@123
--   tech_jane    / Password@123
--   tech_mike    / Password@123
-- ============================================================
