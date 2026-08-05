-- ============================================================
-- V02 — Seed Master Reference Data
-- Skill Matrix Platform
-- Seeds: lookup_types, lookup_values, rating_scale,
--        roles, permissions, role_permissions,
--        skill_categories, accounts, application_types, bundles
-- DO NOT MODIFY this file after deployment.
-- Create a new Vxx migration for any changes.
-- ============================================================

-- ============================================================
-- LOOKUP TYPES  (20 system types)
-- ============================================================
INSERT INTO lookup_type (public_id, type_code, type_name, is_system, display_order, is_active, created_by) VALUES
(UUID(), 'APPLICATION_TYPE_STATUS',     'Application Type Status',        1,  1, 1, 'system'),
(UUID(), 'PORTFOLIO_STATUS',            'Portfolio Status',               1,  2, 1, 'system'),
(UUID(), 'APPLICATION_LIFECYCLE_STATUS','Application Lifecycle Status',   1,  3, 1, 'system'),
(UUID(), 'REQUIREMENT_STATUS',          'Requirement Version Status',     1,  4, 1, 'system'),
(UUID(), 'ASSESSMENT_CYCLE_STATUS',     'Assessment Cycle Status',        1,  5, 1, 'system'),
(UUID(), 'ASSESSMENT_STATUS',           'Assessment Status',              1,  6, 1, 'system'),
(UUID(), 'REVIEW_ROW_DECISION',         'Review Row Decision',            1,  7, 1, 'system'),
(UUID(), 'OVERALL_DECISION',            'Overall Assessment Decision',    1,  8, 1, 'system'),
(UUID(), 'ROLE_ON_APPLICATION',         'Role on Application',            1,  9, 1, 'system'),
(UUID(), 'SKILL_TYPE',                  'Skill Type',                     1, 10, 1, 'system'),
(UUID(), 'SKILL_SCOPE',                 'Skill Scope',                    1, 11, 1, 'system'),
(UUID(), 'CRITICALITY',                 'Criticality Level',              1, 12, 1, 'system'),
(UUID(), 'GAP_SEVERITY',                'Gap Severity',                   1, 13, 1, 'system'),
(UUID(), 'TRAINING_TYPE',               'Training Type',                  1, 14, 1, 'system'),
(UUID(), 'TRAINING_PRIORITY',           'Training Priority',              1, 15, 1, 'system'),
(UUID(), 'TRAINING_STATUS',             'Training Recommendation Status', 1, 16, 1, 'system'),
(UUID(), 'NOTIFICATION_CHANNEL',        'Notification Channel',           1, 17, 1, 'system'),
(UUID(), 'NOTIFICATION_STATUS',         'Notification Status',            1, 18, 1, 'system'),
(UUID(), 'IMPORT_EXPORT_STATUS',        'Import Export Status',           1, 19, 1, 'system'),
(UUID(), 'BUNDLE_STATUS',               'Bundle Status',                  1, 20, 1, 'system');

-- ============================================================
-- LOOKUP VALUES
-- ============================================================

-- APPLICATION_TYPE_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'ACTIVE',   'Active',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_TYPE_STATUS' UNION ALL
SELECT UUID(), lt.id, 'INACTIVE', 'Inactive', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_TYPE_STATUS';

-- PORTFOLIO_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'ACTIVE',   'Active',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'PORTFOLIO_STATUS' UNION ALL
SELECT UUID(), lt.id, 'INACTIVE', 'Inactive', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'PORTFOLIO_STATUS';

-- APPLICATION_LIFECYCLE_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'ACTIVE',     'Active',     1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_LIFECYCLE_STATUS' UNION ALL
SELECT UUID(), lt.id, 'INACTIVE',   'Inactive',   2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_LIFECYCLE_STATUS' UNION ALL
SELECT UUID(), lt.id, 'DEPRECATED', 'Deprecated', 3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_LIFECYCLE_STATUS' UNION ALL
SELECT UUID(), lt.id, 'FUTURE',     'Future',     4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_LIFECYCLE_STATUS';

