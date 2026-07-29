-- ============================================================
-- V03 — Seed Demo Data (DEV ONLY)
-- Skill Matrix Platform
-- WARNING: DO NOT run in QA or PROD environments.
-- This file is only loaded when spring.flyway.locations includes
--   classpath:db/dev-migration (configured in application-dev.yml only)
-- ============================================================

-- ============================================================
-- DEMO ACCOUNT
-- ============================================================
INSERT INTO accounts (public_id, account_code, account_name, description, is_active, created_by) VALUES
(UUID(), 'DEMO_ACCOUNT', 'Demo Organisation', 'Demo account for development and testing', 1, 'system');

-- ============================================================
-- APPLICATION TYPES
-- ============================================================
INSERT INTO application_types (public_id, type_code, type_name, description, is_active, created_by) VALUES
(UUID(), 'WEB',  'Web Applications',  'Web-based application type',  1, 'system'),
(UUID(), 'HOST', 'Host Applications', 'Host-based application type', 1, 'system');

-- ============================================================
-- BUNDLES
-- ============================================================
INSERT INTO bundles (public_id, bundle_code, bundle_name, description, is_active, created_by) VALUES
(UUID(), 'B06', 'Bundle 06', 'Bundle 06 - Core applications',   1, 'system'),
(UUID(), 'B12', 'Bundle 12', 'Bundle 12 - Extended portfolio',  1, 'system'),
(UUID(), 'B20', 'Bundle 20', 'Bundle 20 - Future applications', 1, 'system');

-- ============================================================
-- APPLICATION PORTFOLIOS
-- B06-WEB, B06-HOST, B12-WEB, B12-HOST, B20-WEB, B20-HOST
-- ============================================================
INSERT INTO application_portfolios (public_id, account_id, application_type_id, bundle_id, portfolio_code, portfolio_name, is_active, created_by)
SELECT UUID(), a.id, at2.id, b.id, 'B06-WEB', 'Bundle 06 Web Portfolio', 1, 'system'
FROM accounts a, application_types at2, bundles b
WHERE a.account_code = 'DEMO_ACCOUNT' AND at2.type_code = 'WEB' AND b.bundle_code = 'B06';

INSERT INTO application_portfolios (public_id, account_id, application_type_id, bundle_id, portfolio_code, portfolio_name, is_active, created_by)
SELECT UUID(), a.id, at2.id, b.id, 'B06-HOST', 'Bundle 06 Host Portfolio', 1, 'system'
FROM accounts a, application_types at2, bundles b
WHERE a.account_code = 'DEMO_ACCOUNT' AND at2.type_code = 'HOST' AND b.bundle_code = 'B06';

INSERT INTO application_portfolios (public_id, account_id, application_type_id, bundle_id, portfolio_code, portfolio_name, is_active, created_by)
SELECT UUID(), a.id, at2.id, b.id, 'B12-WEB', 'Bundle 12 Web Portfolio', 1, 'system'
FROM accounts a, application_types at2, bundles b
WHERE a.account_code = 'DEMO_ACCOUNT' AND at2.type_code = 'WEB' AND b.bundle_code = 'B12';

INSERT INTO application_portfolios (public_id, account_id, application_type_id, bundle_id, portfolio_code, portfolio_name, is_active, created_by)
SELECT UUID(), a.id, at2.id, b.id, 'B12-HOST', 'Bundle 12 Host Portfolio', 1, 'system'
FROM accounts a, application_types at2, bundles b
WHERE a.account_code = 'DEMO_ACCOUNT' AND at2.type_code = 'HOST' AND b.bundle_code = 'B12';

-- ============================================================
-- DEMO APPLICATIONS under B06-WEB
-- ============================================================
INSERT INTO applications (public_id, portfolio_id, application_code, application_name, is_active, created_by)
SELECT UUID(), ap.id, 'ATLAS', 'ATLAS-deZentral', 1, 'system'
FROM application_portfolios ap WHERE ap.portfolio_code = 'B06-WEB';

INSERT INTO applications (public_id, portfolio_id, application_code, application_name, is_active, created_by)
SELECT UUID(), ap.id, 'AVUS', 'AVUS', 1, 'system'
FROM application_portfolios ap WHERE ap.portfolio_code = 'B06-WEB';

INSERT INTO applications (public_id, portfolio_id, application_code, application_name, is_active, created_by)
SELECT UUID(), ap.id, 'BEPPO', 'BEPPO', 1, 'system'
FROM application_portfolios ap WHERE ap.portfolio_code = 'B06-WEB';

-- ============================================================
-- DEMO APPLICATIONS under B06-HOST
-- ============================================================
INSERT INTO applications (public_id, portfolio_id, application_code, application_name, is_active, created_by)
SELECT UUID(), ap.id, 'HOST_APP_01', 'Host Application 01', 1, 'system'
FROM application_portfolios ap WHERE ap.portfolio_code = 'B06-HOST';

