-- ============================================================
-- Skill Matrix Platform — Create Local Application User
-- Run as: mysql -u root -p < database/local_setup/01_create_local_user.sql
-- NOTE: Change password for QA and PROD environments
-- ============================================================

-- Create user for localhost connections (local dev)
CREATE USER IF NOT EXISTS 'skillmatrix_user'@'localhost'
    IDENTIFIED BY 'skillmatrix_pass';

-- Create user for Docker container connections (% = any host)
CREATE USER IF NOT EXISTS 'skillmatrix_user'@'%'
    IDENTIFIED BY 'skillmatrix_pass';

-- Grant full privileges on skill_matrix_db for local dev
GRANT ALL PRIVILEGES ON skill_matrix_db.* TO 'skillmatrix_user'@'localhost';
GRANT ALL PRIVILEGES ON skill_matrix_db.* TO 'skillmatrix_user'@'%';

-- Flush to apply
FLUSH PRIVILEGES;

-- Verify user was created
SELECT User, Host, authentication_string IS NOT NULL AS has_password
FROM mysql.user
WHERE User = 'skillmatrix_user';