-- REQUIREMENT_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'DRAFT',     'Draft',     1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REQUIREMENT_STATUS' UNION ALL
SELECT UUID(), lt.id, 'PUBLISHED', 'Published', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REQUIREMENT_STATUS' UNION ALL
SELECT UUID(), lt.id, 'APPROVED',  'Approved',  3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REQUIREMENT_STATUS' UNION ALL
SELECT UUID(), lt.id, 'REJECTED',  'Rejected',  4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REQUIREMENT_STATUS';

-- ASSESSMENT_CYCLE_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'PLANNED',   'Planned',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_CYCLE_STATUS' UNION ALL
SELECT UUID(), lt.id, 'OPEN',      'Open',      2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_CYCLE_STATUS' UNION ALL
SELECT UUID(), lt.id, 'IN_REVIEW', 'In Review', 3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_CYCLE_STATUS' UNION ALL
SELECT UUID(), lt.id, 'CLOSED',    'Closed',    4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_CYCLE_STATUS' UNION ALL
SELECT UUID(), lt.id, 'CANCELLED', 'Cancelled', 5, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_CYCLE_STATUS';

-- ASSESSMENT_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'DRAFT',     'Draft',     1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_STATUS' UNION ALL
SELECT UUID(), lt.id, 'SUBMITTED', 'Submitted', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_STATUS' UNION ALL
SELECT UUID(), lt.id, 'RETURNED',  'Returned',  3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_STATUS' UNION ALL
SELECT UUID(), lt.id, 'APPROVED',  'Approved',  4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_STATUS';

-- REVIEW_ROW_DECISION
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'APPROVED',             'Approved',             1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REVIEW_ROW_DECISION' UNION ALL
SELECT UUID(), lt.id, 'GAP_IDENTIFIED',       'Gap Identified',       2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REVIEW_ROW_DECISION' UNION ALL
SELECT UUID(), lt.id, 'CLARIFICATION_NEEDED', 'Clarification Needed', 3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REVIEW_ROW_DECISION';

-- OVERALL_DECISION
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'APPROVED', 'Approved',                   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'OVERALL_DECISION' UNION ALL
SELECT UUID(), lt.id, 'RETURNED', 'Returned for Clarification', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'OVERALL_DECISION';

-- ROLE_ON_APPLICATION
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'PRIMARY', 'Primary', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ROLE_ON_APPLICATION' UNION ALL
SELECT UUID(), lt.id, 'BACKUP',  'Backup',  2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ROLE_ON_APPLICATION' UNION ALL
SELECT UUID(), lt.id, 'SME',     'SME',     3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ROLE_ON_APPLICATION' UNION ALL
SELECT UUID(), lt.id, 'TRAINEE', 'Trainee', 4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ROLE_ON_APPLICATION';

-- SKILL_TYPE
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'TECHNICAL',     'Technical',     1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'SKILL_TYPE' UNION ALL
SELECT UUID(), lt.id, 'NON_TECHNICAL', 'Non-Technical', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'SKILL_TYPE';

-- SKILL_SCOPE
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'APPLICATION', 'Application-Specific', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'SKILL_SCOPE' UNION ALL
SELECT UUID(), lt.id, 'GENERIC',     'Generic',              2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'SKILL_SCOPE';

-- CRITICALITY
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'HIGH',   'High',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'CRITICALITY' UNION ALL
SELECT UUID(), lt.id, 'MEDIUM', 'Medium', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'CRITICALITY' UNION ALL
SELECT UUID(), lt.id, 'LOW',    'Low',    3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'CRITICALITY';

-- GAP_SEVERITY
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'CRITICAL', 'Critical - Gap 3 or more', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'GAP_SEVERITY' UNION ALL
SELECT UUID(), lt.id, 'HIGH',     'High - Gap of 2',          2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'GAP_SEVERITY' UNION ALL
SELECT UUID(), lt.id, 'MEDIUM',   'Medium - Gap of 1',        3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'GAP_SEVERITY' UNION ALL
SELECT UUID(), lt.id, 'NONE',     'No Gap',                   4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'GAP_SEVERITY';

