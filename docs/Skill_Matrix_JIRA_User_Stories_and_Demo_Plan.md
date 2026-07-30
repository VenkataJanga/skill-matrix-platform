# Skill Matrix Platform - JIRA User Stories and Sprint Demo Plan

**Version:** 1.0  
**Prepared For:** JIRA backlog creation  
**Recommended Stack:** Angular 17+, Java 21, Spring Boot 3.x, MySQL 8, Flyway, JUnit 5, Mockito  

## Purpose

This document splits the Skill Matrix Platform requirements into JIRA-ready epics and user stories from initial design readiness to production delivery. Story points are indicative and should be refined during sprint planning.

## JIRA Usage Recommendation

- Create each `Epic` in JIRA first.
- Create each `SMP-US-xxx` as a Story under the relevant Epic.
- Add Backend, Frontend, QA, DevOps and Documentation subtasks under each story as needed.
- Use the `Demo Milestone` field/tag to identify what goes into each sprint demo.
- Keep DDL and API contract stories ahead of UI stories.
- Every backend story must include JUnit 5 + Mockito tests.
- Every frontend workflow story must include route/access validation and basic UI tests.

## Definition of Ready

- Business value is clear.
- Acceptance criteria are available.
- Required DDL/API dependency is known.
- Role/access impact is clear.
- Test scenario is clear.
- Demo milestone is assigned.

## Definition of Done

- Code is implemented and reviewed.
- Unit tests are added and passing.
- API contract is available in Swagger where applicable.
- UI changes are integrated where applicable.
- Role-based access is validated.
- Audit/notification impact is handled where required.
- Story is demo-ready with sample data.

## Epic Summary

- **E01 - Project Readiness and Design Freeze** - 4 stories, approx. 10 points
- **E02 - Project Foundation and Local Environment** - 7 stories, approx. 23 points
- **E03 - Database, Flyway and Reference Data** - 6 stories, approx. 28 points
- **E04 - Authentication, RBAC and First Login Security** - 7 stories, approx. 41 points
- **E05 - Lookup and Application Hierarchy** - 7 stories, approx. 41 points
- **E06 - User, Team and Technician Assignment** - 9 stories, approx. 63 points
- **E07 - Skill Master and Rating Scale** - 7 stories, approx. 45 points
- **E09 - Requirement Version and Expected Ratings** - 6 stories, approx. 45 points
- **E10 - Assessment Cycle** - 3 stories, approx. 16 points
- **E11 - Technician Self Assessment** - 6 stories, approx. 39 points
- **E12 - Lead Manager Review and Approval** - 7 stories, approx. 44 points
- **E13 - Gap Analysis and Training Recommendation** - 5 stories, approx. 34 points
- **E14 - Dashboard and Reports** - 7 stories, approx. 44 points
- **E08 - Excel Import and Export** - 2 stories, approx. 10 points
- **E15 - Audit Log and Notifications** - 5 stories, approx. 31 points
- **E16 - Testing, Quality and UAT** - 6 stories, approx. 36 points
- **E17 - DevOps, Deployment and Production Readiness** - 6 stories, approx. 42 points
- **E18 - Future SSO Readiness** - 2 stories, approx. 8 points

## Demo Plan

| Demo | Theme | Story Range | What to Demonstrate | Exit Criteria |
|---|---|---|---|---|
| Demo 0 | Backlog and Design Freeze | SMP-US-001 to SMP-US-004 | Show finalized scope, Jira structure, DoR/DoD, architecture/design baseline. No production code demo required. | Agreement to start development. |
| Demo 1 | Foundation and Database | SMP-US-005 to SMP-US-017 | Show backend startup, Angular shell, Flyway migration, seeded DB, Swagger, Actuator, verification queries. | Technical foundation accepted. |
| Demo 2 | Authentication and RBAC | SMP-US-018 to SMP-US-024 | Show login using Company User ID, first-login password change, role-based menus, 401/403 behavior. | Security/login flow accepted. |
| Demo 3 | Application Hierarchy | SMP-US-025 to SMP-US-031 | Show lookup management and WEB/HOST + B06/B12/B20 cascading dropdowns with active/deprecated application behavior. | Application context model accepted. |
| Demo 4 | Users and Technician Assignment | SMP-US-032 to SMP-US-040 | Show bulk user upload, user management, roles, teams, multi-application technician assignment and assignment import. | 140-technician onboarding approach accepted. |
| Demo 5 | Skill Master and Skill Imports | SMP-US-041 to SMP-US-047 | Show three skill categories, common technical skill import once, app-specific skill import for ATLAS/AVUS, rating scale. | Skill governance approach accepted. |
| Demo 6 | Requirement Version and Expected Ratings | SMP-US-048 to SMP-US-053 | Show ATLAS requirement version, expected ratings, publish/approve/reject flow, requirement mapping import. | Approved requirement baseline ready for assessment. |
| Demo 7 | Assessment Cycle and Technician Self Assessment | SMP-US-054 to SMP-US-062 | Show open cycle, technician sees assigned app only, expected level hidden, save draft, submit final. | Technician workflow accepted. |
| Demo 8 | Lead Review and Gap Calculation | SMP-US-063 to SMP-US-071 | Show pending review, manager ratings, row decisions, bulk actions, overall approval/return, gap snapshot generation. | Lead Manager workflow accepted. |
| Demo 9 | Training and Dashboard | SMP-US-072 to SMP-US-077 | Show training recommendations, priority/target dates, dashboard summary, readiness, heatmap and coverage risk. | Management visibility accepted. |
| Demo 10 | Reports, Exports, Audit and Notifications | SMP-US-078 to SMP-US-088 | Show report filters, Excel export, import history, audit log, notification drawer and event notifications. | Operational traceability accepted. |
| Demo 11 | QA, UAT and Deployment Readiness | SMP-US-089 to SMP-US-097 | Show test coverage, API tests, E2E tests, security/performance results and QA deployment. | Release candidate accepted for UAT. |
| Demo 12 | Production Go-Live and Handover | SMP-US-098 to SMP-US-100 | Show production smoke test, runbooks, user guides, support readiness and sign-off pack. | Go-live approved. |
| Post-Go-Live | Future SSO Readiness | SMP-US-101 to SMP-US-102 | Show SSO approach, company identity mapping, required approvals and future implementation backlog. | SSO roadmap accepted. |

## Detailed User Stories


# E01 - Project Readiness and Design Freeze

## SMP-US-001 - Freeze requirement baseline v1.1

**Module:** M01  
**Persona:** Product Owner / Architect  
**Priority:** Highest  
**Story Points:** 2  
**Demo Milestone:** Demo 0  
**Component:** Documentation  

**User Story:** As a Product Owner, I want the requirement, UI, DDL and ERD baselines frozen so that development starts from an agreed scope.

**Acceptance Criteria:**
- Requirement document version is marked v1.1
- DDL_SCHEMA.sql is marked as source of truth
- UI screen document is aligned with Company User ID, first login password change and Excel import rules
- ERD table names match DDL table names
- Open changes are logged as sprint backlog items, not informal changes

## SMP-US-002 - Create Jira project structure

**Module:** M01  
**Persona:** Scrum Master  
**Priority:** Highest  
**Story Points:** 2  
**Demo Milestone:** Demo 0  
**Component:** Scrum  
**Dependencies:** SMP-US-001  

**User Story:** As a Scrum Master, I want epics, stories, components, labels and demo milestones configured in Jira so that the team can track delivery consistently.

**Acceptance Criteria:**
- Jira project has epics matching M01 to M20 delivery areas
- Components include Backend, Frontend, Database, DevOps, QA, Documentation
- Story template includes acceptance criteria, dependencies and demo milestone
- Definition of Ready and Definition of Done are published
- Backlog is ordered by dependency and MVP priority

