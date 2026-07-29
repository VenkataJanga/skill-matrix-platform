# SkillMatrixPlatform
A modular web platform that captures application-specific skill requirements, assigns technicians to applications, enables self-assessment, routes submissions to Lead Managers, calculates approved skill gaps, generates training recommendations and publishes dashboards for management
# Skill Matrix Platform - Module-Wise Requirements and Step-by-Step Implementation Guide

**Document Version:** 1.0  
**Prepared For:** Skill Matrix Platform Development Team  
**Date:** 30 July 2026  
**Target Technology:** Angular 17+, Spring Boot 3.x, Java 21, MySQL 8, AWS  
**Primary Objective:** Convert the complete Skill Matrix Platform requirements into clear modules, step-by-step workflows, business rules, APIs, database impact, testing needs, and delivery sequence.

---

## 1. Document Purpose

This document provides a complete module-wise requirement breakdown for the Skill Matrix Platform. It is intended for architects, business analysts, developers, QA engineers, DevOps engineers, project managers, and lead managers.

The goal is to ensure that every module is understood before development starts, with clear functional scope, data model impact, workflow steps, role-based access, APIs, validation rules, and test scenarios.

---

## 2. Final Scope Confirmation

The platform must support:

1. Application Type based classification: Web and Host.
2. Bundle based classification: B06, B12, and B20.
3. Application selection based on Application Type and Bundle.
4. Current and future applications under each Application Type and Bundle.
5. Future addition and deprecation of applications without database redesign.
6. Account, Application Type, Bundle, Application Portfolio, Application hierarchy.
7. Technician assignment to one or more applications.
8. Application-specific skill requirements.
9. Employee self-assessment.
10. Lead manager review and approval.
11. Skill gap analysis.
12. Training recommendation.
13. Dashboards and reports.
14. Audit and notifications.
15. Future modules such as Certification, Training, Leave, Rota, Resource Planning, and Workforce Analytics using the same foundation.

---

## 3. Recommended Master Hierarchy

The final recommended hierarchy is:

```text
Account
  -> Application Type
       -> WEB
       -> HOST
  -> Bundle
       -> B06
       -> B12
       -> B20
  -> Application Portfolio
       -> B06-WEB
       -> B06-HOST
       -> B12-WEB
       -> B12-HOST
       -> B20-WEB
       -> B20-HOST
  -> Application
       -> ATLAS-deZentral, AVUS, BEPPO, etc.
  -> Team
  -> Employee / Technician
  -> Application Assignment
  -> Skill Requirement
  -> Assessment Cycle
  -> Self Assessment
  -> Manager Review
  -> Gap Snapshot
  -> Training Recommendation
  -> Dashboard / Reports / Audit / Notifications
```

---

## 4. Dropdown Requirement

The UI must support three cascading dropdowns.

### 4.1 Dropdown 1 - Application Type

Values:

```text
WEB
HOST
```

### 4.2 Dropdown 2 - Bundle

Values:

```text
B06
B12
B20
```

### 4.3 Dropdown 3 - Application

Application list must be filtered by Application Type and Bundle.

Examples:

| Application Type | Bundle | Expected Applications |
|---|---|---|
| WEB | B06 | Around 27 or more applications; may increase or be deprecated |
| HOST | B06 | Around 17 or more applications; may increase or be deprecated |
| WEB | B12 | Around 10 or more applications; may increase or be deprecated |
| HOST | B12 | Around 15 or more applications; may increase or be deprecated |
| WEB | B20 | Future application list |
| HOST | B20 | Future application list |

Important rule: Application addition, deprecation, or replacement must be handled as master data changes, not schema changes.

---

## 5. Key Design Principles

1. Use master data driven design to avoid future database redesign.
2. Use lookup tables instead of MySQL ENUM for business-changing values.
3. Use `public_id` UUID for APIs and integrations; keep numeric `id` for internal joins.
4. Use soft delete and lifecycle status instead of deleting business records.
5. Use requirement versioning for expected skill levels.
6. Use assessment cycles for time-bounded submissions.
7. Keep self-assessment, manager review, approval, gap, and training as separate lifecycle stages.
8. Keep row-level review and overall form approval separate.
9. Capture audit logs for all business actions.
10. Hide expected ratings from technicians at both UI and API level.
11. Use modular monolith for initial delivery.
12. Use Flyway for all database migrations.
13. Use stable API contracts so UI is not directly dependent on database tables.

---

## 6. Module List

| Module ID | Module Name | Primary Role |
|---|---|---|
| M01 | Project Foundation and Standards | Development Team |
| M02 | Master Data and Lookup Management | Admin / Lead Manager |
| M03 | Application Type, Bundle, Portfolio and Application Management | Admin / Lead Manager |
| M04 | User, Role, Permission and Team Management | Admin |
| M05 | Technician Application Assignment | Admin / Lead Manager |
| M06 | Skill Category, Skill Catalogue and Rating Scale | Admin / Lead Manager |
| M07 | Application Skill Requirement and Version Management | Admin / Lead Manager |
| M08 | Assessment Cycle Management | Admin / Lead Manager |
| M09 | Technician Self Assessment | Technician |
| M10 | Lead Manager Review and Approval | Lead Manager |
| M11 | Skill Gap Analysis | System / Lead Manager |
| M12 | Training Recommendation | System / Lead Manager |
| M13 | Dashboard and Reports | Admin / Lead Manager |
| M14 | Excel Import and Export | Admin |
| M15 | Audit Log and Activity Tracking | Admin / Lead Manager |
| M16 | Notifications | All Roles |
| M17 | Security, RBAC and Data Protection | All Roles |
| M18 | DevOps, Deployment and Environments | DevOps |
| M19 | Testing and Quality Assurance | QA / Development Team |
| M20 | Handover and Operational Readiness | Project Team |

---

# M01 - Project Foundation and Standards

## Objective

Set up the base project structure, coding standards, development tools, CI/CD foundation, and standard architectural practices for backend, frontend, database, and deployment.

## Actors

- Solution Architect
- Backend Developer
- Frontend Developer
- DevOps Engineer
- QA Engineer

## Backend Requirements

1. Create Spring Boot 3.x project with Java 21.
2. Use Maven multi-module or clean package structure.
3. Configure profiles: dev, qa, prod.
4. Configure Spring Security baseline.
5. Configure Spring Data JPA and MySQL connection.
6. Configure Flyway migration.
7. Configure OpenAPI / Swagger.
8. Configure global exception handling.
9. Configure audit-ready base entity.
10. Configure logging with correlation ID.

## Frontend Requirements

1. Create Angular 17+ application.
2. Use Angular Material.
3. Use standalone components or feature-based modules.
4. Configure environment files: dev, qa, prod.
5. Configure route guards and role guards.
6. Configure HTTP interceptor.
7. Create shared UI components.
8. Create layout with sidebar, header, content area, and notification area.

## Database Requirements

1. Create Flyway migration folder.
2. Use versioned migration files.
3. Do not modify deployed migration files.
4. Use new migration for every schema change.
5. Seed baseline master data.

## Step-by-Step Implementation

1. Create backend Spring Boot project.
2. Add dependencies: Web, Security, JPA, MySQL, Flyway, Validation, Actuator, OpenAPI, Lombok, MapStruct.
3. Create standard backend package structure.
4. Create Angular application.
5. Add Angular Material and routing.
6. Create environment configurations.
7. Create Docker files for backend and frontend.
8. Create local docker-compose with MySQL.
9. Create initial CI build pipeline.
10. Verify backend, frontend, and database start successfully.

