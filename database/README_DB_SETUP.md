# Skill Matrix Platform — Database Setup Guide

**Database:** MySQL 8.x  
**Character Set:** utf8mb4 / utf8mb4_unicode_ci  
**Schema:** `skill_matrix_db`  
**Tables:** 31 (30 domain tables + `refresh_tokens`)

---

## Migration File Overview

| File | Location | Runs In | Content |
|---|---|---|---|
| `V01__initial_schema.sql` | `db/migration/` | ALL | All 31 tables (30 + refresh_tokens via V04) |
| `V02__seed_master_reference_data.sql` | `db/migration/` | ALL | Lookup types/values, rating scale, roles, permissions, role_permissions, skill categories, accounts, application types, bundles |
| `V03__seed_demo_data_dev_only.sql` | `db/dev-migration/` | DEV only | Demo portfolios, applications, users, teams, skills, assessment cycle, assessments, reviews, approvals, gap snapshots, training, audit log, notifications |
| `V04__add_refresh_tokens.sql` | `db/migration/` | ALL | `refresh_tokens` table |

**Production runs V01, V02, V04 only.**  
**DEV additionally runs V03 (loaded via `application-dev.yml` Flyway locations).**

---

## 1. Local Development Setup (Step-by-Step)

### Step 1: Create the Database

```bash
mysql -u root -p < database/local_setup/00_create_database.sql
```

### Step 2: Create the Application User

```bash
mysql -u root -p < database/local_setup/01_create_local_user.sql
```

Creates:
- **User:** `skillmatrix_user`
- **Password:** `skillmatrix_pass` *(change for QA/PROD)*
- **Hosts:** `localhost` and `%` (Docker)
- **Grants:** Full access to `skill_matrix_db`

### Step 3: Verify Connection

```bash
mysql -u skillmatrix_user -p skill_matrix_db
```

### Step 4: Run Flyway Migrations (via Spring Boot)

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway automatically runs in order:

```
db/migration/V01__initial_schema.sql
db/migration/V02__seed_master_reference_data.sql
db/migration/V04__add_refresh_tokens.sql
db/dev-migration/V03__seed_demo_data_dev_only.sql
```

### Step 5: Verify Migration

```bash
mysql -u skillmatrix_user -p skill_matrix_db -e \
  "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Expected:

```
+---------+-------------------------------+---------------------+---------+
| version | description                   | installed_on        | success |
+---------+-------------------------------+---------------------+---------+
| 01      | initial schema                | 2026-xx-xx xx:xx:xx | 1       |
| 02      | seed master reference data    | 2026-xx-xx xx:xx:xx | 1       |
| 03      | seed demo data dev only       | 2026-xx-xx xx:xx:xx | 1       |
| 04      | add refresh tokens            | 2026-xx-xx xx:xx:xx | 1       |
+---------+-------------------------------+---------------------+---------+
```

---

## 2. Demo Credentials (DEV only)

| Username | Password | Role |
|---|---|---|
| `admin` | `Password@123` | Administrator |
| `lead_manager` | `Password@123` | Lead Manager |
| `tech_john` | `Password@123` | Technician |
| `tech_jane` | `Password@123` | Technician |
| `tech_mike` | `Password@123` | Technician |

Passwords are BCrypt-hashed (cost 12). Raw passwords are never stored.

---

## 3. Master Reference Data (V02)

Seeded in all environments:

| Category | Count | Examples |
|---|---|---|
| Lookup types | 20 | APPLICATION_LIFECYCLE_STATUS, ASSESSMENT_STATUS, GAP_SEVERITY … |
| Lookup values | ~57 | ACTIVE, DRAFT, SUBMITTED, APPROVED, HIGH, MEDIUM, LOW … |
| Rating scale | 5 | 1=Awareness → 5=SME/Expert |
| Roles | 3 | ADMIN, LEAD_MANAGER, TECHNICIAN |
| Permissions | 26 | USER_VIEW, ASSESSMENT_SUBMIT, REPORT_VIEW_ALL … |
| Role-permissions | ~48 | ADMIN=all, LEAD_MANAGER=15, TECHNICIAN=7 |
| Skill categories | 10 | APP_KNOWLEDGE, TECHNICAL, SUPPORT_PROCESS, DATABASE … |
| Accounts | 1 | NTT_DATA (NTT DATA Germany) |
| Application types | 5 | WEB, HOST, BATCH, API, MOBILE |
| Bundles | 3 | B06, B12, B20 |

---

## 4. Demo Data (V03 — DEV only)

Full end-to-end demo flow for ATLAS-deZentral:

| Entity | Count | Notes |
|---|---|---|
| Portfolios | 3 | B06-WEB, B06-HOST, B12-WEB |
| Applications | 5 | ATLAS-deZentral (pilot), AVUS, BEPPO, HOST_APP_01, HOST_APP_02 |
| Users | 5 | admin, lead_manager, tech_john, tech_jane, tech_mike |
| Teams | 3 | ATLAS_TEAM, AVUS_TEAM, HOST_TEAM |
| Skills | 8 | ATLAS_APP_KNOWLEDGE, INCIDENT_MANAGEMENT, SQL_QUERY … |
| User-app mappings | 4 | tech_john (100%), tech_jane (80%), tech_mike (AVUS), lead_manager |
| Requirement version | 1 | ATLAS v1.0 (APPROVED) |
| Expected ratings | 8 | All 8 skills with expected levels 2-4 |
| Assessment cycle | 1 | ATLAS Q3 2026 (OPEN) |
| Technician assessments | 8 | tech_john self-rated all 8 skills |
| Lead reviews | 8 | lead_manager reviewed all — 4 gaps, 4 approved |
| Assessment approval | 1 | tech_john APPROVED |
| Gap snapshots | 4 | ATLAS_APP_KNOWLEDGE, ATLAS_CONFIGURATION, SQL_QUERY, ATLAS_DEPLOYMENT |
| Training recommendations | 4 | One per gap skill |
| Training participants | 4 | tech_john enrolled in all training |
| Audit log | 8 | Login, submit, review, approve, generate, create events |
| Notification log | 4 | Cycle open, assessment approved, training recommended |
| Import/export history | 2 | 1 import, 1 export |

---

## 5. Docker Local Setup

```bash
docker-compose up -d
docker-compose logs mysql
docker exec -it skill_matrix_mysql mysql -u skillmatrix_user -p skill_matrix_db
```

---

## 6. QA / Production Setup

1. Use environment variables for all connection details
2. Store credentials in AWS Secrets Manager / Vault
3. Application user should have DML-only privileges in PROD (no DDL)
4. Flyway `validate` mode in PROD — migrations applied manually with DBA approval
5. **Never run V03 (dev-migration) in QA or PROD**
6. `application-dev.yml` Flyway locations must NOT be active in PROD

### Required Environment Variables

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/skill_matrix_db?useSSL=true
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
SPRING_PROFILES_ACTIVE=prod
```