## SMP-US-003 - Create API and coding standards

**Module:** M01  
**Persona:** Architect  
**Priority:** Highest  
**Story Points:** 3  
**Demo Milestone:** Demo 0  
**Component:** Architecture  
**Dependencies:** SMP-US-001  

**User Story:** As an Architect, I want API, package, DTO, validation and error handling standards defined so that developers implement consistently.

**Acceptance Criteria:**
- Standard package structure is documented
- API versioning follows /api/v1
- DTOs are mandatory and JPA entities are not exposed
- public_id is used in APIs instead of internal numeric IDs
- Error response includes timestamp, message and correlation ID

## SMP-US-004 - Define test strategy and quality gates

**Module:** M19  
**Persona:** QA Lead  
**Priority:** Highest  
**Story Points:** 3  
**Demo Milestone:** Demo 0  
**Component:** QA  
**Dependencies:** SMP-US-001  

**User Story:** As a QA Lead, I want unit, integration, API, UI and UAT quality gates agreed so that every story is testable from day one.

**Acceptance Criteria:**
- JUnit 5 and Mockito are mandatory for backend services
- MockMvc tests cover controller/security behavior
- JaCoCo minimum coverage is configured
- Critical E2E journeys are listed
- UAT sign-off checklist is created


# E02 - Project Foundation and Local Environment

## SMP-US-005 - Create Spring Boot backend skeleton

**Module:** M01  
**Persona:** Developer  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 1  
**Component:** Backend  
**Dependencies:** SMP-US-003  

**User Story:** As a developer, I want a Spring Boot 3.x Java 21 backend skeleton so that backend development can start consistently.

**Acceptance Criteria:**
- Maven project builds successfully
- Package structure includes config, controller, service, repository, entity, dto, mapper, exception, security, audit and util
- Spring Boot application starts with dev profile
- Actuator health endpoint is available
- No business APIs are implemented beyond health and base configuration

## SMP-US-006 - Create Angular frontend shell

**Module:** M01  
**Persona:** Frontend Developer  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 1  
**Component:** Frontend  
**Dependencies:** SMP-US-003  

**User Story:** As a developer, I want an Angular 17+ application shell so that feature screens can be added consistently.

**Acceptance Criteria:**
- Angular app builds successfully
- Angular Material is installed
- Header, sidebar and content layout are created
- Environment files for dev, qa and prod exist
- Initial routes are created for login and dashboard placeholders

## SMP-US-007 - Configure backend profiles and common properties

**Module:** M01  
**Persona:** Developer  
**Priority:** Highest  
**Story Points:** 3  
**Demo Milestone:** Demo 1  
**Component:** Backend  
**Dependencies:** SMP-US-005  

**User Story:** As a developer, I want dev, qa and prod profiles so that each environment can use correct configuration.

**Acceptance Criteria:**
- application.yml contains common settings
- application-dev.yml uses local MySQL
- application-qa.yml and application-prod.yml use environment variables
- Hibernate ddl-auto is validate
- Sensitive values are not hardcoded in qa/prod

## SMP-US-008 - Configure Swagger/OpenAPI

**Module:** M01  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 2  
**Demo Milestone:** Demo 1  
**Component:** Backend  
**Dependencies:** SMP-US-005  

**User Story:** As a developer, I want Swagger/OpenAPI enabled so that APIs are easy to test and share.

**Acceptance Criteria:**
- Swagger UI is accessible in dev
- API metadata includes project name and version
- Auth header support is configured
- Controllers later added will appear automatically
- Swagger is disabled or protected in prod as per environment policy

## SMP-US-009 - Implement global exception and API response model

**Module:** M01  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 3  
**Demo Milestone:** Demo 1  
**Component:** Backend  
**Dependencies:** SMP-US-005  

**User Story:** As a developer, I want standard API success and error responses so that frontend error handling is consistent.

**Acceptance Criteria:**
- GlobalExceptionHandler is created
- Validation errors return field-level messages
- Not found returns 404
- Unauthorized returns 401/403
- Correlation ID is included in error response

## SMP-US-010 - Configure logging and correlation ID

**Module:** M01  
**Persona:** Developer  
**Priority:** Medium  
**Story Points:** 3  
**Demo Milestone:** Demo 1  
**Component:** Backend  
**Dependencies:** SMP-US-009  

**User Story:** As a support engineer, I want every request traceable with a correlation ID so that defects can be investigated quickly.

**Acceptance Criteria:**
- Correlation ID filter generates ID if missing
- Correlation ID appears in logs
- API response includes correlation ID for failures
- No passwords or tokens are logged
- Unit test covers correlation ID generation

## SMP-US-011 - Create local build and run instructions

**Module:** M18  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 2  
**Demo Milestone:** Demo 1  
**Component:** Documentation  
**Dependencies:** SMP-US-005,SMP-US-006  

**User Story:** As a new team member, I want simple local setup instructions so that I can run backend, frontend and database without dependency confusion.

**Acceptance Criteria:**
- README explains prerequisites
- Commands are provided for MySQL setup
- Commands are provided for backend start
- Commands are provided for frontend start
- Troubleshooting section covers common errors


# E03 - Database, Flyway and Reference Data

## SMP-US-012 - Create local MySQL database scripts

**Module:** M01  
**Persona:** Developer  
**Priority:** Highest  
**Story Points:** 3  
**Demo Milestone:** Demo 1  
**Component:** Database  
**Dependencies:** SMP-US-001  

**User Story:** As a developer, I want local database setup scripts so that the database can be created consistently.

**Acceptance Criteria:**
- 00_create_database.sql creates skill_matrix_db
- 01_create_local_user.sql creates local user with agreed username/password
- 02_drop_database.sql is available for dev reset
- README_DB_SETUP.md matches the script credentials
- Scripts are not used for production credentials

## SMP-US-013 - Finalize V01 initial schema migration

**Module:** M01  
**Persona:** Developer  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 1  
**Component:** Database  
**Dependencies:** SMP-US-012  

**User Story:** As a developer, I want the full DDL converted into Flyway V01 so that schema creation is automated.

**Acceptance Criteria:**
- V01__initial_schema.sql contains tables, PKs, FKs, indexes and constraints
- No seed data is mixed into V01 except strictly required technical defaults
- refresh_tokens table exists
- users supports must_change_password and account lock fields
- Flyway migration succeeds on a fresh database

## SMP-US-014 - Create V02 master reference seed data

**Module:** M02  
**Persona:** Developer  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 1  
**Component:** Database  
**Dependencies:** SMP-US-013  

**User Story:** As an Admin, I want baseline lookup, role, rating and application hierarchy reference data seeded so that screens can show valid dropdowns.

**Acceptance Criteria:**
- Roles ADMIN, LEAD_MANAGER and TECHNICIAN are seeded
- Lookup types and lookup values are seeded
- Rating scale 1 to 5 is seeded
- Account, Application Types WEB/HOST, Bundles B06/B12/B20 and portfolios are seeded
- Seed script is idempotent for clean Flyway execution

## SMP-US-015 - Create V03 dev demo data

**Module:** M01  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 1  
**Component:** Database  
**Dependencies:** SMP-US-014  

**User Story:** As a demo user, I want realistic demo data so that sprint demos can be shown before full production data is available.

**Acceptance Criteria:**
- Demo data exists only in dev-migration
- Demo users include Admin, Lead Manager and Technicians
- Demo ATLAS-deZentral application data is available
- Demo data supports login, assignment, assessment and review workflow
- Production profile does not execute dev demo data

## SMP-US-016 - Create verification query suite

**Module:** M01  
**Persona:** Developer  
**Priority:** Medium  
**Story Points:** 2  
**Demo Milestone:** Demo 1  
**Component:** Database  
**Dependencies:** SMP-US-014  

