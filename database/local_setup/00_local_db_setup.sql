CREATE DATABASE IF NOT EXISTS skill_matrix_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'skill_matrix_user'@'localhost'
IDENTIFIED BY 'SkillMatrix@123';

CREATE USER IF NOT EXISTS 'skill_matrix_user'@'%'
IDENTIFIED BY 'SkillMatrix@123';

GRANT ALL PRIVILEGES ON skill_matrix_db.*
TO 'skill_matrix_user'@'localhost';

GRANT ALL PRIVILEGES ON skill_matrix_db.*
TO 'skill_matrix_user'@'%';

FLUSH PRIVILEGES;