## Acceptance Criteria

1. Backend starts with dev profile.
2. Frontend builds successfully.
3. MySQL connection works.
4. Flyway migration runs on startup.
5. Swagger is accessible.
6. Angular layout renders successfully.
7. Docker local environment starts backend, frontend, and MySQL.

## Test Scenarios

1. Start backend with valid DB connection.
2. Start backend with invalid DB connection and verify graceful error logging.
3. Run Angular build.
4. Run Flyway migration on fresh database.
5. Verify actuator health endpoint.
6. Verify Swagger endpoint.

---

# M02 - Master Data and Lookup Management

## Objective

Provide configurable lookup values for statuses, decisions, lifecycle states, training types, priorities, notification statuses, and other business values to avoid future DDL changes.

## Key Tables

| Table | Purpose |
|---|---|
| lookup_type | Defines lookup groups |
| lookup_value | Defines values under each lookup group |

## Lookup Types

Recommended lookup types:

```text
APPLICATION_TYPE_STATUS
PORTFOLIO_STATUS
APPLICATION_LIFECYCLE_STATUS
REQUIREMENT_STATUS
ASSESSMENT_STATUS
REVIEW_ROW_DECISION
OVERALL_DECISION
ROLE_ON_APPLICATION
SKILL_TYPE
SKILL_SCOPE
CRITICALITY
TRAINING_TYPE
TRAINING_PRIORITY
NOTIFICATION_CHANNEL
NOTIFICATION_STATUS
IMPORT_EXPORT_STATUS
BUNDLE_STATUS
```

## Step-by-Step Functional Flow

1. Admin opens Lookup Management screen.
2. Admin selects a lookup type.
3. System shows existing lookup values.
4. Admin creates or updates a lookup value.
5. System validates duplicate value code.
6. Admin activates or deactivates lookup value.
7. System records audit log.
8. New lookup value becomes available in related dropdowns.

## Business Rules

1. System-critical lookup values cannot be deleted.
2. Lookup values used in transactions cannot be physically deleted.
3. Deactivate lookup values instead of deleting.
4. Display order controls dropdown order.
5. Admin can manage all lookup values.
6. Lead Manager can manage only business-approved lookup values if permission is granted.

## APIs

```http
GET    /api/v1/lookup-types
GET    /api/v1/lookup-values?typeCode={typeCode}&active=true
POST   /api/v1/lookup-values
PUT    /api/v1/lookup-values/{publicId}
PATCH  /api/v1/lookup-values/{publicId}/status
```

## Acceptance Criteria

1. Admin can create lookup values.
2. Duplicate codes are blocked.
3. Deactivated values do not appear in active dropdowns.
4. Transaction history still displays old values correctly.
5. Audit log is created for every change.

---

# M03 - Application Type, Bundle, Portfolio and Application Management

## Objective

Manage Web/Host application types, B06/B12/B20 bundles, application portfolios, and applications in a scalable way.

## Key Tables

| Table | Purpose |
|---|---|
| accounts | Top-level customer/account |
| application_types | Web, Host, future types |
| bundles | B06, B12, B20 |
| application_portfolios | Combination of Account + Application Type + Bundle |
| applications | Actual applications under a portfolio |

## Important Relationships

```text
accounts 1 -> N application_portfolios
application_types 1 -> N application_portfolios
bundles 1 -> N application_portfolios
application_portfolios 1 -> N applications
```

## Functional Requirements

1. Admin can create and manage application types.
2. Admin can create and manage bundles.
3. Admin can create portfolios for Application Type + Bundle.
4. Admin can create applications under portfolio.
5. Applications can be active, inactive, deprecated, or future.
6. Deprecated applications should not be shown in normal assignment dropdowns.
7. Deprecated applications must remain visible in historical reports.
8. Replacement application can be mapped when one application is deprecated.

## Step-by-Step Functional Flow

### Application Type Setup

1. Admin opens Application Type screen.
2. Admin creates WEB and HOST.
3. System validates unique application type code.
4. System saves and audits the action.

### Bundle Setup

1. Admin opens Bundle screen.
2. Admin creates B06, B12, and B20.
3. Admin marks B06 and B12 as active if applicable.
4. Admin marks B20 as future if not immediately used.
5. System saves and audits the action.

### Portfolio Setup

1. Admin opens Application Portfolio screen.
2. Admin selects Account.
3. Admin selects Application Type.
4. Admin selects Bundle.
5. System generates portfolio code, such as B06-WEB.
6. Admin confirms and saves.
7. System prevents duplicate Application Type + Bundle combination for same account.

### Application Setup

1. Admin opens Application Management screen.
2. Admin selects Application Type.
3. Admin selects Bundle.
4. System loads matching portfolio.
5. Admin enters application code and name.
6. Admin sets lifecycle status.
7. Admin saves application.
8. Application becomes available for skill requirements and technician assignment.

## Cascading Dropdown APIs

```http
GET /api/v1/application-types?active=true
GET /api/v1/bundles?applicationTypeCode=WEB&includeFuture=true
GET /api/v1/applications?applicationTypeCode=WEB&bundleCode=B06&includeDeprecated=false
GET /api/v1/applications?applicationTypeCode=WEB&bundleCode=B06&includeDeprecated=true
```

## UI Requirements

1. Application Type dropdown.
2. Bundle dropdown filtered by application type where required.
3. Application dropdown filtered by Application Type + Bundle.
4. Application list table with lifecycle status.
5. Create/Edit application form.
6. Deprecate application action.
7. Replacement application selector.

## Acceptance Criteria

1. WEB + B06 shows only Web applications under B06.
2. HOST + B06 shows only Host applications under B06.
3. WEB + B12 shows only Web applications under B12.
4. HOST + B12 shows only Host applications under B12.
5. Deprecated application is hidden from assignment dropdown by default.
6. Deprecated application appears in historical reports when requested.
7. Adding a new application does not require DDL change.

---

# M04 - User, Role, Permission and Team Management

## Objective

Manage application users, roles, permissions, teams, reporting hierarchy, and user-team mapping.

## Key Tables

| Table | Purpose |
|---|---|
| users | Admin, Lead Manager, Technician |
| roles | ADMIN, LEAD_MANAGER, TECHNICIAN |
| permissions | Fine-grained permissions |
| role_permissions | Role to permission mapping |
| user_roles | User to role mapping |
| teams | Team master |
| user_teams | User to team mapping |

## Roles

| Role | Responsibilities |
|---|---|
| Admin | Full system configuration and management |
| Lead Manager | Review, approve, manage skill gaps, view dashboards |
| Technician | Submit self-assessment for assigned applications |

## Functional Requirements

1. Admin can create users.
2. Admin can assign one or more roles.
3. Admin can assign users to teams.
4. Admin can activate or deactivate users.
5. Admin can reset password.
6. System supports manager hierarchy using manager_id.
7. User must have at least one active role.
8. Primary role determines default landing page.

## Step-by-Step Functional Flow

1. Admin opens User Management screen.
2. Admin clicks Create User.
3. Admin enters username, full name, email, employee id, manager, and status.
4. Admin selects one or more roles.
5. Admin selects primary role.
6. Admin assigns team if applicable.
7. System validates username and email uniqueness.
8. System creates user with temporary password.
9. System sends notification if enabled.
10. System records audit log.

## APIs