**User Story:** As a developer, I want verification queries so that DB setup can be validated after migration.

**Acceptance Criteria:**
- verification_queries.sql checks table count
- It verifies lookup data
- It verifies roles and rating scale
- It verifies sample users and application data
- README explains expected results

## SMP-US-017 - Add Flyway Testcontainers validation test

**Module:** M01  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 1  
**Component:** QA  
**Dependencies:** SMP-US-013  

**User Story:** As a developer, I want an integration test that runs Flyway on MySQL so that migration failures are caught early.

**Acceptance Criteria:**
- Testcontainers MySQL starts during integration test
- Flyway migrations execute successfully
- Expected table count is asserted
- Seed data count is asserted
- Test runs in CI pipeline profile


# E04 - Authentication, RBAC and First Login Security

## SMP-US-018 - Implement company user ID login API

**Module:** M17  
**Persona:** User  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 2  
**Component:** Backend  
**Dependencies:** SMP-US-013  

**User Story:** As a user, I want to log in using my company user ID so that my identity aligns with corporate standards.

**Acceptance Criteria:**
- Login request accepts companyUserId and password
- companyUserId maps to users.username
- Invalid credentials return generic error
- Inactive users cannot login
- Successful login returns user public_id, full name and roles

## SMP-US-019 - Implement password hashing and first login password change

**Module:** M17  
**Persona:** Security Admin  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 2  
**Component:** Backend  
**Dependencies:** SMP-US-018  

**User Story:** As a Security Admin, I want temporary passwords to force immediate password change so that bulk-created accounts are secure.

**Acceptance Criteria:**
- Passwords are stored only as BCrypt hashes
- must_change_password is true for bulk-created users
- User is redirected to change password after first login
- All other pages/APIs are blocked until password is changed
- After change, must_change_password becomes false and audit log is created

## SMP-US-020 - Implement JWT access token and refresh token flow

**Module:** M17  
**Persona:** User  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 2  
**Component:** Backend  
**Dependencies:** SMP-US-018,SMP-US-013  

**User Story:** As a user, I want a secure session so that I can use the application without repeatedly logging in.

**Acceptance Criteria:**
- Access token is short-lived
- Refresh token is stored hashed in database
- Refresh API issues new access token
- Logout revokes refresh token
- Expired/tampered tokens return 401

## SMP-US-021 - Implement account lockout after failed attempts

**Module:** M17  
**Persona:** Security Admin  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 2  
**Component:** Backend  
**Dependencies:** SMP-US-018  

**User Story:** As a Security Admin, I want accounts locked after repeated failures so that brute-force attempts are reduced.

**Acceptance Criteria:**
- Failed login count increments on invalid password
- Account locks after configured threshold
- Locked account cannot login until lock expires or admin unlocks
- Successful login resets failed count
- Audit logs success and failure events

## SMP-US-022 - Implement frontend login and change password screens

**Module:** M17  
**Persona:** User  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 2  
**Component:** Frontend  
**Dependencies:** SMP-US-018,SMP-US-019  

**User Story:** As a user, I want clear login and password change screens so that I can access the platform securely.

**Acceptance Criteria:**
- Login page label says Company User ID
- Password field is masked
- First login redirects to change password screen
- Change password validates current password and new password rules
- After password change user is redirected by role

## SMP-US-023 - Implement role guards and sidebar visibility

**Module:** M17  
**Persona:** User  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 2  
**Component:** Frontend  
**Dependencies:** SMP-US-020,SMP-US-006  

**User Story:** As a user, I want to see only the menus allowed for my role so that the application is simple and secure.

**Acceptance Criteria:**
- Admin sees admin menus
- Lead Manager sees review, gap, dashboard and reports menus
- Technician sees My Assessments and Notifications only
- Direct URL access without permission returns 403
- Unit tests cover guard behavior

## SMP-US-024 - Add AuthService and security tests

**Module:** M17  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 2  
**Component:** QA  
**Dependencies:** SMP-US-018,SMP-US-020  

**User Story:** As a developer, I want JUnit and Mockito tests for authentication so that login security stays reliable.

**Acceptance Criteria:**
- AuthServiceTest covers valid login
- Invalid password is tested
- Inactive user is tested
- Account lockout is tested
- Refresh and logout are tested
- MockMvc tests verify 401 and 403


# E05 - Lookup and Application Hierarchy

## SMP-US-025 - Implement lookup management APIs

**Module:** M02  
**Persona:** Admin  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 3  
**Component:** Backend  
**Dependencies:** SMP-US-014,SMP-US-020  

**User Story:** As an Admin, I want to manage lookup values so that statuses and dropdowns can evolve without DDL changes.

**Acceptance Criteria:**
- List lookup types API works
- List active lookup values by type works
- Create/update lookup value works
- System values cannot be deleted
- Audit log is created for changes

## SMP-US-026 - Implement lookup management UI

**Module:** M02  
**Persona:** Admin  
**Priority:** Medium  
**Story Points:** 5  
**Demo Milestone:** Demo 3  
**Component:** Frontend  
**Dependencies:** SMP-US-025  

**User Story:** As an Admin, I want lookup management screens so that I can maintain configurable values.

**Acceptance Criteria:**
- Lookup types list is displayed
- Lookup values are displayed by selected type
- Create/edit dialog validates required fields
- Inactive values are hidden from active dropdowns
- Error messages are shown clearly

## SMP-US-027 - Implement application type, bundle and portfolio APIs

**Module:** M03  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 3  
**Component:** Backend  
**Dependencies:** SMP-US-014  

**User Story:** As an Admin, I want APIs for application type, bundle and portfolio so that Web/Host and B06/B12/B20 hierarchy is controlled centrally.

**Acceptance Criteria:**
- GET application types returns WEB and HOST
- GET bundles supports applicationTypeCode filter
- Portfolio list supports account, application type and bundle
- Only active values appear by default
- Deprecated/future values can be included when requested

## SMP-US-028 - Implement application CRUD APIs

**Module:** M03  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 3  
**Component:** Backend  
**Dependencies:** SMP-US-027  

**User Story:** As an Admin, I want to create and maintain applications under portfolio so that changing applications does not require DDL changes.

**Acceptance Criteria:**
- Create application under selected portfolio
- Duplicate application code in same portfolio is blocked
- Update application details works
- Deprecate application without physical delete
- Historical reports can include deprecated apps

## SMP-US-029 - Implement cascading application dropdown APIs

**Module:** M03  
**Persona:** User  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 3  
**Component:** Backend  
**Dependencies:** SMP-US-027,SMP-US-028  

**User Story:** As a user, I want Application Type, Bundle and Application dropdowns so that I can select the correct application context.

**Acceptance Criteria:**
- WEB + B06 returns only B06 Web apps
- HOST + B06 returns only B06 Host apps
- WEB + B12 returns only B12 Web apps
- HOST + B12 returns only B12 Host apps
- B20 is future-ready
- Deprecated apps are hidden by default

## SMP-US-030 - Implement application hierarchy UI

**Module:** M03  
**Persona:** Admin  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 3  
**Component:** Frontend  
**Dependencies:** SMP-US-027,SMP-US-028,SMP-US-029  

**User Story:** As an Admin, I want application hierarchy screens so that I can manage Web/Host, bundles, portfolios and applications.

**Acceptance Criteria:**
- Application Type and Bundle lists are visible
- Portfolio screen filters by account/type/bundle
- Application screen has cascading filters
- Create/edit application dialog validates duplicate codes
- Lifecycle status is shown with chips

## SMP-US-031 - Add application hierarchy tests

**Module:** M03  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 3  
**Component:** QA  
**Dependencies:** SMP-US-027,SMP-US-029  

**User Story:** As a developer, I want tests for application hierarchy so that dropdown and lifecycle behavior remains stable.

