-- ============================================================
-- V07 — Update Demo Users and Roles (DEV / LOCAL ONLY)
-- Skill Matrix Platform
-- WARNING: DEV/LOCAL ONLY — DO NOT run in QA or PROD.
--
-- Replaces placeholder demo users (lead_manager, tech_john,
-- tech_jane, tech_mike) with real NTT DATA team members.
-- Adds 3 new technician users (IDs 6, 7, 8).
--
-- Demo password for all users:  Password@123
-- BCrypt hash (cost 12):
--   $2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lLqy
--
-- Final user list:
--   ID 1  admin           ADMIN
--   ID 2  dumitru_baboiu  LEAD_MANAGER
--   ID 3  thomas_daniel   LEAD_MANAGER
--   ID 4  frank_going     LEAD_MANAGER
--   ID 5  venkata_janga   LEAD_MANAGER
--   ID 6  deepak_mishra   TECHNICIAN
--   ID 7  waseem_mp       TECHNICIAN
--   ID 8  bhupendra_singh TECHNICIAN
--
-- This migration is idempotent — safe to run more than once.
-- DO NOT MODIFY this file after first deployment.
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- SECTION 1 — UPDATE EXISTING USERS (IDs 1–5)
--             Preserve password_hash. Update all other fields.
-- ============================================================

-- ID 1: admin — System Administrator (ADMIN)
UPDATE users
SET username             = 'admin',
    full_name            = 'System Administrator',
    email                = 'admin@nttdata.com',
    primary_role_id      = (SELECT id FROM roles WHERE role_code = 'ADMIN'),
    manager_id           = NULL,
    is_active            = 1,
    must_change_password = 0,
    failed_login_attempts = 0,
    account_locked       = 0,
    lock_time            = NULL,
    updated_by           = 'V07_migration'
WHERE id = 1;

-- ID 2: lead_manager → dumitru_baboiu (LEAD_MANAGER, manager = admin)
UPDATE users
SET username             = 'dumitru_baboiu',
    full_name            = 'Dumitru Baboiu',
    email                = 'dumitru.baboiu.bp@nttdata.com',
    primary_role_id      = (SELECT id FROM roles WHERE role_code = 'LEAD_MANAGER'),
    manager_id           = 1,
    is_active            = 1,
    must_change_password = 0,
    failed_login_attempts = 0,
    account_locked       = 0,
    lock_time            = NULL,
    updated_by           = 'V07_migration'
WHERE id = 2;

-- ID 3: tech_john → thomas_daniel (LEAD_MANAGER)
UPDATE users
SET username             = 'thomas_daniel',
    full_name            = 'Thomas Daniel Mihnea',
    email                = 'thomas.mihnea@nttdata.com',
    primary_role_id      = (SELECT id FROM roles WHERE role_code = 'LEAD_MANAGER'),
    manager_id           = 1,
    is_active            = 1,
    must_change_password = 0,
    failed_login_attempts = 0,
    account_locked       = 0,
    lock_time            = NULL,
    updated_by           = 'V07_migration'
WHERE id = 3;

-- ID 4: tech_jane → frank_going  (LEAD_MANAGER)
-- Note: full_name preserves the umlaut: Frank Göing
UPDATE users
SET username             = 'frank_going',
    full_name            = 'Frank Göing',
    email                = 'Frank.Goeing@emeal.nttdata.com',
    primary_role_id      = (SELECT id FROM roles WHERE role_code = 'LEAD_MANAGER'),
    manager_id           = 1,
    is_active            = 1,
    must_change_password = 0,
    failed_login_attempts = 0,
    account_locked       = 0,
    lock_time            = NULL,
    updated_by           = 'V07_migration'
WHERE id = 4;

-- ID 5: tech_mike → venkata_janga (LEAD_MANAGER)
UPDATE users
SET username             = 'venkata_janga',
    full_name            = 'Venkata Kiran Kumar Janga',
    email                = 'VenkataKiranKumar.Janga@nttdata.com',
    primary_role_id      = (SELECT id FROM roles WHERE role_code = 'LEAD_MANAGER'),
    manager_id           = 1,
    is_active            = 1,
    must_change_password = 0,
    failed_login_attempts = 0,
    account_locked       = 0,
    lock_time            = NULL,
    updated_by           = 'V07_migration'
WHERE id = 5;

-- ============================================================
-- SECTION 2 — INSERT / UPSERT NEW TECHNICIAN USERS (IDs 6–8)
--             Reuse existing admin BCrypt hash.
--             Manager = dumitru_baboiu (ID 2).
-- ============================================================