```http
GET    /api/v1/users?page=&size=&search=&role=&status=
POST   /api/v1/users
PUT    /api/v1/users/{publicId}
PATCH  /api/v1/users/{publicId}/status
POST   /api/v1/users/{publicId}/roles
POST   /api/v1/users/{publicId}/teams
GET    /api/v1/roles
GET    /api/v1/permissions
```

## Acceptance Criteria

1. Admin can create user successfully.
2. Duplicate username is blocked.
3. Duplicate email is blocked.
4. Inactive user cannot login.
5. Technician sees only technician screens.
6. Lead Manager sees only lead manager screens.
7. Admin sees all screens.

---

# M05 - Technician Application Assignment

## Objective

Map technicians to one or more applications with role, allocation percentage, effective dates, and active status.

## Key Tables

| Table | Purpose |
|---|---|
| user_application_mapping | Technician to application mapping |
| users | Technician details |
| applications | Application details |

## Functional Requirements

1. Admin or Lead Manager can assign technician to application.
2. One technician can support multiple applications.
3. One application can have multiple technicians.
4. Assignment includes role on application: Primary, Backup, SME, Trainee.
5. Assignment includes allocation percentage.
6. Assignment includes effective from and effective to dates.
7. Expired assignment should not appear as active.
8. Historical assignment should remain for reporting.

## Step-by-Step Functional Flow

1. Admin opens Technician Assignment screen.
2. Admin selects Application Type.
3. Admin selects Bundle.
4. System loads applications for selected Application Type + Bundle.
5. Admin selects application.
6. Admin selects technician.
7. Admin selects role on application.
8. Admin enters allocation percentage.
9. Admin enters effective from and effective to dates.
10. System validates duplicate active assignment.
11. System saves mapping.
12. Technician can now see the application in self-assessment dropdown.

## APIs

```http
GET    /api/v1/user-application-mappings?applicationId=&userId=&active=true
POST   /api/v1/user-application-mappings
PUT    /api/v1/user-application-mappings/{publicId}
PATCH  /api/v1/user-application-mappings/{publicId}/deactivate
GET    /api/v1/applications/assigned
```

## Business Rules

1. Allocation percentage must be between 0 and 100.
2. Effective from date is mandatory.
3. Effective to date cannot be earlier than effective from date.
4. Same technician can be assigned to same application again only with a different effective period.
5. Technician self-assessment dropdown must use only active mappings.

## Acceptance Criteria

1. Technician assigned to WEB + B06 + ATLAS can see ATLAS in dropdown.
2. Technician not assigned to HOST + B06 application cannot see that application.
3. Expired assignment is not shown in self-assessment dropdown.
4. Assignment history is available for reports.

---

# M06 - Skill Category, Skill Catalogue and Rating Scale

## Objective

Maintain reusable skill categories, skills, and rating scale across applications.

## Key Tables

| Table | Purpose |
|---|---|
| skill_categories | Skill grouping |
| skills | Reusable skill master |
| rating_scale | Rating levels 1 to 5 |

## Recommended Skill Categories

```text
Application Knowledge
Technical Skills
Infrastructure
Tools
Support Process
Documentation
Domain Knowledge
Monitoring
Deployment
Database
```

## Rating Scale

| Level | Name | Meaning |
|---|---|---|
| 1 | Awareness | Understands basic concepts only |
| 2 | Working Knowledge | Can work with guidance |
| 3 | Independent | Can work independently on regular tasks |
| 4 | Advanced | Can handle complex scenarios and guide others |
| 5 | SME / Expert | Recognized expert; can mentor and own critical issues |

## Step-by-Step Functional Flow

1. Admin opens Skill Category screen.
2. Admin creates or updates skill category.
3. Admin opens Skill Management screen.
4. Admin creates skill under a category.
5. Admin selects skill type and scope.
6. System validates unique skill code.
7. Skill becomes available for expected rating setup.

## APIs

```http
GET    /api/v1/skill-categories
POST   /api/v1/skill-categories
PUT    /api/v1/skill-categories/{publicId}
GET    /api/v1/skills?categoryId=&skillType=&active=true
POST   /api/v1/skills
PUT    /api/v1/skills/{publicId}
GET    /api/v1/rating-scale
```

## Acceptance Criteria

1. Admin can create skill categories.
2. Admin can create skills.
3. Duplicate skill code is blocked.
4. Inactive skills are not shown in expected rating setup.
5. Rating scale is available across the platform.

---

# M07 - Application Skill Requirement and Version Management

## Objective

Define expected skill requirements per application and manage them through versioning and approval workflow.

## Key Tables

| Table | Purpose |
|---|---|
| requirement_versions | Version header per application |
| expected_ratings | Skills and expected levels under version |
| skills | Skill master |
| applications | Application master |
| rating_scale | Expected level reference |

## Functional Requirements

1. Admin creates requirement version for an application.
2. Requirement version is linked to one application.
3. Admin adds skills to the version.
4. Each skill has expected level, criticality, minimum people, and mandatory flag.
5. Requirement version starts in Draft status.
6. Admin publishes version for Lead Manager approval.
7. Lead Manager approves or rejects.
8. Only approved requirement versions can be used for assessment cycles.

## Step-by-Step Functional Flow

1. Admin selects Application Type.
2. Admin selects Bundle.
3. Admin selects Application.
4. Admin creates requirement version, such as ATLAS-FY26-Q2-v1.
5. Admin adds required skills.
6. Admin sets expected level for each skill.
7. Admin sets criticality: High, Medium, Low.
8. Admin sets minimum people required.
9. Admin marks skill mandatory or optional.
10. Admin saves as Draft.
11. Admin publishes requirement version.
12. System creates Lead Manager approval task.
13. Lead Manager reviews requirement version.
14. Lead Manager approves or rejects with reason.
15. Approved version becomes available for assessment cycle.

## APIs

```http
GET    /api/v1/applications/{applicationPublicId}/requirement-versions
POST   /api/v1/applications/{applicationPublicId}/requirement-versions
PUT    /api/v1/requirement-versions/{publicId}
POST   /api/v1/requirement-versions/{publicId}/publish
POST   /api/v1/requirement-versions/{publicId}/approve
POST   /api/v1/requirement-versions/{publicId}/reject
GET    /api/v1/requirement-versions/{publicId}/expected-ratings
POST   /api/v1/requirement-versions/{publicId}/expected-ratings
PUT    /api/v1/expected-ratings/{publicId}
```

## Business Rules

1. Same skill cannot be duplicated in same requirement version.
2. Expected level must be between 1 and 5.
3. Minimum people must be greater than or equal to 1.
4. Approved version cannot be directly edited.
5. To change approved requirement, create a new version.
6. Rejected version returns to Draft with rejection reason.

## Acceptance Criteria

1. Requirement version can be created in Draft.
2. Skills can be added with expected levels.
3. Version can be published for approval.
4. Lead Manager can approve.
5. Lead Manager can reject with reason.
6. Only approved versions appear in assessment cycle setup.

---

# M08 - Assessment Cycle Management

## Objective

Create and manage assessment cycles per application using approved requirement versions.

## Key Tables

| Table | Purpose |
|---|---|
| assessment_cycles | Assessment period header |
| requirement_versions | Approved requirements |
| applications | Application master |

## Functional Requirements

1. Admin creates assessment cycle for an application.
2. Cycle must use an approved requirement version.
3. Cycle has start date and end date.
4. Cycle can be Open or Closed.
5. Only one active open cycle should exist per application at a time.
6. Closed cycle becomes read-only.
7. Historical cycles are retained.