**Acceptance Criteria:**
- Service tests cover WEB/HOST and B06/B12/B20 filters
- Duplicate application code test exists
- Deprecated app hidden/visible logic is tested
- Repository queries are integration-tested where needed
- MockMvc tests cover API authorization


# E06 - User, Team and Technician Assignment

## SMP-US-032 - Implement user management APIs

**Module:** M04  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 4  
**Component:** Backend  
**Dependencies:** SMP-US-020  

**User Story:** As an Admin, I want to create and maintain users so that all technicians, leads and admins are available in the system.

**Acceptance Criteria:**
- Create user with Company User ID, full name, email, employee ID and active flag
- Duplicate company user ID is blocked
- Duplicate email is blocked
- Update user works
- Deactivate user prevents login

## SMP-US-033 - Implement role assignment APIs

**Module:** M04  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 4  
**Component:** Backend  
**Dependencies:** SMP-US-032  

**User Story:** As an Admin, I want to assign system roles so that users get correct access.

**Acceptance Criteria:**
- Assign ADMIN, LEAD_MANAGER or TECHNICIAN role
- One primary role can be set
- Multiple roles are supported
- Role changes are audited
- User roles are returned in auth/me response

## SMP-US-034 - Implement team management APIs

**Module:** M04  
**Persona:** Admin  
**Priority:** Medium  
**Story Points:** 5  
**Demo Milestone:** Demo 4  
**Component:** Backend  
**Dependencies:** SMP-US-032  

**User Story:** As an Admin, I want to create teams and map users so that lead/team ownership can be tracked.

**Acceptance Criteria:**
- Create/update team works
- Map users to team works
- Mark team lead works
- Team filter is available in user list
- Audit log is created

## SMP-US-035 - Implement user management UI

**Module:** M04  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 4  
**Component:** Frontend  
**Dependencies:** SMP-US-032,SMP-US-033  

**User Story:** As an Admin, I want a user management screen so that I can manage 140 technicians and leads.

**Acceptance Criteria:**
- User list supports search/filter by Company User ID, name, email, role and status
- Create/edit user form is available
- Assign roles and teams from UI
- Deactivate/reactivate user from UI
- Validation messages are clear

## SMP-US-036 - Implement User Master Excel import

**Module:** M14  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 4  
**Component:** Backend/Frontend  
**Dependencies:** SMP-US-032,SMP-US-019  

**User Story:** As an Admin, I want to bulk upload 140 technicians so that initial onboarding is faster and controlled.

**Acceptance Criteria:**
- Downloadable User Master template exists
- Upload validates Company User ID, email, role and active flag
- System creates users with generated/temporary password hash
- must_change_password is set for imported users
- Import history captures total, success and error rows

## SMP-US-037 - Implement technician application assignment APIs

**Module:** M05  
**Persona:** Admin / Lead Manager  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 4  
**Component:** Backend  
**Dependencies:** SMP-US-029,SMP-US-032  

**User Story:** As an Admin or Lead Manager, I want to assign technicians to one or more applications so that their assessment scope is controlled.

**Acceptance Criteria:**
- Assign user to application with role_on_app, allocation and effective dates
- Same user can support multiple applications
- Duplicate active assignment is blocked
- Expired assignments are excluded from active views
- Assignment history is retained

## SMP-US-038 - Implement technician assignment UI

**Module:** M05  
**Persona:** Admin / Lead Manager  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 4  
**Component:** Frontend  
**Dependencies:** SMP-US-037  

**User Story:** As an Admin or Lead Manager, I want a technician assignment screen so that I can maintain application responsibility.

**Acceptance Criteria:**
- Cascading filter Application Type -> Bundle -> Application works
- Technician autocomplete works
- Role on Application dropdown includes PRIMARY, BACKUP, SME, TRAINEE and other configured values
- Allocation and effective dates are validated
- Deactivation works without physical delete

## SMP-US-039 - Implement Technician Assignment Excel import

**Module:** M14  
**Persona:** Admin  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 4  
**Component:** Backend/Frontend  
**Dependencies:** SMP-US-036,SMP-US-037  

**User Story:** As an Admin, I want to bulk upload technician application assignments so that multi-application mappings can be created quickly.

**Acceptance Criteria:**
- Template uses Company User ID and Application Code, not numeric IDs
- Validation confirms application belongs to selected type and bundle
- Role_on_app is validated against lookup
- Allocation above 100 per user shows warning or configurable error
- All-or-nothing import behavior is applied

## SMP-US-040 - Add user and assignment tests

**Module:** M04/M05  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 4  
**Component:** QA  
**Dependencies:** SMP-US-032,SMP-US-037,SMP-US-036  

**User Story:** As a developer, I want JUnit/Mockito tests for users and assignments so that onboarding logic is stable.

**Acceptance Criteria:**
- UserServiceTest covers create/update/deactivate
- Duplicate username/email tests exist
- UserApplicationMappingServiceTest covers multi-application assignment
- Invalid allocation and date validation tests exist
- Import service tests cover valid and invalid files


# E07 - Skill Master and Rating Scale

## SMP-US-041 - Implement skill category APIs

**Module:** M06  
**Persona:** Admin  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 5  
**Component:** Backend  
**Dependencies:** SMP-US-025  

**User Story:** As an Admin, I want to manage skill categories so that skills can be grouped properly.

**Acceptance Criteria:**
- Create/update skill category works
- Duplicate category code is blocked
- Deactivate category is supported if unused or per business rule
- Skill categories are returned for dropdowns
- Audit log is created

## SMP-US-042 - Implement skill master APIs with three-category model

**Module:** M06  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 5  
**Component:** Backend  
**Dependencies:** SMP-US-041  

**User Story:** As an Admin, I want to manage common technical skills, application know-how and VW tools without changing DDL.

**Acceptance Criteria:**
- Skill supports category group TECHNICAL_SKILL, APP_KNOWLEDGE and VW_TOOL
- Skill type supports TECHNICAL, PROCESS, APP_KNOWLEDGE and TOOL
- Scope supports GLOBAL, WEB, HOST, VW_GLOBAL and APP-specific values
- Duplicate skill code is blocked
- Inactive skills are hidden from new requirement setup

## SMP-US-043 - Implement rating scale APIs

**Module:** M06  
**Persona:** Admin / User  
**Priority:** Medium  
**Story Points:** 3  
**Demo Milestone:** Demo 5  
**Component:** Backend  
**Dependencies:** SMP-US-014  

**User Story:** As a user, I want to see the 1 to 5 rating scale so that skill ratings are understood consistently.

**Acceptance Criteria:**
- Rating scale API returns levels 1 to 5
- Each level includes name and description
- Rating scale is read-only for most users
- Admin can view rating details
- Frontend can reuse rating scale in dropdowns

## SMP-US-044 - Implement skill management UI

**Module:** M06  
**Persona:** Admin  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 5  
**Component:** Frontend  
**Dependencies:** SMP-US-042  

**User Story:** As an Admin, I want skill category and skill screens so that I can maintain the skill catalogue.

**Acceptance Criteria:**
- Skill category list and edit screens exist
- Skill list filters by category, type, scope and active status
- Skill form supports skill category group, type and scope
- Technical skills are clearly marked as reusable/global
- Duplicate errors are shown clearly

## SMP-US-045 - Implement Common Technical Skill Excel import

**Module:** M14  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 5  
**Component:** Backend/Frontend  
**Dependencies:** SMP-US-042  

**User Story:** As an Admin, I want to upload common technical skills once so that all applications can reuse them.

**Acceptance Criteria:**
- Template 01_COMMON_TECH_SKILLS is available
- Technical skills are inserted once into skills table
- No per-application duplicate technical skills are created
- Validation rejects duplicate skill codes
- Import preview shows valid and error rows