-- TRAINING_TYPE
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'ON_THE_JOB', 'On the Job Training', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_TYPE' UNION ALL
SELECT UUID(), lt.id, 'CLASSROOM',  'Classroom Training',  2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_TYPE' UNION ALL
SELECT UUID(), lt.id, 'E_LEARNING', 'E-Learning',          3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_TYPE' UNION ALL
SELECT UUID(), lt.id, 'MENTORING',  'Mentoring',           4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_TYPE' UNION ALL
SELECT UUID(), lt.id, 'SELF_STUDY', 'Self Study',          5, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_TYPE';

-- TRAINING_PRIORITY
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'HIGH',   'High - 30 days',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_PRIORITY' UNION ALL
SELECT UUID(), lt.id, 'MEDIUM', 'Medium - 60 days', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_PRIORITY' UNION ALL
SELECT UUID(), lt.id, 'LOW',    'Low - 90 days',    3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_PRIORITY';

-- TRAINING_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'RECOMMENDED', 'Recommended', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_STATUS' UNION ALL
SELECT UUID(), lt.id, 'CONFIRMED',   'Confirmed',   2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_STATUS' UNION ALL
SELECT UUID(), lt.id, 'IN_PROGRESS', 'In Progress', 3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_STATUS' UNION ALL
SELECT UUID(), lt.id, 'COMPLETED',   'Completed',   4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_STATUS' UNION ALL
SELECT UUID(), lt.id, 'CANCELLED',   'Cancelled',   5, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_STATUS';

-- NOTIFICATION_CHANNEL
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'EMAIL',  'Email',  1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_CHANNEL' UNION ALL
SELECT UUID(), lt.id, 'IN_APP', 'In-App', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_CHANNEL';

-- NOTIFICATION_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'PENDING', 'Pending', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_STATUS' UNION ALL
SELECT UUID(), lt.id, 'SENT',    'Sent',    2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_STATUS' UNION ALL
SELECT UUID(), lt.id, 'FAILED',  'Failed',  3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_STATUS' UNION ALL
SELECT UUID(), lt.id, 'READ',    'Read',    4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_STATUS';

-- IMPORT_EXPORT_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'IN_PROGRESS', 'In Progress', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'IMPORT_EXPORT_STATUS' UNION ALL
SELECT UUID(), lt.id, 'SUCCESS',     'Success',     2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'IMPORT_EXPORT_STATUS' UNION ALL
SELECT UUID(), lt.id, 'FAILED',      'Failed',      3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'IMPORT_EXPORT_STATUS' UNION ALL
SELECT UUID(), lt.id, 'PARTIAL',     'Partial',     4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'IMPORT_EXPORT_STATUS';

-- BUNDLE_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'ACTIVE',   'Active',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'BUNDLE_STATUS' UNION ALL
SELECT UUID(), lt.id, 'INACTIVE', 'Inactive', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'BUNDLE_STATUS' UNION ALL
SELECT UUID(), lt.id, 'FUTURE',   'Future',   3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'BUNDLE_STATUS';

-- ============================================================
-- RATING SCALE (1-5)
-- ============================================================
INSERT INTO rating_scale (public_id, level_value, level_name, level_meaning, is_active) VALUES
(UUID(), 1, 'Awareness',        'Understands basic concepts only; cannot perform tasks independently', 1),
(UUID(), 2, 'Working Knowledge','Can work with guidance; performs standard tasks with support',        1),
(UUID(), 3, 'Independent',      'Can work independently on regular tasks without guidance',            1),
(UUID(), 4, 'Advanced',         'Can handle complex scenarios and guide others on the topic',          1),
(UUID(), 5, 'SME / Expert',     'Recognised expert; can mentor, own critical issues, define standards',1);

-- ============================================================
-- ROLES
-- ============================================================
INSERT INTO roles (public_id, role_code, role_name, description, is_active, created_by) VALUES
(UUID(), 'ADMIN',        'Administrator', 'Full system configuration and management access',              1, 'system'),
(UUID(), 'LEAD_MANAGER', 'Lead Manager',  'Review, approve, manage skill gaps and view dashboards',       1, 'system'),
(UUID(), 'TECHNICIAN',   'Technician',    'Submit self-assessment for assigned applications',             1, 'system');