## Step-by-Step Functional Flow

1. Admin selects Application Type.
2. Admin selects Bundle.
3. Admin selects Application.
4. Admin opens Assessment Cycle screen.
5. Admin creates cycle name, such as FY26-Q2.
6. Admin selects approved requirement version.
7. Admin enters start and end dates.
8. Admin opens the cycle.
9. Technicians can submit self-assessment during open cycle.
10. Admin closes cycle after completion.
11. Closed cycle is available for historical reporting.

## APIs

```http
GET    /api/v1/applications/{applicationPublicId}/cycles
POST   /api/v1/cycles
PATCH  /api/v1/cycles/{publicId}/status
GET    /api/v1/cycles/open?applicationPublicId={applicationPublicId}
```

## Business Rules

1. Requirement version must be approved.
2. Start date cannot be after end date.
3. Only open cycles allow submission.
4. Closed cycles are read-only.
5. One open cycle per application is recommended.

## Acceptance Criteria

1. Admin can create assessment cycle.
2. Draft/rejected requirement versions are not shown.
3. Technician cannot submit if no open cycle exists.
4. Closed cycle blocks submission.
5. Historical cycle data is retained.

---

# M09 - Technician Self Assessment

## Objective

Allow technicians to submit ratings, evidence, and comments for assigned application skills.

## Key Tables

| Table | Purpose |
|---|---|
| technician_assessments | Technician rating per skill per cycle |
| user_application_mapping | Controls assigned applications |
| assessment_cycles | Open cycle validation |
| expected_ratings | Skills to assess |

## UI Requirements

Technician Self Assessment screen must show:

1. Application Type dropdown or pre-filtered context.
2. Bundle dropdown or pre-filtered context.
3. Application dropdown showing only assigned applications.
4. Skill grid.
5. Self rating dropdown 1 to 5.
6. Evidence text field.
7. Technician comment field.
8. Save Draft button.
9. Submit Final button.
10. Status indicator.

Important: Expected Level must not be shown to technician.

## Step-by-Step Functional Flow

1. Technician logs in.
2. System redirects to Self Assessment screen.
3. System loads technician assigned applications.
4. Technician selects application.
5. System checks if open cycle exists.
6. System loads skills from approved requirement version.
7. Technician enters self-rating for each mandatory skill.
8. Technician enters evidence and comments.
9. Technician saves draft if not ready.
10. Technician submits final assessment.
11. System validates all mandatory skills are rated.
12. System changes status to Submitted.
13. System locks the assessment.
14. System creates Lead Manager review task.
15. System sends notification to Lead Manager.
16. System records audit log.

## APIs

```http
GET    /api/v1/assessments/my/applications
GET    /api/v1/assessments/my?applicationPublicId=&cyclePublicId=
PUT    /api/v1/assessments/my/draft
POST   /api/v1/assessments/my/submit
```

## Business Rules

1. Technician can see only assigned active applications.
2. Technician cannot see expected rating in UI or API.
3. Evidence is mandatory for high self-ratings such as 4 and 5.
4. Mandatory skills must have rating before final submission.
5. Submitted assessment becomes read-only.
6. Returned assessment becomes editable again.

## Acceptance Criteria

1. Technician sees only assigned applications.
2. Technician cannot see expected level in UI.
3. Technician cannot see expected level in API response.
4. Draft can be saved.
5. Final submission creates review task.
6. Missing mandatory rating blocks final submission.

---

# M10 - Lead Manager Review and Approval

## Objective

Allow Lead Managers to review technician submissions, approve ratings, identify gaps, request clarification, and trigger gap analysis.

## Key Tables

| Table | Purpose |
|---|---|
| lead_reviews | Row-level manager review |
| assessment_approvals | Overall form-level approval |
| technician_assessments | Submitted technician ratings |
| expected_ratings | Expected levels |

## UI Requirements

Manager Review screen must show:

1. Application Type dropdown.
2. Bundle dropdown.
3. Application dropdown based on selected Application Type + Bundle.
4. Pending technician dropdown.
5. Technician context panel.
6. Review grid.
7. Expected Level column visible to Lead Manager.
8. Self Rating column.
9. Manager Rating dropdown.
10. Final Rating.
11. Manager comment.
12. Row decision.
13. Bulk approve.
14. Bulk gap identified.
15. Bulk clarification.
16. Overall decision note.
17. Approve Ratings button.
18. Request Clarification button.

## Step-by-Step Functional Flow

1. Lead Manager logs in.
2. System shows pending review count.
3. Lead Manager opens Review screen.
4. Lead Manager selects Application Type, Bundle, and Application.
5. System loads technicians with submitted assessments.
6. Lead Manager selects technician.
7. System loads submitted assessment grid.
8. Lead Manager reviews each skill.
9. Lead Manager enters manager rating.
10. Lead Manager marks row as Approved, Gap Identified, or Clarification Needed.
11. Lead Manager can use bulk actions.
12. Lead Manager enters overall decision note.
13. If accepted, Lead Manager clicks Approve Ratings.
14. System creates overall approval record.
15. System triggers gap analysis for gap identified rows.
16. If clarification is needed, Lead Manager clicks Request Clarification.
17. System returns assessment to technician in Draft/Returned state.
18. System sends notification to technician.
19. System records audit log.

## APIs

```http
GET    /api/v1/reviews/pending?applicationPublicId=
GET    /api/v1/reviews?cyclePublicId=&technicianPublicId=
PUT    /api/v1/reviews/{publicId}/row
POST   /api/v1/reviews/bulk-approve
POST   /api/v1/reviews/bulk-gap
POST   /api/v1/reviews/bulk-clarification
POST   /api/v1/reviews/{cyclePublicId}/{technicianPublicId}/approve
POST   /api/v1/reviews/{cyclePublicId}/{technicianPublicId}/return
```

## Business Rules

1. Lead Manager can review only assigned scope unless Admin.
2. Expected level is visible to Lead Manager.
3. Overall approval is separate from row decisions.
4. Gap analysis should run only after overall approval.
5. Returned assessment becomes editable to technician.
6. Approved assessment becomes locked.

## Acceptance Criteria

1. Lead Manager sees submitted technicians.
2. Lead Manager can update manager rating.
3. Lead Manager can mark row decision.
4. Bulk actions work correctly.
5. Overall approval triggers gap analysis.
6. Clarification returns assessment to technician.

---

# M11 - Skill Gap Analysis

## Objective

Calculate and store skill gaps after Lead Manager approval.

## Key Tables

| Table | Purpose |
|---|---|
| skill_gap_snapshots | Stores calculated gaps |
| assessment_approvals | Links gap to approved submission |
| lead_reviews | Source of manager rating and decisions |
| expected_ratings | Source of expected levels |

## Gap Formula

```text
Gap = Expected Level - Final Approved Rating
```

If the result is greater than zero, there is a skill gap.

## Severity Rules

Recommended rules:

| Condition | Severity |
|---|---|
| High criticality and gap greater than 0 | HIGH |
| Medium criticality and gap greater than 0 | MEDIUM |
| Low criticality and gap greater than 0 | LOW |
| No gap | NONE |

## Step-by-Step Functional Flow