## SMP-US-046 - Implement Application Specific Skill Excel import

**Module:** M14  
**Persona:** Application Lead  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 5  
**Component:** Backend/Frontend  
**Dependencies:** SMP-US-042,SMP-US-029  

**User Story:** As an Application Lead, I want to upload application-specific know-how and VW tools for my application so that requirements can be tailored.

**Acceptance Criteria:**
- Template 02_APP_SPECIFIC_SKILLS_<APP> is supported
- Application-specific skills use APP:<Application_Code> scope
- VW tools can be VW_GLOBAL, WEB, HOST or APP-specific
- Validation checks application code if APP scope is used
- Import history is captured

## SMP-US-047 - Add skill master and skill import tests

**Module:** M06/M14  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 5  
**Component:** QA  
**Dependencies:** SMP-US-042,SMP-US-045,SMP-US-046  

**User Story:** As a developer, I want tests for skill APIs and imports so that skill catalogue quality is controlled.

**Acceptance Criteria:**
- SkillServiceTest covers create/update/deactivate
- Duplicate skill code test exists
- Common technical skill import prevents duplicates
- Application-specific skill import validates scope
- Invalid Excel file returns row-level errors


# E09 - Requirement Version and Expected Ratings

## SMP-US-048 - Implement requirement version APIs

**Module:** M07  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 6  
**Component:** Backend  
**Dependencies:** SMP-US-029,SMP-US-042  

**User Story:** As an Admin, I want to create requirement versions per application so that expected skills are version-controlled.

**Acceptance Criteria:**
- Create draft requirement version for application
- Version code is unique per application
- Effective dates are validated
- Draft version can be updated
- Approved version cannot be directly edited

## SMP-US-049 - Implement expected ratings APIs

**Module:** M07  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 6  
**Component:** Backend  
**Dependencies:** SMP-US-048  

**User Story:** As an Admin, I want to add expected skills and levels to a requirement version so that assessment criteria are defined.

**Acceptance Criteria:**
- Add skill to requirement version
- Same skill cannot be duplicated in same version
- Expected level must be 1 to 5
- Criticality, min_people and mandatory flag are required
- Expected ratings can be updated while version is Draft

## SMP-US-050 - Implement publish and approve requirement workflow

**Module:** M07  
**Persona:** Admin / Lead Manager  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 6  
**Component:** Backend  
**Dependencies:** SMP-US-048,SMP-US-049  

**User Story:** As an Admin and Lead Manager, I want requirement versions to be approved before assessment so that requirements are governed.

**Acceptance Criteria:**
- Admin can publish draft version
- Lead Manager can approve or reject
- Rejection requires reason
- Only approved versions can be selected in assessment cycle
- Audit log captures publish/approve/reject

## SMP-US-051 - Implement requirement version UI

**Module:** M07  
**Persona:** Admin / Lead Manager  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 6  
**Component:** Frontend  
**Dependencies:** SMP-US-048,SMP-US-050  

**User Story:** As an Admin or Lead Manager, I want a requirement version screen so that I can manage expected skills per application.

**Acceptance Criteria:**
- Filter by Application Type, Bundle and Application
- List shows version code, status, skills count and actions
- Draft version skill grid supports add/update/remove
- Lead Manager approval panel supports approve/reject
- Expected levels are visible to Admin/Lead Manager only

## SMP-US-052 - Implement Requirement Mapping Excel import

**Module:** M14  
**Persona:** Admin / Application Lead  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 6  
**Component:** Backend/Frontend  
**Dependencies:** SMP-US-045,SMP-US-046,SMP-US-048  

**User Story:** As an Admin, I want to upload application requirement mapping so that expected ratings can be created quickly.

**Acceptance Criteria:**
- Template 03_APP_REQUIREMENT_MAPPING_<APP> is supported
- Application Type, Bundle and Application Code are validated
- Common technical skills can be mapped without duplicating skills
- Application-specific skills can be mapped only to valid applications
- Requirement version and expected_ratings are created/updated in Draft only

## SMP-US-053 - Add requirement version tests

**Module:** M07/M14  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 6  
**Component:** QA  
**Dependencies:** SMP-US-048,SMP-US-050,SMP-US-052  

**User Story:** As a developer, I want tests for requirement version, expected rating and import behavior so that governance rules are safe.

**Acceptance Criteria:**
- RequirementVersionServiceTest covers create, publish, approve and reject
- Duplicate skill in version test exists
- Approved version edit is blocked
- Requirement import validates references
- Lead Manager security scope is tested


# E10 - Assessment Cycle

## SMP-US-054 - Implement assessment cycle APIs

**Module:** M08  
**Persona:** Admin  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 7  
**Component:** Backend  
**Dependencies:** SMP-US-050  

**User Story:** As an Admin, I want to create and manage assessment cycles so that technicians can submit within controlled periods.

**Acceptance Criteria:**
- Create cycle for approved requirement version
- Start and end dates are validated
- Only one open cycle per application is allowed or warned per configuration
- Open/close status changes work
- Closed cycles are read-only

## SMP-US-055 - Implement assessment cycle UI

**Module:** M08  
**Persona:** Admin  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 7  
**Component:** Frontend  
**Dependencies:** SMP-US-054  

**User Story:** As an Admin, I want an assessment cycle screen so that I can open and close assessment windows.

**Acceptance Criteria:**
- Filter by Application Type, Bundle and Application
- Cycle list shows status, dates and requirement version
- Create cycle form shows approved versions only
- Open/close actions are available
- Historical cycles are viewable

## SMP-US-056 - Add cycle tests

**Module:** M08  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 3  
**Demo Milestone:** Demo 7  
**Component:** QA  
**Dependencies:** SMP-US-054  

**User Story:** As a developer, I want tests for assessment cycles so that cycle rules are reliable.

**Acceptance Criteria:**
- Cannot create cycle with draft/rejected requirement version
- End date before start date is rejected
- Closed cycle blocks submission
- Open cycle retrieval works
- MockMvc security tests exist


# E11 - Technician Self Assessment

## SMP-US-057 - Implement assigned application API for technician

**Module:** M09  
**Persona:** Technician  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 7  
**Component:** Backend  
**Dependencies:** SMP-US-037,SMP-US-054  

**User Story:** As a Technician, I want to see only my active assigned applications so that I submit assessment only for my responsibilities.

**Acceptance Criteria:**
- API returns only active assignments
- Expired assignments are excluded
- Application dropdown uses WEB/HOST and bundle context
- Technician cannot see unassigned applications
- Admin/Lead Manager behavior is separately controlled

## SMP-US-058 - Implement self-assessment load API

**Module:** M09  
**Persona:** Technician  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 7  
**Component:** Backend  
**Dependencies:** SMP-US-054,SMP-US-057  

**User Story:** As a Technician, I want the system to load skills for my assigned application and open cycle so that I can rate myself.

**Acceptance Criteria:**
- API loads skills from approved requirement version
- Expected level is not returned in technician response
- Mandatory flag and skill category are returned
- Draft/submitted status is returned
- No open cycle returns clear message

## SMP-US-059 - Implement save draft self-assessment API

**Module:** M09  
**Persona:** Technician  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 7  
**Component:** Backend  
**Dependencies:** SMP-US-058  

**User Story:** As a Technician, I want to save draft ratings so that I can complete assessment in multiple sessions.

**Acceptance Criteria:**
- Draft save allows partial ratings
- Technician can update own draft
- Technician cannot update another user's draft
- Draft does not notify Lead Manager
- Audit log is captured as SAVE_DRAFT

## SMP-US-060 - Implement final submit self-assessment API

**Module:** M09  
**Persona:** Technician  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 7  
**Component:** Backend  
**Dependencies:** SMP-US-059  