-- ============================================================
-- PERMISSIONS
-- ============================================================
INSERT INTO permissions (public_id, permission_code, permission_name, module, description, is_active) VALUES
-- Users module
(UUID(), 'USER_VIEW',              'View Users',                  'USERS',        'View user list and profiles',           1),
(UUID(), 'USER_CREATE',            'Create Users',                'USERS',        'Create new user accounts',              1),
(UUID(), 'USER_EDIT',              'Edit Users',                  'USERS',        'Edit user profiles and roles',          1),
(UUID(), 'USER_DEACTIVATE',        'Deactivate Users',            'USERS',        'Deactivate user accounts',              1),
-- Applications module
(UUID(), 'APP_VIEW',               'View Applications',           'APPLICATIONS', 'View application list and details',     1),
(UUID(), 'APP_CREATE',             'Create Applications',         'APPLICATIONS', 'Create new applications',               1),
(UUID(), 'APP_EDIT',               'Edit Applications',           'APPLICATIONS', 'Edit application details',              1),
(UUID(), 'APP_ASSIGN_TECHNICIAN',  'Assign Technicians',          'APPLICATIONS', 'Assign technicians to applications',    1),
-- Skills module
(UUID(), 'SKILL_VIEW',             'View Skills',                 'SKILLS',       'View skill catalogue',                  1),
(UUID(), 'SKILL_CREATE',           'Create Skills',               'SKILLS',       'Create new skills',                     1),
(UUID(), 'SKILL_EDIT',             'Edit Skills',                 'SKILLS',       'Edit skill definitions',                1),
(UUID(), 'REQUIREMENT_VIEW',       'View Requirements',           'SKILLS',       'View requirement versions',             1),
(UUID(), 'REQUIREMENT_MANAGE',     'Manage Requirements',         'SKILLS',       'Create and publish requirement versions',1),
-- Assessments module
(UUID(), 'ASSESSMENT_VIEW_OWN',    'View Own Assessments',        'ASSESSMENTS',  'View own assessment submissions',       1),
(UUID(), 'ASSESSMENT_SUBMIT',      'Submit Self-Assessment',      'ASSESSMENTS',  'Submit self-assessment ratings',        1),
(UUID(), 'ASSESSMENT_REVIEW',      'Review Assessments',          'ASSESSMENTS',  'Review and rate technician assessments',1),
(UUID(), 'ASSESSMENT_APPROVE',     'Approve Assessments',         'ASSESSMENTS',  'Approve final assessment decisions',    1),
(UUID(), 'CYCLE_MANAGE',           'Manage Assessment Cycles',    'ASSESSMENTS',  'Create and manage assessment cycles',   1),
-- Reports module
(UUID(), 'REPORT_VIEW_OWN',        'View Own Reports',            'REPORTS',      'View own skill gap and training data',  1),
(UUID(), 'REPORT_VIEW_TEAM',       'View Team Reports',           'REPORTS',      'View team skill gap reports',           1),
(UUID(), 'REPORT_VIEW_ALL',        'View All Reports',            'REPORTS',      'View all dashboards and reports',       1),
-- Training module
(UUID(), 'TRAINING_VIEW',          'View Training',               'TRAINING',     'View training recommendations',         1),
(UUID(), 'TRAINING_MANAGE',        'Manage Training',             'TRAINING',     'Confirm and manage training plans',     1),
-- Admin module
(UUID(), 'ADMIN_LOOKUP_MANAGE',    'Manage Lookup Data',          'ADMIN',        'Manage lookup types and values',        1),
(UUID(), 'ADMIN_IMPORT_EXPORT',    'Import/Export Data',          'ADMIN',        'Import and export platform data',       1),
(UUID(), 'ADMIN_AUDIT_VIEW',       'View Audit Log',              'ADMIN',        'View system audit log',                 1);

-- ============================================================
-- ROLE_PERMISSIONS
-- ADMIN gets all permissions
-- LEAD_MANAGER gets review/approve/reports/training
-- TECHNICIAN gets submit-own assessment/view-own reports
-- ============================================================

-- ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.role_code = 'ADMIN';

