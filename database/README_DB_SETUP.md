# Skill Matrix Platform — Database Setup Guide

**Database:** MySQL 8.x  
**Character Set:** utf8mb4 / utf8mb4_unicode_ci  
**Schema:** `skill_matrix_db`  

---

## Prerequisites

- MySQL 8.x installed and running
- MySQL client (CLI or MySQL Workbench)
- Admin access to MySQL (`root` or equivalent)

---

## 1. Local Development Setup (Step-by-Step)

### Step 1: Create the Database

Run the following script as MySQL root:

```bash
mysql -u root -p < database/local_setup/00_create_database.sql
```

### Step 2: Create the Application User

```bash
mysql -u root -p < database/local_setup/01_create_local_user.sql
```

This creates:
- **User:** `skillmatrix_user`
- **Password:** `skillmatrix_pass` *(change for QA/PROD)*
- **Host:** `localhost`
- **Grants:** Full access to `skill_matrix_db`

### Step 3: Verify Connection

```bash
mysql -u skillmatrix_user -p skill_matrix_db
```

Expected output:
```
Welcome to the MySQL monitor. Commands end with ; or \g.
mysql> show tables;
Empty set (0.00 sec)
```

### Step 4: Run Flyway Migrations (via Spring Boot)

Start the Spring Boot backend with the `dev` profile. Flyway will automatically run migrations from:

```
backend/src/main/resources/db/migration/
  V01__initial_schema.sql        ← All 30 tables
  V02__seed_master_reference_data.sql  ← Lookup types, values, roles, rating scale

backend/src/main/resources/db/dev-migration/
  V03__seed_demo_data_dev_only.sql     ← Demo account, users, applications
```

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Step 5: Verify Migration

```bash
mysql -u skillmatrix_user -p skill_matrix_db
mysql> SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;
```

Expected output:
```
+-----+-------------------------------------+---------------------+---------+
| version | description                    | installed_on        | success |
+---------+--------------------------------+---------------------+---------+
| 01      | initial schema                 | 2026-07-30 xx:xx:xx | 1       |
| 02      | seed master reference data     | 2026-07-30 xx:xx:xx | 1       |
| 03      | seed demo data dev only        | 2026-07-30 xx:xx:xx | 1       |
+---------+--------------------------------+---------------------+---------+
```

---

## 2. Docker Local Setup

If using docker-compose, the MySQL container is pre-configured:

```bash
# Start all services (MySQL + backend + frontend)
docker-compose up -d

# View MySQL logs
docker-compose logs mysql

# Connect to MySQL in container
docker exec -it skill_matrix_mysql mysql -u skillmatrix_user -p skill_matrix_db
```

The docker-compose MySQL container initialises with:
- Database: `skill_matrix_db`
- User: `skillmatrix_user` / `skillmatrix_pass`
- Port: `3306` (mapped to host `3306`)

---

## 3. QA / Production Setup

For QA and PROD environments:
1. Use environment variables for all connection details
2. Store secrets in AWS Secrets Manager
3. Application user should have only DML privileges (no DDL)
4. Flyway runs in `validate` mode in PROD (no auto-migration)
5. PROD migrations must be reviewed and applied manually with DBA approval

### Required Environment Variables
```
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/skill_matrix_db
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
```

---

## 4. Drop and Recreate (Dev Only)

> ⚠️ **WARNING: This destroys all data. Use only in local dev.**

```bash
mysql -u root -p < database/local_setup/02_drop_database.sql
mysql -u root -p < database/local_setup/00_create_database.sql
mysql -u root -p < database/local_setup/01_create_local_user.sql
# Then restart Spring Boot to re-run Flyway
```

---

## 5. Flyway Rules

| Rule | Detail |
|---|---|
| Never modify a deployed migration file | Use a new version instead |
| Version format | V01, V02, V03 (zero-padded 2 digits) |
| Dev-only seeds | Place in `db/dev-migration/` folder |
| Production migrations | Must be reviewed by DBA before apply |
| Checksum validation | Flyway validates checksums on every startup |

---

## 6. Database Connection Summary

| Environment | Host | Port | Schema | User |
|---|---|---|---|---|
| DEV (local) | localhost | 3306 | skill_matrix_db | skillmatrix_user |
| DEV (docker) | mysql (container) | 3306 | skill_matrix_db | skillmatrix_user |
| QA | RDS endpoint (env var) | 3306 | skill_matrix_db | Via AWS Secrets |
| PROD | RDS endpoint (env var) | 3306 | skill_matrix_db | Via AWS Secrets |

---

## 7. Verification

Run the verification queries to confirm data integrity after setup:

```bash
mysql -u skillmatrix_user -p skill_matrix_db < database/verification_queries.sql
```

See `database/verification_queries.sql` for full verification suite.