**User Story:** As a Technician, I want to submit final assessment so that my Lead Manager can review it.

**Acceptance Criteria:**
- Mandatory skills require ratings
- Evidence is required for self-rating 4 or 5
- Submitted assessment becomes read-only
- Review task/notification is generated
- Submission after cycle close is blocked

## SMP-US-061 - Implement technician self-assessment UI

**Module:** M09  
**Persona:** Technician  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 7  
**Component:** Frontend  
**Dependencies:** SMP-US-058,SMP-US-060  

**User Story:** As a Technician, I want a simple self-assessment screen so that I can rate skills and provide evidence.

**Acceptance Criteria:**
- Application dropdown shows only assigned applications
- Skill grid shows category, skill, mandatory, self rating, evidence and comments
- Expected level column is never displayed
- Save Draft and Submit Final buttons work
- Status chip shows DRAFT, SUBMITTED, RETURNED or APPROVED

## SMP-US-062 - Add self-assessment tests including expected-level hiding

**Module:** M09/M17  
**Persona:** Developer  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 7  
**Component:** QA  
**Dependencies:** SMP-US-058,SMP-US-060  

**User Story:** As a developer, I want self-assessment tests so that technician privacy and workflow rules are enforced.

**Acceptance Criteria:**
- Technician assigned app filter is tested
- Expected level not present in API response test exists
- Draft save test exists
- Submit validation tests exist
- Closed cycle submission is blocked


# E12 - Lead Manager Review and Approval

## SMP-US-063 - Implement pending review list API

**Module:** M10  
**Persona:** Lead Manager  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 8  
**Component:** Backend  
**Dependencies:** SMP-US-060  

**User Story:** As a Lead Manager, I want to see technicians pending review for my applications so that I can prioritize approvals.

**Acceptance Criteria:**
- Pending list filters by application
- Only submitted assessments are shown
- Lead Manager sees assigned scope only
- Admin can see all scope
- Pagination and search work

## SMP-US-064 - Implement manager review grid API

**Module:** M10  
**Persona:** Lead Manager  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 8  
**Component:** Backend  
**Dependencies:** SMP-US-063  

**User Story:** As a Lead Manager, I want to load a submitted assessment with expected and self ratings so that I can review accurately.

**Acceptance Criteria:**
- Review grid shows expected level to Lead Manager
- Self rating, evidence and technician comments are visible
- Manager rating and comment are editable
- Row decision defaults to pending
- Technician cannot access review API

## SMP-US-065 - Implement row-level review decisions

**Module:** M10  
**Persona:** Lead Manager  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 8  
**Component:** Backend  
**Dependencies:** SMP-US-064  

**User Story:** As a Lead Manager, I want to approve, mark gap or request clarification per skill so that review is precise.

**Acceptance Criteria:**
- Row decision can be APPROVED, GAP_IDENTIFIED or CLARIFICATION_NEEDED
- Manager comment can be saved
- Final rating is stored
- Updates are audited
- Invalid rating outside 1 to 5 is rejected

## SMP-US-066 - Implement bulk review actions

**Module:** M10  
**Persona:** Lead Manager  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 8  
**Component:** Backend  
**Dependencies:** SMP-US-065  

**User Story:** As a Lead Manager, I want bulk approve/gap/clarification actions so that large assessments can be reviewed efficiently.

**Acceptance Criteria:**
- Bulk approve selected rows works
- Bulk gap selected rows works
- Bulk clarification selected rows works
- Partial selection is supported
- Each row action is auditable

## SMP-US-067 - Implement overall approval and return workflow

**Module:** M10  
**Persona:** Lead Manager  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 8  
**Component:** Backend  
**Dependencies:** SMP-US-065  

**User Story:** As a Lead Manager, I want to approve or return the full assessment so that the workflow moves forward correctly.

**Acceptance Criteria:**
- Approve Ratings locks assessment
- Approve triggers gap analysis
- Return sends assessment back to technician
- Overall note is stored
- Notification is created for technician

## SMP-US-068 - Implement Lead Manager review UI

**Module:** M10  
**Persona:** Lead Manager  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 8  
**Component:** Frontend  
**Dependencies:** SMP-US-064,SMP-US-067  

**User Story:** As a Lead Manager, I want a review screen with technician context and bulk actions so that I can efficiently validate assessments.

**Acceptance Criteria:**
- Filter by Application Type, Bundle, Application and Technician works
- Expected level is visible to Lead Manager
- Bulk toolbar appears when rows selected
- Overall decision panel is separate from row decisions
- Approve and Return actions work

## SMP-US-069 - Add review workflow tests

**Module:** M10  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 8  
**Component:** QA  
**Dependencies:** SMP-US-064,SMP-US-067  

**User Story:** As a developer, I want tests for Lead Manager review so that approvals are safe.

**Acceptance Criteria:**
- Pending review scope test exists
- Row decision update test exists
- Bulk action tests exist
- Approval triggers gap analysis test exists
- Return makes technician assessment editable test exists


# E13 - Gap Analysis and Training Recommendation

## SMP-US-070 - Implement gap calculation service

**Module:** M11  
**Persona:** System  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 8  
**Component:** Backend  
**Dependencies:** SMP-US-067  

**User Story:** As a system, I want to calculate gaps after approval so that skill risk is objectively captured.

**Acceptance Criteria:**
- Gap = expected level - final manager rating
- No gap is recorded as resolved/none when final >= expected
- Gap severity considers criticality
- Historical snapshots are not overwritten
- Calculation runs only after overall approval

## SMP-US-071 - Implement gap APIs

**Module:** M11  
**Persona:** Lead Manager  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 8  
**Component:** Backend  
**Dependencies:** SMP-US-070  

**User Story:** As a Lead Manager, I want to view gaps by application, cycle and technician so that I can take action.

**Acceptance Criteria:**
- Get gaps by cycle and technician works
- Get gaps by application works
- Get bundle-level gap summary works
- Filters use application type and bundle codes
- Only authorized scope is returned

## SMP-US-072 - Implement training recommendation generation

**Module:** M12  
**Persona:** System  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 9  
**Component:** Backend  
**Dependencies:** SMP-US-070  

**User Story:** As a system, I want to generate training recommendations from gaps so that skill improvement actions are visible.

**Acceptance Criteria:**
- Training is created only for valid gaps
- Priority maps from severity
- Target date maps to priority target days
- Technician is added as participant
- Duplicate recommendation for same gap is avoided

## SMP-US-073 - Implement gap and training UI

**Module:** M12  
**Persona:** Lead Manager  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 9  
**Component:** Frontend  
**Dependencies:** SMP-US-071,SMP-US-072  

**User Story:** As a Lead Manager, I want gap and training screens so that I can monitor improvement actions.

**Acceptance Criteria:**
- Gap screen shows summary cards and detail table
- Training screen filters by application, cycle, severity, priority and status
- Training target date is visible
- Confirm plan action works
- Export action is available when report export is ready

## SMP-US-074 - Add gap and training tests

**Module:** M11/M12  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 9  
**Component:** QA  
**Dependencies:** SMP-US-070,SMP-US-072  

**User Story:** As a developer, I want tests for gap and training logic so that management reports are accurate.

**Acceptance Criteria:**
- Gap severity mapping tests exist
- Training priority and target date tests exist
- No training for no-gap rows test exists
- Duplicate generation test exists
- Dashboard can consume gap snapshots


# E14 - Dashboard and Reports

## SMP-US-075 - Implement dashboard summary APIs

**Module:** M13  
**Persona:** Admin / Lead Manager  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 9  
**Component:** Backend  
**Dependencies:** SMP-US-070,SMP-US-072  

**User Story:** As management, I want summary metrics so that readiness and risk are visible at a glance.