-- We use a subquery alias to allow reading from the same table in MySQL.
SET @admin_hash = (SELECT password_hash FROM (SELECT password_hash FROM users WHERE id = 1) AS _ph);
SET @tech_role  = (SELECT id FROM roles WHERE role_code = 'TECHNICIAN');

-- ID 6: deepak_mishra (TECHNICIAN)
INSERT INTO users (id, public_id, username, email, password_hash, full_name,
                   manager_id, primary_role_id, is_active, must_change_password,
                   failed_login_attempts, account_locked, created_by, updated_by)
VALUES (6, UUID(), 'deepak_mishra', 'Deepak.Kumar.Mishra@nttdata.com',
        @admin_hash, 'Deepak Kumar Mishra',
        2, @tech_role, 1, 0, 0, 0, 'V07_migration', 'V07_migration')
ON DUPLICATE KEY UPDATE
  username             = VALUES(username),
  email                = VALUES(email),
  full_name            = VALUES(full_name),
  primary_role_id      = @tech_role,
  manager_id           = 2,
  is_active            = 1,
  must_change_password = 0,
  failed_login_attempts = 0,
  account_locked       = 0,
  lock_time            = NULL,
  updated_by           = 'V07_migration';

-- ID 7: waseem_mp (TECHNICIAN)
INSERT INTO users (id, public_id, username, email, password_hash, full_name,
                   manager_id, primary_role_id, is_active, must_change_password,
                   failed_login_attempts, account_locked, created_by, updated_by)
VALUES (7, UUID(), 'waseem_mp', 'Waseem.Mp@nttdata.com',
        @admin_hash, 'Waseem Mp',
        2, @tech_role, 1, 0, 0, 0, 'V07_migration', 'V07_migration')
ON DUPLICATE KEY UPDATE
  username             = VALUES(username),
  email                = VALUES(email),
  full_name            = VALUES(full_name),
  primary_role_id      = @tech_role,
  manager_id           = 2,
  is_active            = 1,
  must_change_password = 0,
  failed_login_attempts = 0,
  account_locked       = 0,
  lock_time            = NULL,
  updated_by           = 'V07_migration';

-- ID 8: bhupendra_singh (TECHNICIAN)
INSERT INTO users (id, public_id, username, email, password_hash, full_name,
                   manager_id, primary_role_id, is_active, must_change_password,
                   failed_login_attempts, account_locked, created_by, updated_by)
VALUES (8, UUID(), 'bhupendra_singh', 'Bhupendra.Singh4@nttdata.com',
        @admin_hash, 'Bhupendra Singh',
        2, @tech_role, 1, 0, 0, 0, 'V07_migration', 'V07_migration')
ON DUPLICATE KEY UPDATE
  username             = VALUES(username),
  email                = VALUES(email),
  full_name            = VALUES(full_name),
  primary_role_id      = @tech_role,
  manager_id           = 2,
  is_active            = 1,
  must_change_password = 0,
  failed_login_attempts = 0,
  account_locked       = 0,
  lock_time            = NULL,
  updated_by           = 'V07_migration';

-- ============================================================
-- SECTION 3 — SET AUTO_INCREMENT TO AT LEAST 9
-- ============================================================
ALTER TABLE users AUTO_INCREMENT = 9;

-- ============================================================
-- SECTION 4 — CLEAR REFRESH TOKENS FOR USERS 1–8
--             Revoke and soft-delete all active tokens so that
--             stale sessions from old usernames are invalidated.
-- ============================================================
UPDATE refresh_tokens
SET    revoked    = 1,
       is_deleted = 1,
       updated_at = NOW()
WHERE  user_id IN (1, 2, 3, 4, 5, 6, 7, 8);

-- ============================================================
-- SECTION 5 — REBUILD USER_ROLES FOR USERS 1–8
--             Delete all existing role entries for these users,
--             then reinsert with the correct role_code lookups.
-- ============================================================
DELETE FROM user_roles WHERE user_id IN (1, 2, 3, 4, 5, 6, 7, 8);

-- ID 1 → ADMIN
INSERT INTO user_roles (user_id, role_id, assigned_by, is_active)
SELECT 1, r.id, 'V07_migration', 1
FROM   roles r WHERE r.role_code = 'ADMIN';