1. Lead Manager approves technician assessment.
2. System reads approved review rows.
3. System compares expected level with final manager rating.
4. System calculates gap.
5. System determines severity.
6. System creates skill gap snapshot.
7. System links snapshot to assessment approval.
8. System triggers training recommendation module.
9. System updates dashboard metrics.
10. System records audit log.

## APIs

```http
GET /api/v1/gaps?cyclePublicId=&technicianPublicId=
GET /api/v1/gaps/application?applicationPublicId=&cyclePublicId=
GET /api/v1/gaps/bundle?applicationTypeCode=&bundleCode=&cycleName=
```

## Business Rules

1. Gap snapshot is created only after approval.
2. Gap should be based on final manager rating, not self-rating.
3. Historical gaps should not be overwritten.
4. If reassessment happens, create new snapshot linked to new approval.
5. Dashboard should use latest approved snapshot unless historical cycle selected.

## Acceptance Criteria

1. Approval creates gap snapshot.
2. Approved rows without gaps are not shown as open gaps.
3. Gap severity is calculated correctly.
4. Historical gaps are available by cycle.

---

# M12 - Training Recommendation

## Objective

Generate training recommendations based on identified skill gaps.

## Key Tables

| Table | Purpose |
|---|---|
| training_recommendations | Training plan per gap |
| training_participants | Users linked to training |
| skill_gap_snapshots | Source gap |
| skills | Skill details |

## Functional Requirements

1. System generates training recommendations for gap skills.
2. Training type depends on skill type and category.
3. Priority depends on gap severity.
4. Target date depends on priority.
5. Lead Manager can review and confirm training plan.
6. Training plan can be exported.
7. Training plan can be tracked in future module.

## Recommended Target Rules

| Severity | Priority | Target |
|---|---|---|
| HIGH | HIGH | 30 days |
| MEDIUM | MEDIUM | 60 days |
| LOW | LOW | 90 days |

## Step-by-Step Functional Flow

1. System receives gap snapshot.
2. System maps skill type to training type.
3. System sets priority.
4. System calculates target date.
5. System creates training recommendation.
6. System maps technician as participant.
7. Lead Manager reviews training plan.
8. Lead Manager confirms plan.
9. System sends notification to technician.
10. Dashboard training count is updated.

## APIs

```http
GET  /api/v1/training-recommendations?cyclePublicId=&technicianPublicId=
GET  /api/v1/training-recommendations/application?applicationPublicId=
POST /api/v1/training-recommendations/generate
POST /api/v1/training-recommendations/{publicId}/confirm
```

## Acceptance Criteria

1. Training is generated only for gaps.
2. Priority is mapped correctly.
3. Target date is calculated correctly.
4. Technician is mapped as participant.
5. Training plan appears in dashboard and report.

---

# M13 - Dashboard and Reports

## Objective

Provide management visibility into application readiness, skill gaps, coverage risks, technician readiness, and training demand.

## Dashboard Views

1. Executive dashboard.
2. Application Type dashboard.
3. Bundle dashboard.
4. Application dashboard.
5. Technician dashboard.
6. Skill heatmap.
7. Coverage risk dashboard.
8. Training demand dashboard.

## Key Metrics

| Metric | Meaning |
|---|---|
| Application Readiness % | Approved skills meeting expected level |
| Skill Gap Count | Count of skills below expected level |
| High Risk Skill Count | High criticality gaps |
| SME Coverage | Number of technicians at required level |
| Backup Coverage | Number of backup technicians |
| Training Demand | Open training recommendations |
| Assessment Completion | Submitted vs expected submissions |

## Step-by-Step Functional Flow

1. Admin or Lead Manager opens dashboard.
2. User selects Application Type.
3. User selects Bundle.
4. User selects Application or views all applications.
5. System loads readiness cards.
6. System loads application readiness table.
7. System loads skill heatmap.
8. System loads coverage risk.
9. User filters by cycle, technician, skill category, or status.
10. User exports report to Excel.

## APIs

```http
GET /api/v1/dashboard/summary?applicationTypeCode=&bundleCode=
GET /api/v1/dashboard/readiness?applicationTypeCode=&bundleCode=
GET /api/v1/dashboard/application-readiness?applicationPublicId=&cyclePublicId=
GET /api/v1/dashboard/heatmap?applicationPublicId=&cyclePublicId=
GET /api/v1/dashboard/coverage-risk?applicationPublicId=
GET /api/v1/reports/gap-report?filters
GET /api/v1/reports/training-demand?filters
GET /api/v1/reports/assessment-status?filters
```

## Acceptance Criteria

1. Dashboard filters work for WEB/HOST and B06/B12/B20.
2. Readiness percentage matches approved assessment data.
3. Skill heatmap shows correct technician ratings.
4. Coverage risk highlights skills below minimum people threshold.
5. Reports export correctly.

---

# M14 - Excel Import and Export

## Objective

Support bulk setup and reporting through Excel import/export.

## Import Types

1. Application master import.
2. Technician assignment import.
3. Skill catalogue import.
4. Application skill requirement import.
5. Historical assessment import if required.

## Export Types

1. Application readiness report.
2. Skill gap report.
3. Training recommendation report.
4. Technician assignment report.
5. Assessment result report.
6. Audit log report.

## Required Import Columns for Application Master

```text
application_type_code
bundle_code
application_code
application_name
lifecycle_status
replacement_application_code
remarks
```

## Required Import Columns for Skill Requirement

```text
application_type_code
bundle_code
application_code
requirement_version
skill_category
skill_code
skill_name
expected_level
criticality
min_people
mandatory_flag
```

## Step-by-Step Functional Flow

1. Admin opens Import screen.
2. Admin downloads template.
3. Admin fills template.
4. Admin uploads file.
5. System validates file format.
6. System validates required columns.
7. System validates row-level values.
8. System checks duplicates.
9. If errors exist, system rejects import and shows row-level errors.
10. If valid, system imports data.
11. System records import history.
12. System records audit log.

## APIs

```http
GET  /api/v1/imports/templates/{templateType}
POST /api/v1/imports/applications
POST /api/v1/imports/skill-requirements
POST /api/v1/imports/assignments
GET  /api/v1/imports/history
GET  /api/v1/reports/{reportType}/export
```

## Business Rules

1. Import should be all-or-nothing by default.
2. Invalid rows should be reported with row number and message.
3. Existing active records should not be duplicated.
4. Imports should use business codes, not numeric IDs.
5. Imported files should be stored in S3 in production.

## Acceptance Criteria

1. Valid file imports successfully.
2. Invalid file is rejected with clear error messages.
3. Import history is saved.
4. Export file downloads successfully.
5. Exported data matches selected filters.

---

# M15 - Audit Log and Activity Tracking

## Objective

Track who performed which action, when, and what changed.

## Key Tables

| Table | Purpose |
|---|---|
| audit_log | Append-only business audit |

## Actions to Audit

```text
LOGIN_SUCCESS
LOGIN_FAILED
CREATE
UPDATE
DEACTIVATE
PUBLISH
APPROVE
REJECT
SUBMIT
RETURN
GENERATE_GAP
GENERATE_TRAINING
IMPORT
EXPORT
```

## Step-by-Step Functional Flow

1. User performs business action.
2. Service layer captures old value and new value.
3. Audit service creates audit log entry.
4. Audit entry includes user, action, entity type, entity id, timestamp, IP, old value, new value.
5. Admin opens Audit Log screen.
6. Admin filters by date, user, action, entity type, and application.
7. Admin exports audit report if required.

## APIs