**Acceptance Criteria:**
- Summary API returns total applications, assessed applications, readiness, open gaps and pending training
- Filters by Application Type and Bundle work
- Application-level filter works
- Metrics are based on approved assessment data
- Unauthorized users cannot view out-of-scope data

## SMP-US-076 - Implement readiness and coverage risk APIs

**Module:** M13  
**Persona:** Admin / Lead Manager  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 9  
**Component:** Backend  
**Dependencies:** SMP-US-075  

**User Story:** As management, I want readiness and coverage risk metrics so that backup gaps are identified early.

**Acceptance Criteria:**
- Readiness percentage is calculated correctly
- Coverage risk uses min_people from expected ratings
- SME/backup coverage metrics are available
- Historical cycle filters work
- Performance target is acceptable for initial data volume

## SMP-US-077 - Implement dashboard UI

**Module:** M13  
**Persona:** Admin / Lead Manager  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 9  
**Component:** Frontend  
**Dependencies:** SMP-US-075,SMP-US-076  

**User Story:** As management, I want a dashboard with cards, readiness table and heatmap so that I can present status to stakeholders.

**Acceptance Criteria:**
- Executive summary cards are displayed
- Filter bar supports Application Type, Bundle, Application and Cycle
- Application readiness table is shown
- Skill heatmap is shown
- Coverage risk panel highlights skills below threshold

## SMP-US-078 - Implement reports APIs

**Module:** M13  
**Persona:** Admin / Lead Manager  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 10  
**Component:** Backend  
**Dependencies:** SMP-US-075  

**User Story:** As management, I want gap, training and assessment reports so that data can be reviewed outside the tool.

**Acceptance Criteria:**
- Gap report endpoint supports filters
- Training demand report endpoint supports filters
- Assessment status report endpoint supports filters
- Pagination works
- Report data matches dashboard totals

## SMP-US-079 - Implement reports UI

**Module:** M13  
**Persona:** Admin / Lead Manager  
**Priority:** Medium  
**Story Points:** 5  
**Demo Milestone:** Demo 10  
**Component:** Frontend  
**Dependencies:** SMP-US-078  

**User Story:** As management, I want report screens with filters so that I can analyze gaps and training demand.

**Acceptance Criteria:**
- Gap report screen works
- Training demand report screen works
- Assessment status report screen works
- Filters persist during export
- Empty results show friendly message

## SMP-US-080 - Implement Excel export APIs

**Module:** M13/M14  
**Persona:** Admin / Lead Manager  
**Priority:** Medium  
**Story Points:** 5  
**Demo Milestone:** Demo 10  
**Component:** Backend  
**Dependencies:** SMP-US-078  

**User Story:** As a user, I want to export reports to Excel so that I can share them in management reviews.

**Acceptance Criteria:**
- Gap report exports to .xlsx
- Training plan exports to .xlsx
- Assessment status exports to .xlsx
- Excel headers and date formats are correct
- Export action is logged in import_export_history

## SMP-US-081 - Add dashboard and report tests

**Module:** M13  
**Persona:** Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 10  
**Component:** QA  
**Dependencies:** SMP-US-075,SMP-US-080  

**User Story:** As a developer, I want tests for dashboard and reports so that management metrics are trustworthy.

**Acceptance Criteria:**
- Readiness calculation tests exist
- Coverage risk tests exist
- Heatmap data tests exist
- Export file validity test exists
- Report filters are tested


# E08 - Excel Import and Export

## SMP-US-082 - Implement import history API and UI

**Module:** M14  
**Persona:** Admin  
**Priority:** Medium  
**Story Points:** 5  
**Demo Milestone:** Demo 10  
**Component:** Backend/Frontend  
**Dependencies:** SMP-US-036,SMP-US-045,SMP-US-052  

**User Story:** As an Admin, I want to see all import/export history so that data loads are traceable.

**Acceptance Criteria:**
- History list shows file name, entity type, operation, status, total/success/error rows and performed by
- Error details are viewable
- History is paginated
- Filter by operation/entity/status works
- Only Admin can access full history

## SMP-US-083 - Implement template download APIs

**Module:** M14  
**Persona:** Admin / Application Lead  
**Priority:** Medium  
**Story Points:** 5  
**Demo Milestone:** Demo 10  
**Component:** Backend/Frontend  
**Dependencies:** SMP-US-036,SMP-US-045,SMP-US-052  

**User Story:** As an Admin or Application Lead, I want to download fixed Excel templates so that uploaded data follows required structure.

**Acceptance Criteria:**
- User master template can be downloaded
- User assignment template can be downloaded
- Common technical skill template can be downloaded
- App-specific skill template can be downloaded
- Requirement mapping template can be downloaded


# E15 - Audit Log and Notifications

## SMP-US-084 - Implement audit logging service

**Module:** M15  
**Persona:** Admin  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 10  
**Component:** Backend  
**Dependencies:** SMP-US-009  

**User Story:** As an Admin, I want all business actions audited so that changes can be traced.

**Acceptance Criteria:**
- Audit service captures user, action, entity type, entity ID, old value, new value and timestamp
- Passwords and tokens are never logged
- Audit is written for create/update/submit/approve/reject/import/export
- Audit failure does not expose sensitive data
- Audit table has no update/delete APIs

## SMP-US-085 - Implement audit log UI

**Module:** M15  
**Persona:** Admin  
**Priority:** Medium  
**Story Points:** 5  
**Demo Milestone:** Demo 10  
**Component:** Frontend  
**Dependencies:** SMP-US-084  

**User Story:** As an Admin, I want an audit log screen so that I can investigate actions.

**Acceptance Criteria:**
- Audit table shows timestamp, user, action, entity type and summary
- Filters by date, user, action and entity type work
- Expand row shows old/new JSON
- Export audit works
- Access restricted to Admin or approved read-only roles

## SMP-US-086 - Implement notification creation service

**Module:** M16  
**Persona:** System  
**Priority:** Medium  
**Story Points:** 8  
**Demo Milestone:** Demo 10  
**Component:** Backend  
**Dependencies:** SMP-US-067,SMP-US-072  

**User Story:** As a system, I want to create notifications for key events so that users know what action is needed.

**Acceptance Criteria:**
- Assessment submitted creates Lead Manager notification
- Assessment returned creates Technician notification
- Assessment approved creates Technician notification
- Training plan generated creates Technician/Lead notification
- Notification failure does not roll back main transaction

## SMP-US-087 - Implement notification UI

**Module:** M16  
**Persona:** User  
**Priority:** Medium  
**Story Points:** 5  
**Demo Milestone:** Demo 10  
**Component:** Frontend  
**Dependencies:** SMP-US-086  

**User Story:** As a user, I want notification badges and a drawer so that I can see pending actions quickly.

**Acceptance Criteria:**
- Header badge count shows unread notifications
- Notification drawer shows recent notifications
- Mark as read works
- Notification click navigates to related screen
- All roles can access their own notifications

## SMP-US-088 - Add audit and notification tests

**Module:** M15/M16  
**Persona:** Developer  
**Priority:** Medium  
**Story Points:** 5  
**Demo Milestone:** Demo 10  
**Component:** QA  
**Dependencies:** SMP-US-084,SMP-US-086  

**User Story:** As a developer, I want tests for audit and notifications so that traceability works reliably.

**Acceptance Criteria:**
- Audit creation is tested
- Sensitive data exclusion is tested
- Notification generation by event is tested
- Notification failure without rollback is tested
- Security test verifies user sees own notifications only


# E16 - Testing, Quality and UAT

## SMP-US-089 - Create backend unit test suite quality gate

**Module:** M19  
**Persona:** QA / Developer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 11  
**Component:** QA  
**Dependencies:** SMP-US-024,SMP-US-031,SMP-US-040  