---

## 7. Drop and Recreate (Dev Only)

> **WARNING: Destroys all data. Local dev only.**

```bash
mysql -u root -p < database/local_setup/02_drop_database.sql
mysql -u root -p < database/local_setup/00_create_database.sql
mysql -u root -p < database/local_setup/01_create_local_user.sql
# Then restart Spring Boot — Flyway re-runs all migrations automatically
```

---

## 8. Flyway Rules

| Rule | Detail |
|---|---|
| Never modify a deployed migration | Create a new Vxx file instead |
| Version format | V01, V02, V03, V04 (zero-padded) |
| Dev-only seeds | `db/dev-migration/` — loaded via `application-dev.yml` only |
| Production migrations | Must be reviewed and approved by DBA |
| Checksum validation | Flyway validates checksums on every startup |
| Repair | `mvn flyway:repair` if checksum mismatch after a forced fix |

---

## 9. Database Connection Summary

| Environment | Host | Port | Schema | User |
|---|---|---|---|---|
| DEV (local) | localhost | 3306 | skill_matrix_db | skillmatrix_user |
| DEV (docker) | mysql (container) | 3306 | skill_matrix_db | skillmatrix_user |
| QA | RDS endpoint (env var) | 3306 | skill_matrix_db | Via secrets manager |
| PROD | RDS endpoint (env var) | 3306 | skill_matrix_db | Via secrets manager |

---

## 10. Verification

```bash
mysql -u skillmatrix_user -p skill_matrix_db < database/verification_queries.sql
```

Runs 21 count queries plus data integrity checks. See `database/verification_queries.sql`.

---

## 11. DDL Schema Observations / Design Notes

| # | Note |
|---|---|
| 1 | `GAP_SEVERITY` lookup type added in V02 (not in original DDL reference) — required by `skill_gap_snapshots.severity_lookup_id` |
| 2 | `ASSESSMENT_CYCLE_STATUS` lookup type added in V02 — required by `assessment_cycles.status_lookup_id` |
| 3 | `TRAINING_STATUS` lookup type added in V02 — required by `training_recommendations.status_lookup_id` |
| 4 | `users` table has no `must_change_password` column — BCrypt hashing is the only password security mechanism in Phase 1 |
| 5 | `refresh_tokens` table (V04) uses `created_by BIGINT` with no FK — intentional to avoid circular bootstrap dependency |
| 6 | `application_portfolios` has `UNIQUE (account_id, application_type_id, bundle_id)` — one portfolio per account+type+bundle combination |
| 7 | Demo password `Password@123` BCrypt hash: `$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lLqy` |
