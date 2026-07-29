# Skill Matrix Platform — Module-Wise Requirements with JUnit & Mockito Test Plan

**Document Version:** 1.0  
**Date:** 30 July 2026  
**Technology Stack:** Angular 17+, Spring Boot 3.x, Java 21, MySQL 8, AWS  

---

## Table of Contents

1. [Module List](#module-list)
2. [M01 – Project Foundation and Standards](#m01--project-foundation-and-standards)
3. [M02 – Master Data and Lookup Management](#m02--master-data-and-lookup-management)
4. [M03 – Application Type, Bundle, Portfolio and Application](#m03--application-type-bundle-portfolio-and-application-management)
5. [M04 – User, Role, Permission and Team Management](#m04--user-role-permission-and-team-management)
6. [M05 – Technician Application Assignment](#m05--technician-application-assignment)
7. [M06 – Skill Category, Skill Catalogue and Rating Scale](#m06--skill-category-skill-catalogue-and-rating-scale)
8. [M07 – Application Skill Requirement and Version Management](#m07--application-skill-requirement-and-version-management)
9. [M08 – Assessment Cycle Management](#m08--assessment-cycle-management)
10. [M09 – Technician Self Assessment](#m09--technician-self-assessment)
11. [M10 – Lead Manager Review and Approval](#m10--lead-manager-review-and-approval)
12. [M11 – Skill Gap Analysis](#m11--skill-gap-analysis)
13. [M12 – Training Recommendation](#m12--training-recommendation)
14. [M13 – Dashboard and Reports](#m13--dashboard-and-reports)
15. [M14 – Excel Import and Export](#m14--excel-import-and-export)
16. [M15 – Audit Log and Activity Tracking](#m15--audit-log-and-activity-tracking)
17. [M16 – Notifications](#m16--notifications)
18. [M17 – Security, RBAC and Data Protection](#m17--security-rbac-and-data-protection)
19. [M18 – DevOps, Deployment and Environments](#m18--devops-deployment-and-environments)
20. [M19 – Testing and Quality Assurance](#m19--testing-and-quality-assurance)
21. [M20 – Handover and Operational Readiness](#m20--handover-and-operational-readiness)
22. [JUnit and Mockito Test Plan](#junit-and-mockito-test-plan)
23. [Module Dependency Sequence](#module-dependency-sequence)

---

## Module List

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

## M01 – Project Foundation and Standards

### Objective
Set up the base project structure, coding standards, development tools, CI/CD foundation, and standard architectural practices for backend, frontend, database, and deployment.

### Backend Requirements
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

### Frontend Requirements
1. Create Angular 17+ application.
2. Use Angular Material.
3. Use standalone components or feature-based modules.
4. Configure environment files: dev, qa, prod.
5. Configure route guards and role guards.
6. Configure HTTP interceptor.
7. Create shared UI components.
8. Create layout with sidebar, header, content area, and notification area.

### Database Requirements
1. Create Flyway migration folder.
2. Use versioned migration files (V01, V02, ...).
3. Do not modify deployed migration files.
4. Use new migration for every schema change.
5. Seed baseline master data.

### Step-by-Step Implementation
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

### Acceptance Criteria
- [ ] Backend starts with dev profile
- [ ] Frontend builds successfully
- [ ] MySQL connection works
- [ ] Flyway migration runs on startup
- [ ] Swagger is accessible at `/swagger-ui.html`
- [ ] Angular layout renders successfully
- [ ] Docker local environment starts backend, frontend, and MySQL

---

## M02 – Master Data and Lookup Management

### Objective
Provide configurable lookup values for statuses, decisions, lifecycle states, training types, priorities, notification statuses, and other business values to avoid future DDL changes.

### Key Tables
| Table | Purpose |
|---|---|
| `lookup_type` | Defines lookup groups |
| `lookup_value` | Defines values under each lookup group |

### Lookup Types
```
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

### Step-by-Step Functional Flow
1. Admin opens Lookup Management screen.
2. Admin selects a lookup type.
3. System shows existing lookup values.
4. Admin creates or updates a lookup value.
5. System validates duplicate value code.
6. Admin activates or deactivates lookup value.
7. System records audit log.
8. New lookup value becomes available in related dropdowns.

### APIs
```http
GET    /api/v1/lookup-types
GET    /api/v1/lookup-values?typeCode={typeCode}&active=true
POST   /api/v1/lookup-values
PUT    /api/v1/lookup-values/{publicId}
PATCH  /api/v1/lookup-values/{publicId}/status
```

### Business Rules
1. System-critical lookup values cannot be deleted.
2. Lookup values used in transactions cannot be physically deleted.
3. Deactivate lookup values instead of deleting.
4. Display order controls dropdown order.
5. Admin can manage all lookup values.

### Acceptance Criteria
- [ ] Admin can create lookup values
- [ ] Duplicate codes are blocked
- [ ] Deactivated values do not appear in active dropdowns
- [ ] Transaction history still displays old values correctly
- [ ] Audit log is created for every change

---

## M03 – Application Type, Bundle, Portfolio and Application Management

### Objective
Manage Web/Host application types, B06/B12/B20 bundles, application portfolios, and applications in a scalable way.

### Key Tables
| Table | Purpose |
|---|---|
| `accounts` | Top-level customer/account |
| `application_types` | Web, Host, future types |
| `bundles` | B06, B12, B20 |
| `application_portfolios` | Account + Application Type + Bundle combination |
| `applications` | Actual applications under a portfolio |

### Important Relationships
```
accounts            1 -> N  application_portfolios
application_types   1 -> N  application_portfolios
bundles             1 -> N  application_portfolios
application_portfolios 1 -> N  applications
```

### Cascading Dropdown APIs
```http
GET /api/v1/application-types?active=true
GET /api/v1/bundles?applicationTypeCode=WEB&includeFuture=true
GET /api/v1/applications?applicationTypeCode=WEB&bundleCode=B06&includeDeprecated=false
GET /api/v1/applications?applicationTypeCode=WEB&bundleCode=B06&includeDeprecated=true
```

### Application Dropdown Combinations
| Application Type | Bundle | Expected Applications |
|---|---|---|
| WEB | B06 | ~27+ applications |
| HOST | B06 | ~17+ applications |
| WEB | B12 | ~10+ applications |
| HOST | B12 | ~15+ applications |
| WEB | B20 | Future list |
| HOST | B20 | Future list |

### Acceptance Criteria
- [ ] WEB + B06 shows only Web applications under B06
- [ ] HOST + B06 shows only Host applications under B06
- [ ] WEB + B12 shows only Web applications under B12
- [ ] HOST + B12 shows only Host applications under B12
- [ ] Deprecated application is hidden from assignment dropdown by default
- [ ] Deprecated application appears in historical reports when requested
- [ ] Adding a new application does not require DDL change

---

## M04 – User, Role, Permission and Team Management

### Objective
Manage application users, roles, permissions, teams, reporting hierarchy, and user-team mapping.

### Key Tables
| Table | Purpose |
|---|---|
| `users` | Admin, Lead Manager, Technician |
| `roles` | ADMIN, LEAD_MANAGER, TECHNICIAN |
| `permissions` | Fine-grained permissions |
| `role_permissions` | Role to permission mapping |
| `user_roles` | User to role mapping |
| `teams` | Team master |
| `user_teams` | User to team mapping |

### Roles
| Role | Responsibilities |
|---|---|
| Admin | Full system configuration and management |
| Lead Manager | Review, approve, manage skill gaps, view dashboards |
| Technician | Submit self-assessment for assigned applications |

### APIs
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

### Acceptance Criteria
- [ ] Admin can create user successfully
- [ ] Duplicate username is blocked
- [ ] Duplicate email is blocked
- [ ] Inactive user cannot login
- [ ] Technician sees only technician screens
- [ ] Lead Manager sees only lead manager screens
- [ ] Admin sees all screens

---

## M05 – Technician Application Assignment

### Objective
Map technicians to one or more applications with role, allocation percentage, effective dates, and active status.

### Key Tables
| Table | Purpose |
|---|---|
| `user_application_mapping` | Technician to application mapping |

### APIs
```http
GET    /api/v1/user-application-mappings?applicationId=&userId=&active=true
POST   /api/v1/user-application-mappings
PUT    /api/v1/user-application-mappings/{publicId}
PATCH  /api/v1/user-application-mappings/{publicId}/deactivate
GET    /api/v1/applications/assigned
```

### Business Rules
1. Allocation percentage must be between 0 and 100.
2. Effective from date is mandatory.
3. Effective to date cannot be earlier than effective from date.
4. Same technician can be assigned to same application again only with a different effective period.
5. Technician self-assessment dropdown must use only active mappings.

### Acceptance Criteria
- [ ] Technician assigned to WEB + B06 + ATLAS can see ATLAS in dropdown
- [ ] Technician not assigned to HOST + B06 application cannot see that application
- [ ] Expired assignment is not shown in self-assessment dropdown
- [ ] Assignment history is available for reports

---

## M06 – Skill Category, Skill Catalogue and Rating Scale

### Rating Scale
| Level | Name | Meaning |
|---|---|---|
| 1 | Awareness | Understands basic concepts only |
| 2 | Working Knowledge | Can work with guidance |
| 3 | Independent | Can work independently on regular tasks |
| 4 | Advanced | Can handle complex scenarios and guide others |
| 5 | SME / Expert | Recognized expert; can mentor and own critical issues |

### Recommended Skill Categories
```
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

### APIs
```http
GET    /api/v1/skill-categories
POST   /api/v1/skill-categories
PUT    /api/v1/skill-categories/{publicId}
GET    /api/v1/skills?categoryId=&skillType=&active=true
POST   /api/v1/skills
PUT    /api/v1/skills/{publicId}
GET    /api/v1/rating-scale
```

### Acceptance Criteria
- [ ] Admin can create skill categories
- [ ] Admin can create skills
- [ ] Duplicate skill code is blocked
- [ ] Inactive skills are not shown in expected rating setup
- [ ] Rating scale is available across the platform

---

## M07 – Application Skill Requirement and Version Management

### Objective
Define expected skill requirements per application and manage them through versioning and approval workflow.

### Key Tables
| Table | Purpose |
|---|---|
| `requirement_versions` | Version header per application |
| `expected_ratings` | Skills and expected levels under version |

### APIs
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

### Business Rules
1. Same skill cannot be duplicated in same requirement version.
2. Expected level must be between 1 and 5.
3. Minimum people must be greater than or equal to 1.
4. Approved version cannot be directly edited.
5. Rejected version returns to Draft with rejection reason.

### Acceptance Criteria
- [ ] Requirement version can be created in Draft
- [ ] Skills can be added with expected levels
- [ ] Version can be published for approval
- [ ] Lead Manager can approve
- [ ] Lead Manager can reject with reason
- [ ] Only approved versions appear in assessment cycle setup

---

## M08 – Assessment Cycle Management

### Objective
Create and manage assessment cycles per application using approved requirement versions.

### Key Tables
| Table | Purpose |
|---|---|
| `assessment_cycles` | Assessment period header |

### APIs
```http
GET    /api/v1/applications/{applicationPublicId}/cycles
POST   /api/v1/cycles
PATCH  /api/v1/cycles/{publicId}/status
GET    /api/v1/cycles/open?applicationPublicId={applicationPublicId}
```

### Business Rules
1. Requirement version must be approved before use.
2. Start date cannot be after end date.
3. Only open cycles allow submission.
4. Closed cycles are read-only.
5. One open cycle per application is recommended.

### Acceptance Criteria
- [ ] Admin can create assessment cycle
- [ ] Draft/rejected requirement versions are not shown
- [ ] Technician cannot submit if no open cycle exists
- [ ] Closed cycle blocks submission
- [ ] Historical cycle data is retained

---

## M09 – Technician Self Assessment

### Objective
Allow technicians to submit ratings, evidence, and comments for assigned application skills.

### Key Tables
| Table | Purpose |
|---|---|
| `technician_assessments` | Technician rating per skill per cycle |

### APIs
```http
GET    /api/v1/assessments/my/applications
GET    /api/v1/assessments/my?applicationPublicId=&cyclePublicId=
PUT    /api/v1/assessments/my/draft
POST   /api/v1/assessments/my/submit
```

### Business Rules
1. Technician can see only assigned active applications.
2. **Technician cannot see expected rating in UI or API.**
3. Evidence is mandatory for high self-ratings (4 and 5).
4. Mandatory skills must have rating before final submission.
5. Submitted assessment becomes read-only.
6. Returned assessment becomes editable again.

### Acceptance Criteria
- [ ] Technician sees only assigned applications
- [ ] Technician cannot see expected level in UI
- [ ] Technician cannot see expected level in API response
- [ ] Draft can be saved
- [ ] Final submission creates review task
- [ ] Missing mandatory rating blocks final submission

---

## M10 – Lead Manager Review and Approval

### Objective
Allow Lead Managers to review technician submissions, approve ratings, identify gaps, request clarification, and trigger gap analysis.

### Key Tables
| Table | Purpose |
|---|---|
| `lead_reviews` | Row-level manager review |
| `assessment_approvals` | Overall form-level approval |

### APIs
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

### Business Rules
1. Lead Manager can review only assigned scope unless Admin.
2. Expected level is visible to Lead Manager.
3. Overall approval is separate from row decisions.
4. Gap analysis should run only after overall approval.
5. Returned assessment becomes editable to technician.

### Acceptance Criteria
- [ ] Lead Manager sees submitted technicians
- [ ] Lead Manager can update manager rating
- [ ] Lead Manager can mark row decision
- [ ] Bulk actions work correctly
- [ ] Overall approval triggers gap analysis
- [ ] Clarification returns assessment to technician

---

## M11 – Skill Gap Analysis

### Objective
Calculate and store skill gaps after Lead Manager approval.

### Gap Formula
```
Gap = Expected Level - Final Approved Rating
```
If Gap > 0, there is a skill gap.

### Severity Rules
| Condition | Severity |
|---|---|
| High criticality and gap > 0 | HIGH |
| Medium criticality and gap > 0 | MEDIUM |
| Low criticality and gap > 0 | LOW |
| No gap | NONE |

### Key Tables
| Table | Purpose |
|---|---|
| `skill_gap_snapshots` | Stores calculated gaps |

### APIs
```http
GET /api/v1/gaps?cyclePublicId=&technicianPublicId=
GET /api/v1/gaps/application?applicationPublicId=&cyclePublicId=
GET /api/v1/gaps/bundle?applicationTypeCode=&bundleCode=&cycleName=
```

### Business Rules
1. Gap snapshot is created only after approval.
2. Gap should be based on final manager rating, not self-rating.
3. Historical gaps should not be overwritten.
4. Dashboard should use latest approved snapshot unless historical cycle selected.

### Acceptance Criteria
- [ ] Approval creates gap snapshot
- [ ] Approved rows without gaps are not shown as open gaps
- [ ] Gap severity is calculated correctly
- [ ] Historical gaps are available by cycle

---

## M12 – Training Recommendation

### Objective
Generate training recommendations based on identified skill gaps.

### Target Rules
| Severity | Priority | Target Days |
|---|---|---|
| HIGH | HIGH | 30 days |
| MEDIUM | MEDIUM | 60 days |
| LOW | LOW | 90 days |

### Key Tables
| Table | Purpose |
|---|---|
| `training_recommendations` | Training plan per gap |
| `training_participants` | Users linked to training |

### APIs
```http
GET  /api/v1/training-recommendations?cyclePublicId=&technicianPublicId=
GET  /api/v1/training-recommendations/application?applicationPublicId=
POST /api/v1/training-recommendations/generate
POST /api/v1/training-recommendations/{publicId}/confirm
```

### Acceptance Criteria
- [ ] Training is generated only for gaps
- [ ] Priority is mapped correctly
- [ ] Target date is calculated correctly
- [ ] Technician is mapped as participant
- [ ] Training plan appears in dashboard and report

---

## M13 – Dashboard and Reports

### Key Metrics
| Metric | Meaning |
|---|---|
| Application Readiness % | Approved skills meeting expected level |
| Skill Gap Count | Count of skills below expected level |
| High Risk Skill Count | High criticality gaps |
| SME Coverage | Number of technicians at required level |
| Backup Coverage | Number of backup technicians |
| Training Demand | Open training recommendations |
| Assessment Completion | Submitted vs expected submissions |

### APIs
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

### Acceptance Criteria
- [ ] Dashboard filters work for WEB/HOST and B06/B12/B20
- [ ] Readiness percentage matches approved assessment data
- [ ] Skill heatmap shows correct technician ratings
- [ ] Coverage risk highlights skills below minimum people threshold
- [ ] Reports export correctly

---

## M14 – Excel Import and Export

### Import Template Columns — Application Master
```
application_type_code, bundle_code, application_code, application_name,
lifecycle_status, replacement_application_code, remarks
```

### Import Template Columns — Skill Requirement
```
application_type_code, bundle_code, application_code, requirement_version,
skill_category, skill_code, skill_name, expected_level, criticality,
min_people, mandatory_flag
```

### APIs
```http
GET  /api/v1/imports/templates/{templateType}
POST /api/v1/imports/applications
POST /api/v1/imports/skill-requirements
POST /api/v1/imports/assignments
GET  /api/v1/imports/history
GET  /api/v1/reports/{reportType}/export
```

### Business Rules
1. Import should be all-or-nothing by default.
2. Invalid rows should be reported with row number and message.
3. Existing active records should not be duplicated.
4. Imports should use business codes, not numeric IDs.

### Acceptance Criteria
- [ ] Valid file imports successfully
- [ ] Invalid file is rejected with clear error messages
- [ ] Import history is saved
- [ ] Export file downloads successfully
- [ ] Exported data matches selected filters

---

## M15 – Audit Log and Activity Tracking

### Actions to Audit
```
LOGIN_SUCCESS, LOGIN_FAILED, CREATE, UPDATE, DEACTIVATE,
PUBLISH, APPROVE, REJECT, SUBMIT, RETURN,
GENERATE_GAP, GENERATE_TRAINING, IMPORT, EXPORT
```

### Key Tables
| Table | Purpose |
|---|---|
| `audit_log` | Append-only business audit |

### APIs
```http
GET /api/v1/audit-log?fromDate=&toDate=&userPublicId=&entityType=&action=
GET /api/v1/audit-log/export?filters
```

### Business Rules
1. Audit log is append-only.
2. No update or delete endpoint for audit log.
3. Sensitive values such as passwords and tokens must not be logged.

### Acceptance Criteria
- [ ] Submit assessment creates audit log
- [ ] Manager approval creates audit log
- [ ] Requirement publish and approval create audit logs
- [ ] Import and export are logged
- [ ] Audit screen supports filters

---

## M16 – Notifications

### Notification Events
| Event | Recipient |
|---|---|
| Requirement published | Lead Manager |
| Requirement approved/rejected | Admin |
| Assessment submitted | Lead Manager |
| Assessment returned | Technician |
| Assessment approved | Technician |
| Training plan generated | Technician and Lead Manager |
| Cycle opened/closed | Assigned technicians and lead managers |

### Key Tables
| Table | Purpose |
|---|---|
| `notification_log` | Email and in-app notification tracking |

### APIs
```http
GET   /api/v1/notifications/my
PATCH /api/v1/notifications/{publicId}/read
GET   /api/v1/notifications/pending-count
GET   /api/v1/notifications/admin-log
```

### Business Rules
1. Notification failures should not block business transaction.
2. Email sending should be asynchronous.
3. Failed notifications are retried by scheduler.

---

## M17 – Security, RBAC and Data Protection

### Key Security Requirements
1. Username/password login with BCrypt password hashing.
2. JWT access token (short-lived, stored in memory).
3. Refresh token (HTTP-only, Secure, SameSite cookie).
4. Role-based and method-level authorization.
5. Expected rating hidden from technician at API level.
6. HTTPS only in production.
7. Secrets stored in AWS Secrets Manager.
8. Account lockout after repeated failed login.

### Role Access Matrix
| Module | Admin | Lead Manager | Technician |
|---|---|---|---|
| Master Data | Full | Limited | No |
| User Management | Full | No | No |
| Application Management | Full | Limited | No |
| Skill Requirement | Full | Approve/Reject | No |
| Self Assessment | View all | View submitted | Own only |
| Manager Review | Full | Assigned scope | No |
| Dashboard | Full | Assigned scope | Own view |
| Audit | Full | Read limited | No |

### Acceptance Criteria
- [ ] Unauthenticated API returns 401
- [ ] Unauthorized API returns 403
- [ ] Technician cannot access admin screens
- [ ] Technician cannot view expected rating in API response
- [ ] Passwords are never stored in plain text

---

## M18 – DevOps, Deployment and Environments

### AWS Components
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
| CI/CD | CodePipeline / GitHub Actions |

### Environments
| Environment | Purpose |
|---|---|
| DEV | Developer integration and unit testing |
| QA | Functional testing, SIT, UAT preparation |
| PROD | Production users |

---

## M19 – Testing and Quality Assurance

### Unit Test Coverage Targets
| Layer | Minimum Coverage |
|---|---:|
| Service layer | 85%+ |
| Utility / mapper logic | 80%+ |
| Controller layer | 70%+ |
| Overall backend | 80%+ |

### Performance Targets
| Area | Target |
|---|---|
| Login API | < 500 ms |
| Dropdown APIs | < 300 ms |
| Self assessment load | < 1 second |
| Manager review load | < 2 seconds |
| Dashboard load | < 3 seconds |
| Excel import 500 rows | < 30 seconds |
| Concurrent users | 50 initial |

---

## M20 – Handover and Operational Readiness

### Handover Deliverables
1. Architecture document
2. Database design document and ER diagram
3. API specification / Swagger URL
4. Deployment runbook
5. Production support runbook
6. User guides for Admin, Lead Manager, and Technician
7. Test case document and UAT sign-off
8. Known issues list and release notes
9. Rollback plan and monitoring dashboard details

---

## JUnit and Mockito Test Plan

### Recommended Testing Stack
| Area | Tool | Purpose |
|---|---|---|
| Unit testing | JUnit 5 | Service methods, validators, utility classes |
| Mocking | Mockito | Mock repositories, security context, external deps |
| Assertions | AssertJ | Clear and readable validation |
| REST API testing | MockMvc | Controller and security endpoint validation |
| DB integration | Testcontainers MySQL | Repository and Flyway validation |
| Coverage | JaCoCo | Enforce minimum coverage in CI |

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

### Mockito Testing Rules
1. Mock repositories, email service, audit service, notification service, and security context.
2. Do not mock the class under test.
3. Do not connect to MySQL in pure unit tests.
4. Do not call real AWS services in unit tests.
5. Validate both success and failure scenarios.
6. Verify that audit and notification methods are triggered where required.
7. Keep unit tests fast, deterministic, and independent.

### Module-Wise JUnit and Mockito Test Classes

| Module | Test Class | Key Scenarios |
|---|---|---|
| Auth / RBAC | `AuthServiceTest`, `JwtServiceTest` | Valid login, invalid login, inactive user, lockout, token generation |
| User Management | `UserServiceTest` | Create user, duplicate username/email, deactivate, role assignment |
| Application Type / Bundle | `ApplicationTypeServiceTest`, `BundleServiceTest` | Create WEB/HOST, create B06/B12/B20, prevent duplicate portfolio |
| Application Management | `ApplicationServiceTest` | Create app under portfolio, duplicate code prevention, lifecycle status |
| Technician Assignment | `UserApplicationMappingServiceTest` | Assign technician, invalid allocation, multi-application, filter assigned |
| Skill Master | `SkillServiceTest`, `SkillCategoryServiceTest` | Create skill, duplicate code, deactivate, filter by category |
| Requirement Version | `RequirementVersionServiceTest` | Create draft, publish, approve, reject, prevent duplicate skill |
| Assessment Cycle | `AssessmentCycleServiceTest` | Create cycle, one open cycle per application, close cycle |
| Self Assessment | `AssessmentServiceTest` | Load skills, save draft, submit, mandatory validation, hide expected level |
| Manager Review | `ReviewServiceTest` | Load pending, update rating, bulk approve, gap identified, return |
| Gap Analysis | `GapAnalysisServiceTest` | Calculate gap, severity mapping, create snapshot after approval |
| Training | `TrainingRecommendationServiceTest` | Generate training, priority mapping, target date, participant mapping |
| Dashboard | `DashboardServiceTest` | Readiness percentage, coverage risk, heatmap data |
| Import / Export | `ImportServiceTest`, `ExportServiceTest` | Validate rows, reject invalid file, all-or-nothing import |
| Audit Log | `AuditLogServiceTest` | Create entry, capture old/new values, block update/delete |
| Notification | `NotificationServiceTest` | Create notification, async email, handle failure without rollback |

### Must-Have Unit Test Scenarios
1. Technician can only retrieve assigned applications.
2. Technician API response must not expose expected rating.
3. Non-approved requirement version cannot be used for assessment cycle.
4. Only one open cycle is allowed per application.
5. Technician cannot submit assessment for a closed cycle.
6. Rating 4 or 5 requires evidence.
7. Manager review approval triggers gap analysis.
8. Gap analysis creates snapshots only for gap-identified rows.
9. Training recommendations are created only for valid gaps.
10. Audit log is created for submit, approve, reject, return, and update actions.
11. Notification failure must not roll back main business transaction.
12. Deprecated applications are excluded from active dropdowns.
13. B06/B12/B20 and WEB/HOST filters return correct application lists.

### Controller Security Tests (MockMvc)
| API Area | Required Tests |
|---|---|
| Auth APIs | Login success, invalid credentials, inactive user, refresh token, logout |
| Admin APIs | Technician receives 403, Lead Manager receives 403 where not allowed |
| Technician APIs | Can access `/assessments/my`, cannot access `/reviews` or `/admin` |
| Lead Manager APIs | Can access review APIs only for assigned application scope |
| Token validation | Missing token 401, expired token 401, tampered token 401 |

### JaCoCo CI Quality Gate
```
Overall backend coverage : minimum 80%
Service layer coverage   : minimum 85%
Critical workflow services: minimum 90% recommended
```

---

## Module Dependency Sequence

Recommended development order:

```
1.  M01 - Project Foundation
2.  M02 - Lookup Management
3.  M03 - Application Type / Bundle / Portfolio / Application
4.  M04 - User / Role / Permission / Team
5.  M06 - Skill Master and Rating Scale
6.  M05 - Technician Application Assignment
7.  M07 - Requirement Version and Expected Rating
8.  M08 - Assessment Cycle
9.  M09 - Technician Self Assessment
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

*End of Document*