**User Story:** As a QA lead, I want backend unit test coverage enforced so that code quality is measurable.

**Acceptance Criteria:**
- JaCoCo is configured
- Overall backend coverage target is minimum 80%
- Service layer target is minimum 85%
- Build fails if quality gate fails
- Coverage report is published in CI

## SMP-US-090 - Create API test collection

**Module:** M19  
**Persona:** QA  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 11  
**Component:** QA  
**Dependencies:** SMP-US-080  

**User Story:** As a QA engineer, I want a Postman/API collection so that all APIs can be regression-tested.

**Acceptance Criteria:**
- Collection includes auth, master data, assignment, skills, requirements, assessment, review, gaps, reports and imports
- DEV and QA environment variables exist
- Positive and negative cases are covered
- Collection can run in CI
- API test report is generated

## SMP-US-091 - Create Angular unit and E2E test baseline

**Module:** M19  
**Persona:** QA / Frontend Developer  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 11  
**Component:** QA  
**Dependencies:** SMP-US-061,SMP-US-068  

**User Story:** As a QA engineer, I want frontend tests so that critical UI journeys are stable.

**Acceptance Criteria:**
- Auth guard tests exist
- Login screen tests exist
- Self-assessment UI tests exist
- Manager review UI tests exist
- E2E flow Login -> Submit -> Review -> Approve is automated

## SMP-US-092 - Execute security testing

**Module:** M19  
**Persona:** Security / QA  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 11  
**Component:** QA  
**Dependencies:** SMP-US-020,SMP-US-023  

**User Story:** As a security reviewer, I want security validation so that the production release is safe.

**Acceptance Criteria:**
- OWASP ZAP baseline scan executed
- JWT tampering test passes
- Role elevation test passes
- SQL injection checks are performed
- No sensitive data in logs is verified

## SMP-US-093 - Execute performance testing

**Module:** M19  
**Persona:** QA  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 11  
**Component:** QA  
**Dependencies:** SMP-US-080  

**User Story:** As a QA lead, I want performance testing so that the system handles initial user volume.

**Acceptance Criteria:**
- Login API target is validated
- Dropdown API performance is validated
- Self-assessment load target is validated
- Dashboard load target is validated
- Excel import 500 rows target is validated

## SMP-US-094 - Prepare UAT scenarios and sign-off pack

**Module:** M19  
**Persona:** Scrum Master / QA  
**Priority:** Highest  
**Story Points:** 5  
**Demo Milestone:** Demo 11  
**Component:** QA  
**Dependencies:** SMP-US-091  

**User Story:** As a Product Owner, I want UAT scenarios so that business users can validate the end-to-end workflow.

**Acceptance Criteria:**
- UAT scenarios cover Admin, Lead Manager and Technician
- ATLAS-deZentral pilot data is included
- Expected results are documented
- Defect tracking process is defined
- Sign-off template is prepared


# E17 - DevOps, Deployment and Production Readiness

## SMP-US-095 - Create CI/CD pipeline

**Module:** M18  
**Persona:** DevOps Engineer  
**Priority:** High  
**Story Points:** 8  
**Demo Milestone:** Demo 11  
**Component:** DevOps  
**Dependencies:** SMP-US-089  

**User Story:** As a DevOps engineer, I want automated build and deployment so that releases are repeatable.

**Acceptance Criteria:**
- Backend build runs mvn clean verify
- Frontend build runs npm test and ng build
- Docker image is built
- Artifacts are versioned
- Pipeline fails on test or quality gate failure

## SMP-US-096 - Containerize backend and frontend

**Module:** M18  
**Persona:** DevOps Engineer  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 11  
**Component:** DevOps  
**Dependencies:** SMP-US-005,SMP-US-006  

**User Story:** As a DevOps engineer, I want Docker packaging so that the application can be deployed consistently.

**Acceptance Criteria:**
- Backend Dockerfile is created
- Frontend build artifact deployment approach is documented
- Local docker-compose can run backend + database if required
- Health checks are configured
- No secrets are embedded in images

## SMP-US-097 - Prepare QA environment deployment

**Module:** M18  
**Persona:** DevOps Engineer  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 11  
**Component:** DevOps  
**Dependencies:** SMP-US-095,SMP-US-096  

**User Story:** As a QA team, I want a stable QA environment so that SIT and UAT can be executed.

**Acceptance Criteria:**
- QA backend is deployed
- QA frontend is deployed
- QA database migration is applied
- Environment variables/secrets are configured
- Smoke test passes

## SMP-US-098 - Prepare production environment deployment

**Module:** M18  
**Persona:** DevOps Engineer  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 12  
**Component:** DevOps  
**Dependencies:** SMP-US-097,SMP-US-094  

**User Story:** As the project team, I want production infrastructure prepared so that go-live can happen safely.

**Acceptance Criteria:**
- Production database is provisioned
- Secrets are configured securely
- Backend and frontend deployment runbook is ready
- Rollback plan is documented
- Smoke test checklist is ready

## SMP-US-099 - Create user guides and support runbook

**Module:** M20  
**Persona:** Scrum Master / Support Lead  
**Priority:** High  
**Story Points:** 5  
**Demo Milestone:** Demo 12  
**Component:** Documentation  
**Dependencies:** SMP-US-094  

**User Story:** As a support team, I want user guides and runbooks so that the application can be supported after go-live.

**Acceptance Criteria:**
- Admin guide is prepared
- Lead Manager guide is prepared
- Technician guide is prepared
- Production support runbook is prepared
- Known issues and FAQ are documented

## SMP-US-100 - Execute production go-live and handover

**Module:** M20  
**Persona:** Project Team  
**Priority:** Highest  
**Story Points:** 8  
**Demo Milestone:** Demo 12  
**Component:** Release  
**Dependencies:** SMP-US-098,SMP-US-099  

**User Story:** As the business owner, I want a controlled go-live so that users can start using the platform with minimal risk.

**Acceptance Criteria:**
- Production smoke test passes
- Initial admin users are validated
- Pilot application data is validated
- Support contact and escalation path are published
- Go-live sign-off is recorded


# E18 - Future SSO Readiness

## SMP-US-101 - Document future SSO integration approach

**Module:** M17  
**Persona:** Architect  
**Priority:** Medium  
**Story Points:** 3  
**Demo Milestone:** Post-Go-Live  
**Component:** Architecture  
**Dependencies:** SMP-US-018  

**User Story:** As an Architect, I want future SSO readiness documented so that local login can later be replaced with company identity provider integration.

**Acceptance Criteria:**
- Company User ID mapping to users.username is documented
- SSO claim mapping for username/email/employee ID is documented
- Local login fallback strategy is documented
- No direct company DB password validation is proposed
- Future user_external_identities migration is listed as optional

## SMP-US-102 - Create SSO integration spike

**Module:** M17  
**Persona:** Architect / Developer  
**Priority:** Medium  
**Story Points:** 5  
**Demo Milestone:** Post-Go-Live  
**Component:** Architecture  
**Dependencies:** SMP-US-101  

**User Story:** As a project team, I want a technical spike for SSO so that effort and security approvals are understood after MVP launch.

**Acceptance Criteria:**
- Identity provider option is identified
- OAuth2/OIDC or SAML approach is compared
- Required approvals and access are listed
- Impact on existing users table is assessed
- Implementation backlog is created


## Suggested Sprint Grouping

The demo milestones can be used as sprint or monthly demo groupings. If your team follows two-week sprints, Demo 1 to Demo 12 can be treated as sprint demos. If management prefers monthly demos, group two demo milestones into one monthly review.

## Final Recommendation

Start with Demo 0 and Demo 1. Do not begin UI-heavy stories until database, authentication, and API contracts are stable. For every sprint demo, prepare one pilot flow using ATLAS-deZentral before expanding to other applications.
