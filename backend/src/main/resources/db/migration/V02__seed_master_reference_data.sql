-- ============================================================
-- V02 — Seed Master Reference Data
-- Skill Matrix Platform
-- Seeds: lookup types, lookup values, rating scale, roles
-- DO NOT MODIFY this file after deployment.
-- ============================================================

-- ============================================================
-- LOOKUP TYPES
-- ============================================================
INSERT INTO lookup_type (public_id, type_code, type_name, is_system, display_order, is_active, created_by) VALUES
(UUID(), 'APPLICATION_TYPE_STATUS',    'Application Type Status',    1, 1,  1, 'system'),
(UUID(), 'PORTFOLIO_STATUS',           'Portfolio Status',           1, 2,  1, 'system'),
(UUID(), 'APPLICATION_LIFECYCLE_STATUS','Application Lifecycle Status',1,3, 1, 'system'),
(UUID(), 'REQUIREMENT_STATUS',         'Requirement Version Status', 1, 4,  1, 'system'),
(UUID(), 'ASSESSMENT_STATUS',          'Assessment Status',          1, 5,  1, 'system'),
(UUID(), 'REVIEW_ROW_DECISION',        'Review Row Decision',        1, 6,  1, 'system'),
(UUID(), 'OVERALL_DECISION',           'Overall Assessment Decision',1, 7,  1, 'system'),
(UUID(), 'ROLE_ON_APPLICATION',        'Role on Application',        1, 8,  1, 'system'),
(UUID(), 'SKILL_TYPE',                 'Skill Type',                 1, 9,  1, 'system'),
(UUID(), 'SKILL_SCOPE',                'Skill Scope',                1, 10, 1, 'system'),
(UUID(), 'CRITICALITY',                'Criticality Level',          1, 11, 1, 'system'),
(UUID(), 'TRAINING_TYPE',              'Training Type',              1, 12, 1, 'system'),
(UUID(), 'TRAINING_PRIORITY',          'Training Priority',          1, 13, 1, 'system'),
(UUID(), 'NOTIFICATION_CHANNEL',       'Notification Channel',       1, 14, 1, 'system'),
(UUID(), 'NOTIFICATION_STATUS',        'Notification Status',        1, 15, 1, 'system'),
(UUID(), 'IMPORT_EXPORT_STATUS',       'Import Export Status',       1, 16, 1, 'system'),
(UUID(), 'BUNDLE_STATUS',              'Bundle Status',              1, 17, 1, 'system');

-- ============================================================
-- LOOKUP VALUES
-- ============================================================

-- APPLICATION_TYPE_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'ACTIVE',   'Active',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_TYPE_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'INACTIVE', 'Inactive', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_TYPE_STATUS';

-- PORTFOLIO_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'ACTIVE',   'Active',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'PORTFOLIO_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'INACTIVE', 'Inactive', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'PORTFOLIO_STATUS';

-- APPLICATION_LIFECYCLE_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'ACTIVE',     'Active',     1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_LIFECYCLE_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'INACTIVE',   'Inactive',   2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_LIFECYCLE_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'DEPRECATED', 'Deprecated', 3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_LIFECYCLE_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'FUTURE',     'Future',     4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'APPLICATION_LIFECYCLE_STATUS';

-- REQUIREMENT_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'DRAFT',     'Draft',     1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REQUIREMENT_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'PUBLISHED', 'Published', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REQUIREMENT_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'APPROVED',  'Approved',  3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REQUIREMENT_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'REJECTED',  'Rejected',  4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REQUIREMENT_STATUS';

-- ASSESSMENT_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'DRAFT',     'Draft',     1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'SUBMITTED', 'Submitted', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'RETURNED',  'Returned',  3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'APPROVED',  'Approved',  4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ASSESSMENT_STATUS';

-- REVIEW_ROW_DECISION
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'APPROVED',              'Approved',              1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REVIEW_ROW_DECISION'
UNION ALL
SELECT UUID(), lt.id, 'GAP_IDENTIFIED',        'Gap Identified',        2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REVIEW_ROW_DECISION'
UNION ALL
SELECT UUID(), lt.id, 'CLARIFICATION_NEEDED',  'Clarification Needed',  3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'REVIEW_ROW_DECISION';

-- OVERALL_DECISION
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'APPROVED',             'Approved',             1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'OVERALL_DECISION'
UNION ALL
SELECT UUID(), lt.id, 'RETURNED',             'Returned for Clarification', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'OVERALL_DECISION';

-- ROLE_ON_APPLICATION
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'PRIMARY', 'Primary',  1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ROLE_ON_APPLICATION'
UNION ALL
SELECT UUID(), lt.id, 'BACKUP',  'Backup',   2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ROLE_ON_APPLICATION'
UNION ALL
SELECT UUID(), lt.id, 'SME',     'SME',      3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ROLE_ON_APPLICATION'
UNION ALL
SELECT UUID(), lt.id, 'TRAINEE', 'Trainee',  4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'ROLE_ON_APPLICATION';

-- SKILL_TYPE
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'TECHNICAL',     'Technical',     1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'SKILL_TYPE'
UNION ALL
SELECT UUID(), lt.id, 'NON_TECHNICAL', 'Non-Technical', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'SKILL_TYPE';