```http
GET /api/v1/audit-log?fromDate=&toDate=&userPublicId=&entityType=&action=
GET /api/v1/audit-log/export?filters
```

## Business Rules

1. Audit log is append-only.
2. No update or delete endpoint for audit log.
3. Sensitive values such as passwords and tokens must not be logged.
4. Audit log must retain business history even if records are soft-deleted.

## Acceptance Criteria

1. Submit assessment creates audit log.
2. Manager approval creates audit log.
3. Requirement publish and approval create audit logs.
4. Import and export are logged.
5. Audit screen supports filters.

---

# M16 - Notifications

## Objective

Notify users about pending actions, approvals, returns, training plans, and important workflow events.

## Key Tables

| Table | Purpose |
|---|---|
| notification_log | Email and in-app notification tracking |

## Notification Events

| Event | Recipient |
|---|---|
| Requirement published | Lead Manager |
| Requirement approved/rejected | Admin |
| Assessment submitted | Lead Manager |
| Assessment returned | Technician |
| Assessment approved | Technician |
| Training plan generated | Technician and Lead Manager |
| Cycle opened/closed | Assigned technicians and lead managers |

## Step-by-Step Functional Flow

1. Business action occurs.
2. System determines notification recipients.
3. System creates notification log in Pending status.
4. System sends email or in-app notification.
5. System updates notification status to Sent or Failed.
6. Failed notifications are retried by scheduler.
7. User views notification in notification center.

## APIs

```http
GET   /api/v1/notifications/my
PATCH /api/v1/notifications/{publicId}/read
GET   /api/v1/notifications/pending-count
GET   /api/v1/notifications/admin-log
```

## Business Rules

1. Notification failures should not block business transaction.
2. Email sending should be asynchronous.
3. Notification should be linked to triggering entity.
4. In-app notification count should be role-based.

## Acceptance Criteria

1. Lead Manager receives notification after technician submission.
2. Technician receives notification after approval or return.
3. Notification status is logged.
4. Failed notification can be retried.

---

# M17 - Security, RBAC and Data Protection

## Objective

Secure the platform using authentication, authorization, API protection, secure token handling, and data visibility rules.

## Key Security Requirements

1. Username/password login.
2. BCrypt password hashing.
3. JWT access token.
4. Refresh token stored securely.
5. Role-based authorization.
6. Method-level security in backend.
7. Expected rating hidden from technician at API level.
8. HTTPS only in production.
9. No secrets in code.
10. Secrets stored in AWS Secrets Manager.
11. Audit of important business actions.
12. Account lockout after repeated failed login.

## Recommended Token Strategy

```text
Access token: short-lived and stored in memory
Refresh token: HTTP-only, Secure, SameSite cookie
Refresh token hash: stored in database
```

## Role Access Matrix

| Module | Admin | Lead Manager | Technician |
|---|---|---|---|
| Master Data | Full | Limited if permitted | No |
| User Management | Full | No | No |
| Application Management | Full | Limited if permitted | No |
| Skill Requirement | Full | Approve/Reject | No |
| Technician Assignment | Full | Limited if permitted | No |
| Self Assessment | View all | View submitted | Own only |
| Manager Review | Full | Assigned scope | No |
| Dashboard | Full | Assigned scope | Own view if needed |
| Audit | Full | Read limited | No |
| Notifications | Own + admin view | Own | Own |

## Acceptance Criteria

1. Unauthenticated API returns 401.
2. Unauthorized API returns 403.
3. Technician cannot access admin screens.
4. Technician cannot view expected rating in API response.
5. Lead Manager cannot approve unrelated application unless allowed.
6. Passwords are never stored in plain text.

---

# M18 - DevOps, Deployment and Environments

## Objective

Deploy the platform to AWS using secure, scalable, and maintainable infrastructure.

## Recommended AWS Components

| Layer | AWS Service |
|---|---|
| Frontend | S3 + CloudFront |
| Backend | ECS Fargate |
| Container Registry | ECR |
| Database | RDS MySQL 8 |
| File Storage | S3 |
| Secrets | Secrets Manager |
| Email | AWS SES |
| Logs and Monitoring | CloudWatch |
| CI/CD | CodePipeline / CodeBuild or GitHub Actions |

## Environments

| Environment | Purpose |
|---|---|
| DEV | Developer integration and unit testing |
| QA | Functional testing, SIT, UAT preparation |
| PROD | Production users |

## Deployment Steps

1. Developer commits code.
2. CI pipeline runs unit tests.
3. Backend build creates JAR.
4. Docker image is built.
5. Docker image is pushed to ECR.
6. Angular build is created.
7. Angular files are deployed to S3.
8. CloudFront cache is invalidated.
9. Flyway migration is executed in controlled stage.
10. ECS service is updated.
11. Smoke tests run.
12. Deployment status notification is sent.

## Production Rules

1. No hardcoded credentials.
2. Manual approval before production deployment.
3. Flyway migration must be reviewed before production.
4. Rollback plan must exist.
5. CloudWatch alarms must be configured.
6. RDS backup must be enabled.

## Acceptance Criteria

1. DEV deployment works automatically.
2. QA deployment works with test data.
3. PROD deployment requires approval.
4. Smoke test passes after deployment.
5. Logs are visible in CloudWatch.
6. Secrets are not stored in code or config files.

---

# M19 - Testing and Quality Assurance

## Objective

Ensure the platform is functionally correct, secure, performant, and ready for production use.

## Test Types

1. Unit testing.
2. Integration testing.
3. API testing.
4. UI component testing.
5. End-to-end testing.
6. Security testing.
7. Performance testing.
8. UAT testing.
9. Regression testing.
10. Deployment smoke testing.


## Backend Unit Testing - JUnit 5 and Mockito

The backend development must include detailed unit testing using **JUnit 5** and **Mockito** for all service-layer business logic. The purpose is to validate business rules independently without depending on the actual database, external email service, AWS services, or frontend.

### Recommended Testing Stack

| Area | Tool / Library | Purpose |
|---|---|---|
| Unit testing | JUnit 5 | Test service methods, validators, utility classes, and business logic |
| Mocking | Mockito | Mock repositories, security context, notification service, and external dependencies |
| Assertions | AssertJ / JUnit Assertions | Clear and readable validation of expected results |
| Spring Boot tests | Spring Boot Test | Limited use for integration-style tests only |
| REST API testing | MockMvc / WebTestClient | Controller and security endpoint validation |
| DB integration testing | Testcontainers MySQL | Validate repositories and Flyway migrations against real MySQL |
| Coverage | JaCoCo | Enforce minimum coverage in CI pipeline |

### Unit Test Coverage Target

| Layer | Minimum Coverage | Notes |
|---|---:|---|
| Service layer | 85%+ | Highest priority; contains core business rules |
| Utility / mapper logic | 80%+ | Validate conversion, formatting, and reusable logic |
| Controller layer | 70%+ | Prefer MockMvc tests for API behavior |
| Repository layer | Integration tested | Use Testcontainers instead of Mockito |
| Overall backend | 80%+ | Enforced through JaCoCo report |

### Mockito Testing Rules

1. Mock repositories, email service, audit service, notification service, and security context where needed.
2. Do not mock the class under test.
3. Do not connect to MySQL in pure unit tests.
4. Do not call real AWS services in unit tests.
5. Validate both success and failure scenarios.
6. Verify that audit and notification methods are triggered where required.
7. Keep unit tests fast, deterministic, and independent.

### Standard Unit Test Structure

