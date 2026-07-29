# ERD — Entity Relationship Diagram

> **Note:** The ERD image file (`ERD.png`) must be generated using a database design tool such as:
> - MySQL Workbench (reverse-engineer from DDL_SCHEMA.sql)
> - dbdiagram.io (import DDL_SCHEMA.sql)
> - draw.io / Lucidchart (manual layout)
>
> Export the diagram as `ERD.png` and place it in this `docs/` folder.

## Entity Groups

### Group 1: Lookup / Master Reference
- `lookup_type` → `lookup_value`

### Group 2: Application Hierarchy
- `accounts` → `application_portfolios` ← `application_types`
- `application_portfolios` ← `bundles`
- `application_portfolios` → `applications`

### Group 3: Users, Roles, Teams
- `roles` ← `role_permissions` → `permissions`
- `users` ← `user_roles` → `roles`
- `users` ← `user_teams` → `teams`
- `users` self-reference (manager_id)

### Group 4: Assignment
- `users` ← `user_application_mapping` → `applications`

### Group 5: Skills
- `skill_categories` → `skills`
- `rating_scale` (standalone)

### Group 6: Requirements
- `applications` → `requirement_versions` → `expected_ratings` → `skills`

### Group 7: Assessment
- `assessment_cycles` → `technician_assessments` → `expected_ratings`
- `technician_assessments` ← `lead_reviews`
- `assessment_cycles` → `assessment_approvals`

### Group 8: Gap and Training
- `assessment_approvals` → `skill_gap_snapshots`
- `skill_gap_snapshots` → `training_recommendations` ← `training_participants` → `users`

### Group 9: Audit and Notifications
- `audit_log` (standalone, append-only)
- `notification_log` → `users`
- `import_export_history` → `users`