-- IDs 2, 3, 4, 5 → LEAD_MANAGER
INSERT INTO user_roles (user_id, role_id, assigned_by, is_active)
SELECT u.id, r.id, 'V07_migration', 1
FROM   users u
CROSS JOIN roles r
WHERE  u.id IN (2, 3, 4, 5) AND r.role_code = 'LEAD_MANAGER';

-- IDs 6, 7, 8 → TECHNICIAN
INSERT INTO user_roles (user_id, role_id, assigned_by, is_active)
SELECT u.id, r.id, 'V07_migration', 1
FROM   users u
CROSS JOIN roles r
WHERE  u.id IN (6, 7, 8) AND r.role_code = 'TECHNICIAN';

-- ============================================================
-- SECTION 6 — REBUILD USER_TEAMS
--             Remove old memberships for users 1–8,
--             then reassign to appropriate teams.
-- ============================================================
DELETE FROM user_teams WHERE user_id IN (1, 2, 3, 4, 5, 6, 7, 8);

-- dumitru_baboiu (2) → ATLAS_TEAM (lead reviewer)
INSERT INTO user_teams (user_id, team_id, is_active)
SELECT u.id, t.id, 1
FROM users u JOIN teams t ON t.team_code = 'ATLAS_TEAM'
WHERE u.id = 2;

-- deepak_mishra (6) → ATLAS_TEAM
INSERT INTO user_teams (user_id, team_id, is_active)
SELECT u.id, t.id, 1
FROM users u JOIN teams t ON t.team_code = 'ATLAS_TEAM'
WHERE u.id = 6;

-- waseem_mp (7) → ATLAS_TEAM
INSERT INTO user_teams (user_id, team_id, is_active)
SELECT u.id, t.id, 1
FROM users u JOIN teams t ON t.team_code = 'ATLAS_TEAM'
WHERE u.id = 7;

-- bhupendra_singh (8) → AVUS_TEAM
INSERT INTO user_teams (user_id, team_id, is_active)
SELECT u.id, t.id, 1
FROM users u JOIN teams t ON t.team_code = 'AVUS_TEAM'
WHERE u.id = 8;

-- ============================================================
-- SECTION 7 — UPDATE USER_APPLICATION_MAPPING
--             Reassign old technician mappings to new technician IDs.
--             Mapping:
--               old tech_john (3) on ATLAS  → deepak_mishra (6)
--               old tech_jane (4) on ATLAS  → waseem_mp     (7)
--               old tech_mike (5) on AVUS   → bhupendra_singh (8)
--             Lead manager mapping (user 2) stays — only user_id updated.
-- ============================================================

-- Old tech_john (3) → deepak_mishra (6) on ATLAS
UPDATE user_application_mapping uam
JOIN   applications a ON a.id = uam.application_id AND a.application_code = 'ATLAS'
SET    uam.user_id    = 6,
       uam.updated_by = 'V07_migration'
WHERE  uam.user_id = 3;

-- Old tech_jane (4) → waseem_mp (7) on ATLAS
UPDATE user_application_mapping uam
JOIN   applications a ON a.id = uam.application_id AND a.application_code = 'ATLAS'
SET    uam.user_id    = 7,
       uam.updated_by = 'V07_migration'
WHERE  uam.user_id = 4;

-- Old tech_mike (5) → bhupendra_singh (8) on AVUS
UPDATE user_application_mapping uam
JOIN   applications a ON a.id = uam.application_id AND a.application_code = 'AVUS'
SET    uam.user_id    = 8,
       uam.updated_by = 'V07_migration'
WHERE  uam.user_id = 5;

-- ============================================================
-- SECTION 8 — UPDATE TECHNICIAN_ASSESSMENTS
--             Old tech_john (3) → deepak_mishra (6)
-- ============================================================
UPDATE technician_assessments
SET    user_id    = 6,
       updated_by = 'V07_migration'
WHERE  user_id = 3;

-- ============================================================
-- SECTION 9 — UPDATE ASSESSMENT_APPROVALS
--             Technician side: old tech_john (3) → deepak_mishra (6)
--             Reviewer side: lead_manager (2) unchanged — still a lead manager.
-- ============================================================
UPDATE assessment_approvals
SET    technician_user_id = 6,
       updated_by         = 'V07_migration'
WHERE  technician_user_id = 3;

-- ============================================================
-- SECTION 10 — UPDATE SKILL_GAP_SNAPSHOTS
--              old tech_john (3) → deepak_mishra (6)
-- ============================================================
UPDATE skill_gap_snapshots
SET    user_id = 6
WHERE  user_id = 3;