-- LEAD_MANAGER permissions
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.role_code = 'LEAD_MANAGER'
  AND p.permission_code IN (
    'USER_VIEW',
    'APP_VIEW',
    'APP_ASSIGN_TECHNICIAN',
    'SKILL_VIEW',
    'REQUIREMENT_VIEW',
    'REQUIREMENT_MANAGE',
    'ASSESSMENT_VIEW_OWN',
    'ASSESSMENT_REVIEW',
    'ASSESSMENT_APPROVE',
    'CYCLE_MANAGE',
    'REPORT_VIEW_OWN',
    'REPORT_VIEW_TEAM',
    'REPORT_VIEW_ALL',
    'TRAINING_VIEW',
    'TRAINING_MANAGE'
  );

-- TECHNICIAN permissions
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.role_code = 'TECHNICIAN'
  AND p.permission_code IN (
    'APP_VIEW',
    'SKILL_VIEW',
    'REQUIREMENT_VIEW',
    'ASSESSMENT_VIEW_OWN',
    'ASSESSMENT_SUBMIT',
    'REPORT_VIEW_OWN',
    'TRAINING_VIEW'
  );

-- ============================================================
-- SKILL CATEGORIES
-- ============================================================
INSERT INTO skill_categories (public_id, category_code, category_name, display_order, is_active, created_by) VALUES
(UUID(), 'APP_KNOWLEDGE',   'Application Knowledge', 1,  1, 'system'),
(UUID(), 'TECHNICAL',       'Technical Skills',      2,  1, 'system'),
(UUID(), 'INFRASTRUCTURE',  'Infrastructure',        3,  1, 'system'),
(UUID(), 'TOOLS',           'Tools',                 4,  1, 'system'),
(UUID(), 'SUPPORT_PROCESS', 'Support Process',       5,  1, 'system'),
(UUID(), 'DOCUMENTATION',   'Documentation',         6,  1, 'system'),
(UUID(), 'DOMAIN',          'Domain Knowledge',      7,  1, 'system'),
(UUID(), 'MONITORING',      'Monitoring',            8,  1, 'system'),
(UUID(), 'DEPLOYMENT',      'Deployment',            9,  1, 'system'),
(UUID(), 'DATABASE',        'Database',              10, 1, 'system');

-- ============================================================
-- ACCOUNTS  (master reference — pilot accounts)
-- ============================================================
INSERT INTO accounts (public_id, account_code, account_name, description, is_active, created_by) VALUES
(UUID(), 'NTT_DATA', 'NTT DATA Germany', 'NTT DATA pilot account for Skill Matrix Platform', 1, 'system');

-- ============================================================
-- APPLICATION TYPES  (master reference)
-- ============================================================
INSERT INTO application_types (public_id, type_code, type_name, description, is_active, created_by) VALUES
(UUID(), 'WEB',    'Web Application',    'Browser-based / web-tier application',         1, 'system'),
(UUID(), 'HOST',   'Host Application',   'Mainframe or host-based application',           1, 'system'),
(UUID(), 'BATCH',  'Batch Application',  'Scheduled batch processing application',        1, 'system'),
(UUID(), 'API',    'API Service',        'Backend API or microservice',                   1, 'system'),
(UUID(), 'MOBILE', 'Mobile Application', 'iOS or Android mobile application',             1, 'system');

-- ============================================================
-- BUNDLES  (master reference — organisational groupings)
-- ============================================================
INSERT INTO bundles (public_id, bundle_code, bundle_name, description, is_active, created_by) VALUES
(UUID(), 'B06', 'Bundle 06', 'Bundle 06 — Core operational applications',   1, 'system'),
(UUID(), 'B12', 'Bundle 12', 'Bundle 12 — Extended portfolio applications', 1, 'system'),
(UUID(), 'B20', 'Bundle 20', 'Bundle 20 — Future and planned applications', 1, 'system');

-- ============================================================
-- V02 complete
-- Summary:
--   20 lookup types, ~57 lookup values
--    5 rating scale levels
--    3 roles, 26 permissions, role_permissions mapped
--   10 skill categories
--    1 account (NTT_DATA)
--    5 application types
--    3 bundles
-- ============================================================