```java
@ExtendWith(MockitoExtension.class)
class RequirementVersionServiceTest {

    @Mock
    private RequirementVersionRepository requirementVersionRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RequirementVersionService requirementVersionService;

    @Test
    void shouldPublishRequirementVersionWhenStatusIsDraft() {
        // arrange
        // act
        // assert
    }
}
```

### Module-wise JUnit and Mockito Test Plan

| Module | Test Class Examples | Key Scenarios |
|---|---|---|
| Authentication and RBAC | `AuthServiceTest`, `JwtServiceTest`, `RoleGuardServiceTest` | Valid login, invalid login, inactive user, lockout after failed attempts, token generation, role resolution |
| User Management | `UserServiceTest` | Create user, duplicate username/email, deactivate/reactivate user, role assignment, password reset |
| Application Type / Bundle / Portfolio | `ApplicationTypeServiceTest`, `BundleServiceTest`, `ApplicationPortfolioServiceTest` | Create WEB/HOST types, create B06/B12/B20, prevent duplicate portfolio, filter portfolios by type and bundle |
| Application Management | `ApplicationServiceTest` | Create application under portfolio, duplicate app code prevention, lifecycle status ACTIVE/DEPRECATED, replacement application mapping |
| Technician Assignment | `UserApplicationMappingServiceTest` | Assign technician, prevent invalid allocation, support multi-application assignment, filter assigned applications |
| Skill Master | `SkillServiceTest`, `SkillCategoryServiceTest` | Create skill, duplicate skill code, deactivate skill, filter skills by category/type |
| Requirement Version | `RequirementVersionServiceTest`, `ExpectedRatingServiceTest` | Create draft version, publish, approve, reject, prevent use of non-approved version, duplicate skill validation |
| Assessment Cycle | `AssessmentCycleServiceTest` | Create cycle, allow only one open cycle per application, close cycle, block submission for closed cycle |
| Technician Self Assessment | `AssessmentServiceTest` | Load assigned skills, save draft, submit final, mandatory rating validation, evidence required for rating 4/5, hide expected level for technician response |
| Manager Review | `ReviewServiceTest` | Load pending reviews, update row rating, bulk approve, bulk gap identified, return for clarification, approve overall submission |
| Gap Analysis | `GapAnalysisServiceTest` | Calculate gap, severity mapping, ignore approved rows, create snapshot only after approval |
| Training Recommendation | `TrainingRecommendationServiceTest` | Generate training based on gap severity, high priority 30 days, medium priority 60 days, participant mapping |
| Dashboard | `DashboardServiceTest` | Readiness percentage, coverage risk, heatmap data, application-wise summary |
| Import / Export | `ImportServiceTest`, `ExportServiceTest` | Validate Excel rows, reject invalid file, all-or-nothing import, export headers and data creation |
| Audit Log | `AuditLogServiceTest` | Create audit entry, capture old/new values, block update/delete behavior at service level |
| Notification | `NotificationServiceTest` | Create notification, send email asynchronously, handle failed email without breaking main workflow |

### Must-Have Backend Unit Test Scenarios

1. Technician can only retrieve assigned applications.
2. Technician API response must not expose expected rating.
3. Admin can create application type, bundle, portfolio, and application.
4. Lead Manager can approve or reject requirement versions.
5. Non-approved requirement version cannot be used for assessment cycle.
6. Only one open cycle is allowed per application.
7. Technician cannot submit assessment for a closed cycle.
8. Rating 4 or 5 requires evidence.
9. Manager review approval triggers gap analysis.
10. Gap analysis creates snapshots only for gap-identified rows.
11. Training recommendations are created only for valid gaps.
12. Audit log is created for submit, approve, reject, return, and update actions.
13. Notification failure must not roll back main business transaction unless explicitly required.
14. Deprecated applications are excluded from active dropdowns but available for historical reports.
15. B06/B12/B20 and WEB/HOST filters return correct application lists.

### Controller and Security Tests

Use `MockMvc` for API and security behavior.

| API Area | Required Tests |
|---|---|
| Auth APIs | Login success, invalid credentials, inactive user, refresh token, logout |
| Admin APIs | Technician receives 403, Lead Manager receives 403 where not allowed, Admin succeeds |
| Technician APIs | Technician can access `/assessments/my`, but cannot access `/reviews` or `/admin` |
| Lead Manager APIs | Lead Manager can access review APIs only for assigned application scope |
| Token validation | Missing token 401, expired token 401, tampered token 401 |

### Integration Tests with Testcontainers

Mockito should not be used for repository behavior. Repository and migration validation should use Testcontainers with MySQL.

Required integration tests:

1. Flyway migrations run successfully on fresh MySQL.
2. Unique constraints prevent duplicate application under same portfolio.
3. Unique constraints prevent duplicate skill within same requirement version.
4. Assessment cycle, technician assessment, review, gap, and training relationships persist correctly.
5. Dashboard queries return correct values from real test data.

### CI/CD Quality Gate

The CI pipeline must run:

```bash
mvn clean test
mvn verify
```

JaCoCo quality gate:

```text
Overall backend coverage: minimum 80%
Service layer coverage: minimum 85%
Critical workflow services: minimum 90% recommended
```

Build should fail when:

1. Unit tests fail.
2. Integration tests fail.
3. JaCoCo minimum coverage is not met.
4. Security-related tests fail.
5. Flyway migration validation fails.

## Key UAT Scenarios

1. Admin creates WEB and HOST application types.
2. Admin creates B06, B12, B20 bundles.
3. Admin creates B06-WEB and B06-HOST portfolios.
4. Admin creates applications under portfolios.
5. Admin creates skills and expected ratings.
6. Admin publishes requirement version.
7. Lead Manager approves requirement version.
8. Admin opens assessment cycle.
9. Admin assigns technician to application.
10. Technician submits self-assessment.
11. Lead Manager reviews and approves.
12. System calculates skill gaps.
13. System generates training recommendations.
14. Dashboard updates readiness and gap metrics.
15. Audit log shows all actions.
16. Notifications are delivered.

## Must-Pass Security Tests

1. Technician cannot access admin APIs.
2. Technician cannot access manager review APIs.
3. Technician cannot see expected ratings in API response.
4. Expired token returns 401.
5. Tampered token returns 401.
6. Invalid role returns 403.
7. Repeated login failures lock account.
8. SQL injection attempts are blocked.

## Performance Targets

| Area | Target |
|---|---|
| Login API | Less than 500 ms |
| Dropdown APIs | Less than 300 ms |
| Self assessment load | Less than 1 second |
| Manager review load | Less than 2 seconds |
| Dashboard load | Less than 3 seconds |
| Excel import 500 rows | Less than 30 seconds |
| Concurrent users | 50 initial target |

## Acceptance Criteria

1. Critical functional flows pass.
2. Role-based tests pass.
3. API test collection passes.
4. No open critical or high security defects.
5. UAT sign-off is completed.
6. Performance targets are met.

---

# M20 - Handover and Operational Readiness

## Objective

Ensure the project can be supported, maintained, enhanced, and operated after go-live.

## Handover Deliverables

1. Architecture document.
2. Database design document.
3. ER diagram.
4. API specification.
5. Swagger URL.
6. Deployment runbook.
7. Production support runbook.
8. User guide for Admin.
9. User guide for Lead Manager.
10. User guide for Technician.
11. Test case document.
12. UAT sign-off document.
13. Known issues list.
14. Release notes.
15. Rollback plan.
16. Monitoring dashboard details.
17. Backup and recovery process.