-- ============================================================
-- SECTION 11 — UPDATE TRAINING_PARTICIPANTS
--              old tech_john (3) → deepak_mishra (6)
-- ============================================================
UPDATE training_participants
SET    user_id = 6
WHERE  user_id = 3;

-- ============================================================
-- SECTION 12 — UPDATE NOTIFICATION_LOG
--              Reassign notifications sent to old technician users
--              to the new technician who took that role.
-- ============================================================
UPDATE notification_log SET recipient_user_id = 6 WHERE recipient_user_id = 3;
UPDATE notification_log SET recipient_user_id = 7 WHERE recipient_user_id = 4;
UPDATE notification_log SET recipient_user_id = 8 WHERE recipient_user_id = 5;

-- ============================================================
-- SECTION 13 — UPDATE IMPORT/EXPORT HISTORY
--              initiated_by references users by ID; lead_manager (2)
--              becomes dumitru_baboiu but ID stays the same — no change needed.
--              admin (1) is unchanged — no change needed.
-- ============================================================
-- (No changes required — IDs 1 and 2 are preserved.)

-- ============================================================
-- SECTION 14 — UPDATE AUDIT LOG actor references
--              Update actor_username for renamed users so audit
--              log context is consistent with new usernames.
-- ============================================================
UPDATE audit_log SET actor_username = 'dumitru_baboiu'  WHERE actor_username = 'lead_manager';
UPDATE audit_log SET actor_username = 'thomas_daniel'   WHERE actor_username = 'tech_john';
UPDATE audit_log SET actor_username = 'frank_going'     WHERE actor_username = 'tech_jane';
UPDATE audit_log SET actor_username = 'venkata_janga'   WHERE actor_username = 'tech_mike';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- V07 complete — Demo users updated
--
-- Final state:
--   ID 1  admin            ADMIN          admin@nttdata.com
--   ID 2  dumitru_baboiu   LEAD_MANAGER   dumitru.baboiu.bp@nttdata.com
--   ID 3  thomas_daniel    LEAD_MANAGER   thomas.mihnea@nttdata.com
--   ID 4  frank_going      LEAD_MANAGER   Frank.Goeing@emeal.nttdata.com
--   ID 5  venkata_janga    LEAD_MANAGER   VenkataKiranKumar.Janga@nttdata.com
--   ID 6  deepak_mishra    TECHNICIAN     Deepak.Kumar.Mishra@nttdata.com
--   ID 7  waseem_mp        TECHNICIAN     Waseem.Mp@nttdata.com
--   ID 8  bhupendra_singh  TECHNICIAN     Bhupendra.Singh4@nttdata.com
--
-- All passwords remain: Password@123
-- AUTO_INCREMENT on users set to 9.
-- ============================================================

-- ============================================================
-- VERIFICATION QUERIES (run manually — not part of migration)
-- ============================================================
-- -- 1. Confirm all 8 users with correct fields:
-- SELECT id, username, full_name, email, is_active, must_change_password
-- FROM users WHERE id BETWEEN 1 AND 8 ORDER BY id;
--
-- -- 2. Confirm LEAD_MANAGER roles for IDs 2–5:
-- SELECT u.id, u.username, r.role_code
-- FROM users u
-- JOIN user_roles ur ON ur.user_id = u.id
-- JOIN roles r ON r.id = ur.role_id
-- WHERE u.id BETWEEN 2 AND 5;
--
-- -- 3. Confirm TECHNICIAN roles for IDs 6–8:
-- SELECT u.id, u.username, r.role_code
-- FROM users u
-- JOIN user_roles ur ON ur.user_id = u.id
-- JOIN roles r ON r.id = ur.role_id
-- WHERE u.id BETWEEN 6 AND 8;
--
-- -- 4. Confirm no old demo usernames remain:
-- SELECT id, username FROM users
-- WHERE username IN ('lead_manager','tech_john','tech_jane','tech_mike');
-- -- Expected: 0 rows
--
-- -- 5. Confirm technician app mappings point to IDs 6, 7, 8:
-- SELECT uam.user_id, u.username, a.application_code, uam.allocation_percentage
-- FROM user_application_mapping uam
-- JOIN users u ON u.id = uam.user_id
-- JOIN applications a ON a.id = uam.application_id
-- WHERE uam.user_id IN (6, 7, 8);
--
-- -- 6. Confirm lead review records use reviewer IDs 2–5:
-- SELECT lr.id, lr.reviewer_user_id, u.username
-- FROM lead_reviews lr
-- JOIN users u ON u.id = lr.reviewer_user_id;