INSERT INTO applications (public_id, portfolio_id, application_code, application_name, is_active, created_by)
SELECT UUID(), ap.id, 'HOST_APP_02', 'Host Application 02', 1, 'system'
FROM application_portfolios ap WHERE ap.portfolio_code = 'B06-HOST';

-- ============================================================
-- DEMO USERS
-- Passwords are BCrypt of 'Password1!'
-- $2a$12$... is a placeholder — replace with actual BCrypt hash
-- ============================================================

-- Admin user
INSERT INTO users (public_id, username, email, password_hash, full_name, employee_id, primary_role_id, is_active, created_by)
SELECT UUID(), 'admin', 'admin@skillmatrix.local',
    '$2a$12$tK5TGbS1f8aYfYkH6TRqJOdE7jLgz6lOvXLMBFE8QVGT9ZJ.OJFqK',
    'System Administrator', 'EMP001',
    r.id, 1, 'system'
FROM roles r WHERE r.role_code = 'ADMIN';

-- Lead Manager user
INSERT INTO users (public_id, username, email, password_hash, full_name, employee_id, primary_role_id, is_active, created_by)
SELECT UUID(), 'lead_manager', 'leadmanager@skillmatrix.local',
    '$2a$12$tK5TGbS1f8aYfYkH6TRqJOdE7jLgz6lOvXLMBFE8QVGT9ZJ.OJFqK',
    'Demo Lead Manager', 'EMP002',
    r.id, 1, 'system'
FROM roles r WHERE r.role_code = 'LEAD_MANAGER';

-- Technician user 1
INSERT INTO users (public_id, username, email, password_hash, full_name, employee_id, primary_role_id, is_active, created_by)
SELECT UUID(), 'tech_john', 'john.smith@skillmatrix.local',
    '$2a$12$tK5TGbS1f8aYfYkH6TRqJOdE7jLgz6lOvXLMBFE8QVGT9ZJ.OJFqK',
    'John Smith', 'EMP003',
    r.id, 1, 'system'
FROM roles r WHERE r.role_code = 'TECHNICIAN';

-- Technician user 2
INSERT INTO users (public_id, username, email, password_hash, full_name, employee_id, primary_role_id, is_active, created_by)
SELECT UUID(), 'tech_jane', 'jane.doe@skillmatrix.local',
    '$2a$12$tK5TGbS1f8aYfYkH6TRqJOdE7jLgz6lOvXLMBFE8QVGT9ZJ.OJFqK',
    'Jane Doe', 'EMP004',
    r.id, 1, 'system'
FROM roles r WHERE r.role_code = 'TECHNICIAN';

-- Assign roles to users in user_roles
INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'system' FROM users u, roles r
WHERE u.username = 'admin' AND r.role_code = 'ADMIN';

INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'system' FROM users u, roles r
WHERE u.username = 'lead_manager' AND r.role_code = 'LEAD_MANAGER';

INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'system' FROM users u, roles r
WHERE u.username = 'tech_john' AND r.role_code = 'TECHNICIAN';

INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'system' FROM users u, roles r
WHERE u.username = 'tech_jane' AND r.role_code = 'TECHNICIAN';

-- ============================================================
-- DEMO SKILLS
-- ============================================================
INSERT INTO skills (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'APP_KNOWLEDGE_ATLAS', 'ATLAS Application Knowledge',
    'Knowledge of ATLAS-deZentral application functionality', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'APP_KNOWLEDGE';

INSERT INTO skills (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'INCIDENT_MANAGEMENT', 'Incident Management',
    'Ability to manage and resolve application incidents', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'SUPPORT_PROCESS';

INSERT INTO skills (public_id, skill_category_id, skill_code, skill_name, description, is_active, created_by)
SELECT UUID(), sc.id, 'DB_QUERY', 'Database Query and Analysis',
    'Ability to write and analyse SQL queries for troubleshooting', 1, 'system'
FROM skill_categories sc WHERE sc.category_code = 'DATABASE';

-- ============================================================
-- DEMO TECHNICIAN ASSIGNMENTS
-- Assign tech_john and tech_jane to ATLAS application
-- ============================================================
INSERT INTO user_application_mapping (public_id, user_id, application_id, allocation_percentage, effective_from, is_active, created_by)
SELECT UUID(), u.id, a.id, 100.00, '2026-01-01', 1, 'system'
FROM users u, applications a
WHERE u.username = 'tech_john' AND a.application_code = 'ATLAS';

INSERT INTO user_application_mapping (public_id, user_id, application_id, allocation_percentage, effective_from, is_active, created_by)
SELECT UUID(), u.id, a.id, 80.00, '2026-01-01', 1, 'system'
FROM users u, applications a
WHERE u.username = 'tech_jane' AND a.application_code = 'ATLAS';

-- ============================================================
-- V03 complete — DEV demo data seeded
-- Demo credentials (change on first login):
--   admin        / Password1!
--   lead_manager / Password1!
--   tech_john    / Password1!
--   tech_jane    / Password1!
-- ============================================================