-- SKILL_SCOPE
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'APPLICATION', 'Application-Specific', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'SKILL_SCOPE'
UNION ALL
SELECT UUID(), lt.id, 'GENERIC',     'Generic',             2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'SKILL_SCOPE';

-- CRITICALITY
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'HIGH',   'High',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'CRITICALITY'
UNION ALL
SELECT UUID(), lt.id, 'MEDIUM', 'Medium', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'CRITICALITY'
UNION ALL
SELECT UUID(), lt.id, 'LOW',    'Low',    3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'CRITICALITY';

-- TRAINING_TYPE
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'ON_THE_JOB',    'On the Job Training', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_TYPE'
UNION ALL
SELECT UUID(), lt.id, 'CLASSROOM',     'Classroom Training',  2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_TYPE'
UNION ALL
SELECT UUID(), lt.id, 'E_LEARNING',    'E-Learning',          3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_TYPE'
UNION ALL
SELECT UUID(), lt.id, 'MENTORING',     'Mentoring',           4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_TYPE'
UNION ALL
SELECT UUID(), lt.id, 'SELF_STUDY',    'Self Study',          5, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_TYPE';

-- TRAINING_PRIORITY
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'HIGH',   'High — 30 days',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_PRIORITY'
UNION ALL
SELECT UUID(), lt.id, 'MEDIUM', 'Medium — 60 days', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_PRIORITY'
UNION ALL
SELECT UUID(), lt.id, 'LOW',    'Low — 90 days',    3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'TRAINING_PRIORITY';

-- NOTIFICATION_CHANNEL
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'EMAIL',  'Email',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_CHANNEL'
UNION ALL
SELECT UUID(), lt.id, 'IN_APP', 'In-App',  2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_CHANNEL';

-- NOTIFICATION_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'PENDING', 'Pending', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'SENT',    'Sent',    2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'FAILED',  'Failed',  3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'READ',    'Read',    4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'NOTIFICATION_STATUS';

-- IMPORT_EXPORT_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'IN_PROGRESS', 'In Progress', 1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'IMPORT_EXPORT_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'SUCCESS',     'Success',     2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'IMPORT_EXPORT_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'FAILED',      'Failed',      3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'IMPORT_EXPORT_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'PARTIAL',     'Partial',     4, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'IMPORT_EXPORT_STATUS';

-- BUNDLE_STATUS
INSERT INTO lookup_value (public_id, lookup_type_id, value_code, value_label, display_order, is_system, is_active, created_by)
SELECT UUID(), lt.id, 'ACTIVE',   'Active',   1, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'BUNDLE_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'INACTIVE', 'Inactive', 2, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'BUNDLE_STATUS'
UNION ALL
SELECT UUID(), lt.id, 'FUTURE',   'Future',   3, 1, 1, 'system' FROM lookup_type lt WHERE lt.type_code = 'BUNDLE_STATUS';

-- ============================================================
-- RATING SCALE (levels 1-5)
-- ============================================================
INSERT INTO rating_scale (public_id, level_value, level_name, level_meaning, is_active) VALUES
(UUID(), 1, 'Awareness',       'Understands basic concepts only; cannot perform tasks independently', 1),
(UUID(), 2, 'Working Knowledge','Can work with guidance; performs standard tasks with support', 1),
(UUID(), 3, 'Independent',     'Can work independently on regular tasks without guidance', 1),
(UUID(), 4, 'Advanced',        'Can handle complex scenarios and guide others on the topic', 1),
(UUID(), 5, 'SME / Expert',    'Recognised expert; can mentor, own critical issues, and define standards', 1);

-- ============================================================
-- ROLES
-- ============================================================
INSERT INTO roles (public_id, role_code, role_name, description, is_active, created_by) VALUES
(UUID(), 'ADMIN',        'Administrator',  'Full system configuration and management access', 1, 'system'),
(UUID(), 'LEAD_MANAGER', 'Lead Manager',   'Review, approve, manage skill gaps and view dashboards', 1, 'system'),
(UUID(), 'TECHNICIAN',   'Technician',     'Submit self-assessment for assigned applications', 1, 'system');

-- ============================================================
-- SKILL CATEGORIES
-- ============================================================
INSERT INTO skill_categories (public_id, category_code, category_name, display_order, is_active, created_by) VALUES
(UUID(), 'APP_KNOWLEDGE',  'Application Knowledge', 1,  1, 'system'),
(UUID(), 'TECHNICAL',      'Technical Skills',      2,  1, 'system'),
(UUID(), 'INFRASTRUCTURE', 'Infrastructure',        3,  1, 'system'),
(UUID(), 'TOOLS',          'Tools',                 4,  1, 'system'),
(UUID(), 'SUPPORT_PROCESS','Support Process',       5,  1, 'system'),
(UUID(), 'DOCUMENTATION',  'Documentation',         6,  1, 'system'),
(UUID(), 'DOMAIN',         'Domain Knowledge',      7,  1, 'system'),
(UUID(), 'MONITORING',     'Monitoring',            8,  1, 'system'),
(UUID(), 'DEPLOYMENT',     'Deployment',            9,  1, 'system'),
(UUID(), 'DATABASE',       'Database',              10, 1, 'system');

-- ============================================================
-- V02 complete
-- ============================================================