## Operational Checklist

1. Production URL confirmed.
2. Admin users created.
3. Roles and permissions seeded.
4. B06, B12, B20 created.
5. WEB and HOST application types created.
6. Application portfolios created.
7. Initial applications imported.
8. Skill categories created.
9. Rating scale seeded.
10. Email notification tested.
11. RDS backup enabled.
12. CloudWatch alarms configured.
13. Audit log verified.
14. Smoke test completed.
15. Support contacts documented.

## Acceptance Criteria

1. Support team can operate the system using runbook.
2. Admin can create and manage master data.
3. Monitoring and alerts are active.
4. Backup is verified.
5. Rollback process is documented.
6. Go-live checklist is signed off.

---

## 21. Step-by-Step End-to-End Business Flow

This is the complete business process from setup to dashboard.

### Step 1 - Platform Setup

1. Admin logs in.
2. Admin verifies account master.
3. Admin verifies lookup values.
4. Admin verifies roles and permissions.

### Step 2 - Application Hierarchy Setup

1. Admin creates Application Types: WEB, HOST.
2. Admin creates Bundles: B06, B12, B20.
3. Admin creates portfolios: B06-WEB, B06-HOST, B12-WEB, B12-HOST, B20-WEB, B20-HOST.
4. Admin imports or creates applications under each portfolio.

### Step 3 - User and Team Setup

1. Admin creates users.
2. Admin assigns roles.
3. Admin creates teams.
4. Admin maps users to teams.
5. Admin maps managers where required.

### Step 4 - Skill Master Setup

1. Admin creates skill categories.
2. Admin creates skills.
3. Admin verifies rating scale.

### Step 5 - Application Skill Requirement Setup

1. Admin selects Application Type.
2. Admin selects Bundle.
3. Admin selects Application.
4. Admin creates requirement version.
5. Admin adds skills and expected levels.
6. Admin publishes requirement version.
7. Lead Manager approves or rejects.

### Step 6 - Technician Assignment

1. Admin selects Application Type.
2. Admin selects Bundle.
3. Admin selects Application.
4. Admin assigns technicians.
5. Admin sets role on application and allocation.

### Step 7 - Assessment Cycle Setup

1. Admin selects approved requirement version.
2. Admin creates assessment cycle.
3. Admin opens cycle.
4. Assigned technicians become eligible for self-assessment.

### Step 8 - Technician Self Assessment

1. Technician logs in.
2. Technician sees assigned applications only.
3. Technician selects application.
4. Technician enters ratings and evidence.
5. Technician saves draft or submits final.
6. System creates review task.

### Step 9 - Lead Manager Review

1. Lead Manager opens pending review.
2. Lead Manager selects technician.
3. Lead Manager reviews each skill.
4. Lead Manager updates manager rating.
5. Lead Manager marks gaps or approves rows.
6. Lead Manager approves or returns overall form.

### Step 10 - Gap and Training

1. System calculates gaps after approval.
2. System creates gap snapshots.
3. System generates training recommendations.
4. Lead Manager reviews training plan.
5. Technician receives training notification.

### Step 11 - Dashboard and Reports

1. Admin or Lead Manager opens dashboard.
2. User filters by Application Type, Bundle, Application, and Cycle.
3. System shows readiness, gap, training, coverage, and heatmap.
4. User exports reports as required.

### Step 12 - Audit and Operations

1. System records all business actions.
2. Admin monitors audit and notification logs.
3. Support team reviews alerts and issues.
4. Reports support management governance.

---

## 22. Module Dependency Sequence

Recommended development order:

```text
1. M01 - Project Foundation
2. M02 - Lookup Management
3. M03 - Application Type / Bundle / Portfolio / Application
4. M04 - User / Role / Permission / Team
5. M06 - Skill Master and Rating Scale
6. M05 - Technician Application Assignment
7. M07 - Requirement Version and Expected Rating
8. M08 - Assessment Cycle
9. M09 - Technician Self Assessment
10. M10 - Lead Manager Review
11. M11 - Gap Analysis
12. M12 - Training Recommendation
13. M13 - Dashboard and Reports
14. M14 - Excel Import and Export
15. M15 - Audit Log
16. M16 - Notifications
17. M17 - Security Hardening
18. M18 - Deployment
19. M19 - Testing
20. M20 - Handover
```

---

## 23. Recommended Phase Plan

## Phase 0 - Database Freeze and Final Design

Duration: 3 to 5 working days

Deliverables:

1. Final ER diagram.
2. Final database design.
3. Final lookup strategy.
4. Final API convention.
5. Final role matrix.
6. Flyway migration plan.

## Phase 1 - Foundation and Master Data

Deliverables:

1. Login.
2. RBAC.
3. Application Type, Bundle, Portfolio, Application.
4. User management.
5. Skill master.
6. Technician assignment.

## Phase 2 - Core Assessment Workflow

Deliverables:

1. Requirement version.
2. Expected rating setup.
3. Assessment cycle.
4. Technician self-assessment.
5. Lead Manager review.
6. Approval and clarification.

## Phase 3 - Gap, Training and Dashboard

Deliverables:

1. Gap calculation.
2. Training recommendation.
3. Dashboard.
4. Reports.
5. Excel export.

## Phase 4 - Import, Audit, Notification and Production Readiness

Deliverables:

1. Excel import.
2. Audit log.
3. Email notification.
4. In-app notification.
5. AWS deployment.
6. Security testing.
7. UAT.
8. Handover.

---

## 24. Definition of Ready

A story is ready for development only when:

1. Business requirement is clear.
2. UI screen or wireframe is available.
3. API contract is defined.
4. Database impact is identified.
5. Role access is defined.
6. Validation rules are listed.
7. Error handling is defined.
8. Test scenarios are documented.
9. Dependencies are cleared.
10. Acceptance criteria are approved.

---

## 25. Definition of Done

A story is complete only when:

1. Code is implemented.
2. Unit tests are completed.
3. API tests are completed.
4. UI tests are completed where applicable.
5. Role-based access is verified.
6. Audit log is verified where applicable.
7. Error handling is verified.
8. Code review is completed.
9. Build pipeline passes.
10. QA sign-off is received.
11. Documentation is updated.

---

## 26. Final Recommendations

1. Freeze the database after adding Application Type and Application Portfolio.
2. Use lookup tables instead of ENUM columns.
3. Use UUID public_id in APIs.
4. Keep applications lifecycle managed, not physically deleted.
5. Start development with one complete vertical slice: WEB + B06 + ATLAS-deZentral.
6. Seed B06, B12, and B20 from day one.
7. Include both WEB and HOST portfolios from day one.
8. Keep future modules in mind but do not overbuild them in Phase 1.
9. Maintain stable API contracts even if internal database evolves.
10. Use audit and notification framework from early phases.

---

## 27. Conclusion

The Skill Matrix Platform should be built as a master-data-driven, workflow-enabled, role-based enterprise application. The revised design using Application Type, Bundle, Application Portfolio, and Application provides the flexibility needed for Web and Host applications across B06, B12, and B20 without repeated database redesign.

By implementing the modules in the recommended order, the team can deliver a strong foundation first, then complete the end-to-end assessment workflow, dashboards, reports, audit, notifications, and production deployment. This approach reduces risk, improves maintainability, and supports future expansion into training, certification, rota, leave, resource planning, and workforce analytics.


