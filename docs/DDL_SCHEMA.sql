-- ============================================================
-- Skill Matrix Platform — Complete DDL Schema (Reference)
-- Version     : 1.0
-- Date        : 30 July 2026
-- Database    : MySQL 8.x  |  Charset: utf8mb4_unicode_ci
-- Note        : Authoritative copy lives in Flyway migrations
--               backend/src/main/resources/db/migration/
-- Tables      : 30 tables across M02–M16
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 01. lookup_type  (M02)
-- ============================================================
CREATE TABLE IF NOT EXISTS lookup_type (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    public_id      VARCHAR(36)  NOT NULL,
    type_code      VARCHAR(100) NOT NULL,
    type_name      VARCHAR(200) NOT NULL,
    description    TEXT,
    is_system      TINYINT(1)   NOT NULL DEFAULT 0,
    display_order  INT          NOT NULL DEFAULT 0,
    is_active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    CONSTRAINT pk_lookup_type       PRIMARY KEY (id),
    CONSTRAINT uq_lookup_type_pubid UNIQUE (public_id),
    CONSTRAINT uq_lookup_type_code  UNIQUE (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 02. lookup_value  (M02)
-- ============================================================
CREATE TABLE IF NOT EXISTS lookup_value (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    public_id      VARCHAR(36)  NOT NULL,
    lookup_type_id BIGINT       NOT NULL,
    value_code     VARCHAR(100) NOT NULL,
    value_label    VARCHAR(200) NOT NULL,
    description    TEXT,
    display_order  INT          NOT NULL DEFAULT 0,
    is_system      TINYINT(1)   NOT NULL DEFAULT 0,
    is_active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    CONSTRAINT pk_lookup_value            PRIMARY KEY (id),
    CONSTRAINT uq_lookup_value_pubid      UNIQUE (public_id),
    CONSTRAINT uq_lookup_type_value_code  UNIQUE (lookup_type_id, value_code),
    CONSTRAINT fk_lv_lookup_type          FOREIGN KEY (lookup_type_id) REFERENCES lookup_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 03. accounts  (M03)
-- ============================================================
CREATE TABLE IF NOT EXISTS accounts (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    public_id     VARCHAR(36)  NOT NULL,
    account_code  VARCHAR(50)  NOT NULL,
    account_name  VARCHAR(200) NOT NULL,
    description   TEXT,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    CONSTRAINT pk_accounts       PRIMARY KEY (id),
    CONSTRAINT uq_accounts_pubid UNIQUE (public_id),
    CONSTRAINT uq_accounts_code  UNIQUE (account_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 04. application_types  (M03)
-- ============================================================
CREATE TABLE IF NOT EXISTS application_types (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    public_id        VARCHAR(36)  NOT NULL,
    type_code        VARCHAR(50)  NOT NULL,
    type_name        VARCHAR(100) NOT NULL,
    description      TEXT,
    status_lookup_id BIGINT,
    is_active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT pk_application_types        PRIMARY KEY (id),
    CONSTRAINT uq_application_types_pubid  UNIQUE (public_id),
    CONSTRAINT uq_application_types_code   UNIQUE (type_code),
    CONSTRAINT fk_at_status_lookup         FOREIGN KEY (status_lookup_id) REFERENCES lookup_value (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 05. bundles  (M03)
-- ============================================================
CREATE TABLE IF NOT EXISTS bundles (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    public_id        VARCHAR(36)  NOT NULL,
    bundle_code      VARCHAR(50)  NOT NULL,
    bundle_name      VARCHAR(100) NOT NULL,
    description      TEXT,
    status_lookup_id BIGINT,
    is_active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT pk_bundles               PRIMARY KEY (id),
    CONSTRAINT uq_bundles_pubid         UNIQUE (public_id),
    CONSTRAINT uq_bundles_code          UNIQUE (bundle_code),
    CONSTRAINT fk_bundles_status_lookup FOREIGN KEY (status_lookup_id) REFERENCES lookup_value (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 06. application_portfolios  (M03)
-- ============================================================
CREATE TABLE IF NOT EXISTS application_portfolios (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    public_id           VARCHAR(36)  NOT NULL,
    account_id          BIGINT       NOT NULL,
    application_type_id BIGINT       NOT NULL,
    bundle_id           BIGINT       NOT NULL,
    portfolio_code      VARCHAR(100) NOT NULL,
    portfolio_name      VARCHAR(200) NOT NULL,
    description         TEXT,
    status_lookup_id    BIGINT,
    is_active           TINYINT(1)   NOT NULL DEFAULT 1,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    CONSTRAINT pk_application_portfolios          PRIMARY KEY (id),
    CONSTRAINT uq_application_portfolios_pubid    UNIQUE (public_id),
    CONSTRAINT uq_portfolio_acct_type_bundle      UNIQUE (account_id, application_type_id, bundle_id),
    CONSTRAINT fk_ap_account                      FOREIGN KEY (account_id)          REFERENCES accounts (id),
    CONSTRAINT fk_ap_application_type             FOREIGN KEY (application_type_id) REFERENCES application_types (id),
    CONSTRAINT fk_ap_bundle                       FOREIGN KEY (bundle_id)           REFERENCES bundles (id),
    CONSTRAINT fk_ap_status_lookup                FOREIGN KEY (status_lookup_id)    REFERENCES lookup_value (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 07. applications  (M03)
-- ============================================================
CREATE TABLE IF NOT EXISTS applications (
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    public_id                  VARCHAR(36)  NOT NULL,
    portfolio_id               BIGINT       NOT NULL,
    application_code           VARCHAR(100) NOT NULL,
    application_name           VARCHAR(200) NOT NULL,
    description                TEXT,
    lifecycle_status_lookup_id BIGINT,
    replacement_application_id BIGINT,
    remarks                    TEXT,
    is_active                  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                 VARCHAR(100),
    updated_by                 VARCHAR(100),
    CONSTRAINT pk_applications              PRIMARY KEY (id),
    CONSTRAINT uq_applications_pubid        UNIQUE (public_id),
    CONSTRAINT uq_app_code_per_portfolio    UNIQUE (portfolio_id, application_code),
    CONSTRAINT fk_app_portfolio             FOREIGN KEY (portfolio_id)               REFERENCES application_portfolios (id),
    CONSTRAINT fk_app_lifecycle_status      FOREIGN KEY (lifecycle_status_lookup_id) REFERENCES lookup_value (id),
    CONSTRAINT fk_app_replacement           FOREIGN KEY (replacement_application_id) REFERENCES applications (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 08. roles  (M04)
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    public_id   VARCHAR(36)  NOT NULL,
    role_code   VARCHAR(50)  NOT NULL,
    role_name   VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    CONSTRAINT pk_roles       PRIMARY KEY (id),
    CONSTRAINT uq_roles_pubid UNIQUE (public_id),
    CONSTRAINT uq_roles_code  UNIQUE (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 09. permissions  (M04)
-- ============================================================
CREATE TABLE IF NOT EXISTS permissions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    public_id       VARCHAR(36)  NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(200) NOT NULL,
    module          VARCHAR(100),
    description     TEXT,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_permissions        PRIMARY KEY (id),
    CONSTRAINT uq_permissions_pubid  UNIQUE (public_id),
    CONSTRAINT uq_permissions_code   UNIQUE (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 10. role_permissions  (M04)
-- ============================================================
CREATE TABLE IF NOT EXISTS role_permissions (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    role_id       BIGINT   NOT NULL,
    permission_id BIGINT   NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(100),
    CONSTRAINT pk_role_permissions  PRIMARY KEY (id),
    CONSTRAINT uq_role_permission   UNIQUE (role_id, permission_id),
    CONSTRAINT fk_rp_role           FOREIGN KEY (role_id)       REFERENCES roles (id),
    CONSTRAINT fk_rp_permission     FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11. teams  (M04)
-- ============================================================
CREATE TABLE IF NOT EXISTS teams (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    public_id   VARCHAR(36)  NOT NULL,
    team_code   VARCHAR(50)  NOT NULL,
    team_name   VARCHAR(200) NOT NULL,
    description TEXT,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    CONSTRAINT pk_teams       PRIMARY KEY (id),
    CONSTRAINT uq_teams_pubid UNIQUE (public_id),
    CONSTRAINT uq_teams_code  UNIQUE (team_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 12. users  (M04)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    public_id             VARCHAR(36)  NOT NULL,
    username              VARCHAR(100) NOT NULL,
    email                 VARCHAR(200) NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    full_name             VARCHAR(200) NOT NULL,
    employee_id           VARCHAR(50),
    manager_id            BIGINT,
    primary_role_id       BIGINT,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    account_locked        TINYINT(1)   NOT NULL DEFAULT 0,
    lock_time             DATETIME,
    last_login            DATETIME,
    is_active             TINYINT(1)   NOT NULL DEFAULT 1,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    CONSTRAINT pk_users               PRIMARY KEY (id),
    CONSTRAINT uq_users_pubid         UNIQUE (public_id),
    CONSTRAINT uq_users_username      UNIQUE (username),
    CONSTRAINT uq_users_email         UNIQUE (email),
    CONSTRAINT fk_users_manager       FOREIGN KEY (manager_id)      REFERENCES users (id),
    CONSTRAINT fk_users_primary_role  FOREIGN KEY (primary_role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 13. user_roles  (M04)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_roles (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    role_id     BIGINT      NOT NULL,
    assigned_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(100),
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT pk_user_roles  PRIMARY KEY (id),
    CONSTRAINT uq_user_role   UNIQUE (user_id, role_id),
    CONSTRAINT fk_ur_user     FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ur_role     FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 14. user_teams  (M04)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_teams (
    id        BIGINT     NOT NULL AUTO_INCREMENT,
    user_id   BIGINT     NOT NULL,
    team_id   BIGINT     NOT NULL,
    joined_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT pk_user_teams  PRIMARY KEY (id),
    CONSTRAINT uq_user_team   UNIQUE (user_id, team_id),
    CONSTRAINT fk_ut_user     FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ut_team     FOREIGN KEY (team_id) REFERENCES teams (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 15. user_application_mapping  (M05)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_application_mapping (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    public_id             VARCHAR(36)  NOT NULL,
    user_id               BIGINT       NOT NULL,
    application_id        BIGINT       NOT NULL,
    role_on_app_lookup_id BIGINT,
    allocation_percentage DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    effective_from        DATE         NOT NULL,
    effective_to          DATE,
    is_active             TINYINT(1)   NOT NULL DEFAULT 1,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    CONSTRAINT pk_user_app_mapping       PRIMARY KEY (id),
    CONSTRAINT uq_user_app_mapping_pubid UNIQUE (public_id),
    CONSTRAINT fk_uam_user               FOREIGN KEY (user_id)               REFERENCES users (id),
    CONSTRAINT fk_uam_application        FOREIGN KEY (application_id)        REFERENCES applications (id),
    CONSTRAINT fk_uam_role_on_app        FOREIGN KEY (role_on_app_lookup_id) REFERENCES lookup_value (id),
    CONSTRAINT chk_uam_allocation        CHECK (allocation_percentage BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 16. skill_categories  (M06)
-- ============================================================
CREATE TABLE IF NOT EXISTS skill_categories (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    public_id     VARCHAR(36)  NOT NULL,
    category_code VARCHAR(50)  NOT NULL,
    category_name VARCHAR(200) NOT NULL,
    description   TEXT,
    display_order INT          NOT NULL DEFAULT 0,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    CONSTRAINT pk_skill_categories       PRIMARY KEY (id),
    CONSTRAINT uq_skill_categories_pubid UNIQUE (public_id),
    CONSTRAINT uq_skill_category_code    UNIQUE (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 17. skills  (M06)
-- ============================================================
CREATE TABLE IF NOT EXISTS skills (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    public_id             VARCHAR(36)  NOT NULL,
    skill_category_id     BIGINT       NOT NULL,
    skill_code            VARCHAR(100) NOT NULL,
    skill_name            VARCHAR(200) NOT NULL,
    description           TEXT,
    skill_type_lookup_id  BIGINT,
    skill_scope_lookup_id BIGINT,
    is_active             TINYINT(1)   NOT NULL DEFAULT 1,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    CONSTRAINT pk_skills               PRIMARY KEY (id),
    CONSTRAINT uq_skills_pubid         UNIQUE (public_id),
    CONSTRAINT uq_skills_code          UNIQUE (skill_code),
    CONSTRAINT fk_skills_category      FOREIGN KEY (skill_category_id)     REFERENCES skill_categories (id),
    CONSTRAINT fk_skills_type_lookup   FOREIGN KEY (skill_type_lookup_id)  REFERENCES lookup_value (id),
    CONSTRAINT fk_skills_scope_lookup  FOREIGN KEY (skill_scope_lookup_id) REFERENCES lookup_value (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 18. rating_scale  (M06)
-- ============================================================
CREATE TABLE IF NOT EXISTS rating_scale (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    public_id     VARCHAR(36)  NOT NULL,
    level_value   INT          NOT NULL,
    level_name    VARCHAR(100) NOT NULL,
    level_meaning TEXT,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_rating_scale       PRIMARY KEY (id),
    CONSTRAINT uq_rating_scale_pubid UNIQUE (public_id),
    CONSTRAINT uq_rating_scale_level UNIQUE (level_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 19. requirement_versions  (M07)
-- ============================================================
CREATE TABLE IF NOT EXISTS requirement_versions (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    public_id        VARCHAR(36)  NOT NULL,
    application_id   BIGINT       NOT NULL,
    version_code     VARCHAR(100) NOT NULL,
    version_name     VARCHAR(200) NOT NULL,
    description      TEXT,
    status_lookup_id BIGINT,
    rejection_reason TEXT,
    published_at     DATETIME,
    approved_at      DATETIME,
    approved_by      VARCHAR(100),
    is_active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT pk_requirement_versions          PRIMARY KEY (id),
    CONSTRAINT uq_requirement_versions_pubid    UNIQUE (public_id),
    CONSTRAINT uq_req_version_per_application   UNIQUE (application_id, version_code),
    CONSTRAINT fk_rv_application                FOREIGN KEY (application_id)  REFERENCES applications (id),
    CONSTRAINT fk_rv_status_lookup              FOREIGN KEY (status_lookup_id) REFERENCES lookup_value (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 20. expected_ratings  (M07)
-- ============================================================
CREATE TABLE IF NOT EXISTS expected_ratings (
    id                     BIGINT     NOT NULL AUTO_INCREMENT,
    public_id              VARCHAR(36) NOT NULL,
    requirement_version_id BIGINT     NOT NULL,
    skill_id               BIGINT     NOT NULL,
    expected_level         INT        NOT NULL,
    criticality_lookup_id  BIGINT,
    min_people_required    INT        NOT NULL DEFAULT 1,
    is_mandatory           TINYINT(1) NOT NULL DEFAULT 1,
    is_active              TINYINT(1) NOT NULL DEFAULT 1,
    created_at             DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    CONSTRAINT pk_expected_ratings               PRIMARY KEY (id),
    CONSTRAINT uq_expected_ratings_pubid         UNIQUE (public_id),
    CONSTRAINT uq_expected_rating_version_skill  UNIQUE (requirement_version_id, skill_id),
    CONSTRAINT fk_er_requirement_version         FOREIGN KEY (requirement_version_id) REFERENCES requirement_versions (id),
    CONSTRAINT fk_er_skill                       FOREIGN KEY (skill_id)               REFERENCES skills (id),
    CONSTRAINT fk_er_criticality_lookup          FOREIGN KEY (criticality_lookup_id)  REFERENCES lookup_value (id),
    CONSTRAINT chk_expected_level                CHECK (expected_level BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 21. assessment_cycles  (M08)
-- ============================================================
CREATE TABLE IF NOT EXISTS assessment_cycles (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    public_id              VARCHAR(36)  NOT NULL,
    application_id         BIGINT       NOT NULL,
    requirement_version_id BIGINT       NOT NULL,
    cycle_name             VARCHAR(100) NOT NULL,
    cycle_start_date       DATE         NOT NULL,
    cycle_end_date         DATE         NOT NULL,
    status_lookup_id       BIGINT,
    is_active              TINYINT(1)   NOT NULL DEFAULT 1,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    CONSTRAINT pk_assessment_cycles           PRIMARY KEY (id),
    CONSTRAINT uq_assessment_cycles_pubid     UNIQUE (public_id),
    CONSTRAINT uq_cycle_name_per_application  UNIQUE (application_id, cycle_name),
    CONSTRAINT fk_ac_application              FOREIGN KEY (application_id)         REFERENCES applications (id),
    CONSTRAINT fk_ac_requirement_version      FOREIGN KEY (requirement_version_id) REFERENCES requirement_versions (id),
    CONSTRAINT fk_ac_status_lookup            FOREIGN KEY (status_lookup_id)       REFERENCES lookup_value (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 22. technician_assessments  (M09)
-- ============================================================
CREATE TABLE IF NOT EXISTS technician_assessments (
    id                 BIGINT     NOT NULL AUTO_INCREMENT,
    public_id          VARCHAR(36) NOT NULL,
    cycle_id           BIGINT     NOT NULL,
    user_id            BIGINT     NOT NULL,
    expected_rating_id BIGINT     NOT NULL,
    self_rating        INT,
    evidence           TEXT,
    technician_comment TEXT,
    status_lookup_id   BIGINT,
    submitted_at       DATETIME,
    is_active          TINYINT(1) NOT NULL DEFAULT 1,
    created_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    CONSTRAINT pk_technician_assessments       PRIMARY KEY (id),
    CONSTRAINT uq_technician_assessments_pubid UNIQUE (public_id),
    CONSTRAINT uq_assessment_cycle_user_skill  UNIQUE (cycle_id, user_id, expected_rating_id),
    CONSTRAINT fk_ta_cycle                     FOREIGN KEY (cycle_id)           REFERENCES assessment_cycles (id),
    CONSTRAINT fk_ta_user                      FOREIGN KEY (user_id)            REFERENCES users (id),
    CONSTRAINT fk_ta_expected_rating           FOREIGN KEY (expected_rating_id) REFERENCES expected_ratings (id),
    CONSTRAINT fk_ta_status_lookup             FOREIGN KEY (status_lookup_id)   REFERENCES lookup_value (id),
    CONSTRAINT chk_self_rating                 CHECK (self_rating IS NULL OR self_rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 23. lead_reviews  (M10)
-- ============================================================
CREATE TABLE IF NOT EXISTS lead_reviews (
    id                       BIGINT     NOT NULL AUTO_INCREMENT,
    public_id                VARCHAR(36) NOT NULL,
    technician_assessment_id BIGINT     NOT NULL,
    reviewer_user_id         BIGINT     NOT NULL,
    manager_rating           INT,
    final_rating             INT,
    row_decision_lookup_id   BIGINT,
    manager_comment          TEXT,
    reviewed_at              DATETIME,
    is_active                TINYINT(1) NOT NULL DEFAULT 1,
    created_at               DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100),
    CONSTRAINT pk_lead_reviews               PRIMARY KEY (id),
    CONSTRAINT uq_lead_reviews_pubid         UNIQUE (public_id),
    CONSTRAINT uq_lead_review_per_assessment UNIQUE (technician_assessment_id),
    CONSTRAINT fk_lr_technician_assessment   FOREIGN KEY (technician_assessment_id) REFERENCES technician_assessments (id),
    CONSTRAINT fk_lr_reviewer                FOREIGN KEY (reviewer_user_id)         REFERENCES users (id),
    CONSTRAINT fk_lr_row_decision_lookup     FOREIGN KEY (row_decision_lookup_id)   REFERENCES lookup_value (id),
    CONSTRAINT chk_manager_rating            CHECK (manager_rating IS NULL OR manager_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_final_rating              CHECK (final_rating IS NULL OR final_rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 24. assessment_approvals  (M10)
-- ============================================================
CREATE TABLE IF NOT EXISTS assessment_approvals (
    id                         BIGINT     NOT NULL AUTO_INCREMENT,
    public_id                  VARCHAR(36) NOT NULL,
    cycle_id                   BIGINT     NOT NULL,
    technician_user_id         BIGINT     NOT NULL,
    reviewer_user_id           BIGINT     NOT NULL,
    overall_decision_lookup_id BIGINT,
    decision_note              TEXT,
    approved_at                DATETIME,
    is_active                  TINYINT(1) NOT NULL DEFAULT 1,
    created_at                 DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                 VARCHAR(100),
    updated_by                 VARCHAR(100),
    CONSTRAINT pk_assessment_approvals           PRIMARY KEY (id),
    CONSTRAINT uq_assessment_approvals_pubid     UNIQUE (public_id),
    CONSTRAINT fk_aa_cycle                       FOREIGN KEY (cycle_id)                  REFERENCES assessment_cycles (id),
    CONSTRAINT fk_aa_technician                  FOREIGN KEY (technician_user_id)        REFERENCES users (id),
    CONSTRAINT fk_aa_reviewer                    FOREIGN KEY (reviewer_user_id)          REFERENCES users (id),
    CONSTRAINT fk_aa_overall_decision_lookup     FOREIGN KEY (overall_decision_lookup_id) REFERENCES lookup_value (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 25. skill_gap_snapshots  (M11)
-- ============================================================
CREATE TABLE IF NOT EXISTS skill_gap_snapshots (
    id                     BIGINT     NOT NULL AUTO_INCREMENT,
    public_id              VARCHAR(36) NOT NULL,
    assessment_approval_id BIGINT     NOT NULL,
    user_id                BIGINT     NOT NULL,
    skill_id               BIGINT     NOT NULL,
    application_id         BIGINT     NOT NULL,
    cycle_id               BIGINT     NOT NULL,
    expected_level         INT        NOT NULL,
    final_approved_rating  INT        NOT NULL,
    gap_value              INT        NOT NULL,
    severity_lookup_id     BIGINT,
    criticality_lookup_id  BIGINT,
    snapshot_date          DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active              TINYINT(1) NOT NULL DEFAULT 1,
    created_at             DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_skill_gap_snapshots       PRIMARY KEY (id),
    CONSTRAINT uq_skill_gap_snapshots_pubid UNIQUE (public_id),
    CONSTRAINT fk_sgs_assessment_approval   FOREIGN KEY (assessment_approval_id) REFERENCES assessment_approvals (id),
    CONSTRAINT fk_sgs_user                  FOREIGN KEY (user_id)               REFERENCES users (id),
    CONSTRAINT fk_sgs_skill                 FOREIGN KEY (skill_id)              REFERENCES skills (id),
    CONSTRAINT fk_sgs_application           FOREIGN KEY (application_id)        REFERENCES applications (id),
    CONSTRAINT fk_sgs_cycle                 FOREIGN KEY (cycle_id)              REFERENCES assessment_cycles (id),
    CONSTRAINT fk_sgs_severity_lookup       FOREIGN KEY (severity_lookup_id)    REFERENCES lookup_value (id),
    CONSTRAINT fk_sgs_criticality_lookup    FOREIGN KEY (criticality_lookup_id) REFERENCES lookup_value (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 26. training_recommendations  (M12)
-- ============================================================
CREATE TABLE IF NOT EXISTS training_recommendations (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    public_id               VARCHAR(36)  NOT NULL,
    gap_snapshot_id         BIGINT       NOT NULL,
    skill_id                BIGINT       NOT NULL,
    application_id          BIGINT       NOT NULL,
    training_type_lookup_id BIGINT,
    priority_lookup_id      BIGINT,
    target_date             DATE,
    status_lookup_id        BIGINT,
    confirmed_at            DATETIME,
    confirmed_by            VARCHAR(100),
    notes                   TEXT,
    is_active               TINYINT(1)   NOT NULL DEFAULT 1,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    CONSTRAINT pk_training_recommendations          PRIMARY KEY (id),
    CONSTRAINT uq_training_recommendations_pubid    UNIQUE (public_id),
    CONSTRAINT fk_tr_gap_snapshot                   FOREIGN KEY (gap_snapshot_id)         REFERENCES skill_gap_snapshots (id),
    CONSTRAINT fk_tr_skill                          FOREIGN KEY (skill_id)                REFERENCES skills (id),
    CONSTRAINT fk_tr_application                    FOREIGN KEY (application_id)          REFERENCES applications (id),
    CONSTRAINT fk_tr_training_type_lookup           FOREIGN KEY (training_type_lookup_id) REFERENCES lookup_value (id),
    CONSTRAINT fk_tr_priority_lookup                FOREIGN KEY (priority_lookup_id)      REFERENCES lookup_value (id),
    CONSTRAINT fk_tr_status_lookup                  FOREIGN KEY (status_lookup_id)        REFERENCES lookup_value (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 27. training_participants  (M12)
-- ============================================================
CREATE TABLE IF NOT EXISTS training_participants (
    id                         BIGINT      NOT NULL AUTO_INCREMENT,
    public_id                  VARCHAR(36) NOT NULL,
    training_recommendation_id BIGINT      NOT NULL,
    user_id                    BIGINT      NOT NULL,
    enrolled_at                DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active                  TINYINT(1)  NOT NULL DEFAULT 1,
    CONSTRAINT pk_training_participants          PRIMARY KEY (id),
    CONSTRAINT uq_training_participants_pubid    UNIQUE (public_id),
    CONSTRAINT uq_training_participant_per_rec   UNIQUE (training_recommendation_id, user_id),
    CONSTRAINT fk_tp_training_recommendation     FOREIGN KEY (training_recommendation_id) REFERENCES training_recommendations (id),
    CONSTRAINT fk_tp_user                        FOREIGN KEY (user_id)                    REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 28. audit_log  (M15)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    public_id      VARCHAR(36)  NOT NULL,
    actor_user_id  BIGINT,
    actor_username VARCHAR(100),
    action         VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100) NOT NULL,
    entity_id      VARCHAR(100),
    entity_pubid   VARCHAR(36),
    old_value      LONGTEXT,
    new_value      LONGTEXT,
    ip_address     VARCHAR(45),
    correlation_id VARCHAR(36),
    occurred_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_log        PRIMARY KEY (id),
    CONSTRAINT uq_audit_log_pubid  UNIQUE (public_id),
    INDEX idx_audit_actor          (actor_user_id),
    INDEX idx_audit_entity         (entity_type, entity_pubid),
    INDEX idx_audit_occurred_at    (occurred_at),
    INDEX idx_audit_action         (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 29. notification_log  (M16)
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_log (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    public_id         VARCHAR(36)  NOT NULL,
    recipient_user_id BIGINT       NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    channel_lookup_id BIGINT,
    status_lookup_id  BIGINT,
    subject           VARCHAR(500),
    message_body      TEXT,
    entity_type       VARCHAR(100),
    entity_pubid      VARCHAR(36),
    sent_at           DATETIME,
    read_at           DATETIME,
    retry_count       INT          NOT NULL DEFAULT 0,
    error_message     TEXT,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_notification_log          PRIMARY KEY (id),
    CONSTRAINT uq_notification_log_pubid    UNIQUE (public_id),
    CONSTRAINT fk_nl_recipient_user         FOREIGN KEY (recipient_user_id) REFERENCES users (id),
    CONSTRAINT fk_nl_channel_lookup         FOREIGN KEY (channel_lookup_id) REFERENCES lookup_value (id),
    CONSTRAINT fk_nl_status_lookup          FOREIGN KEY (status_lookup_id)  REFERENCES lookup_value (id),
    INDEX idx_notification_recipient        (recipient_user_id),
    INDEX idx_notification_created_at       (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 30. import_export_history  (M14)
-- ============================================================
CREATE TABLE IF NOT EXISTS import_export_history (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    public_id        VARCHAR(36)  NOT NULL,
    operation_type   VARCHAR(20)  NOT NULL COMMENT 'IMPORT or EXPORT',
    template_type    VARCHAR(100) NOT NULL,
    file_name        VARCHAR(500),
    file_path        VARCHAR(1000),
    total_rows       INT,
    success_rows     INT,
    failed_rows      INT,
    status_lookup_id BIGINT,
    error_summary    TEXT,
    initiated_by     BIGINT       NOT NULL,
    started_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at     DATETIME,
    CONSTRAINT pk_import_export_history        PRIMARY KEY (id),
    CONSTRAINT uq_import_export_history_pubid  UNIQUE (public_id),
    CONSTRAINT fk_ieh_initiated_by             FOREIGN KEY (initiated_by)     REFERENCES users (id),
    CONSTRAINT fk_ieh_status_lookup            FOREIGN KEY (status_lookup_id) REFERENCES lookup_value (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;
-- ============================================================
-- End of DDL — 30 tables created
-- ============================================================
