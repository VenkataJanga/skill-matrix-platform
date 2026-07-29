# Skill Matrix Platform — UI Screen Details

**Document Version:** 1.0  
**Date:** 30 July 2026  
**Framework:** Angular 17+ with Angular Material  

---

## Table of Contents

1. [Application Layout](#1-application-layout)
2. [Login Screen](#2-login-screen)
3. [Lookup Management Screen](#3-lookup-management-screen)
4. [Application Type / Bundle / Portfolio / Application Screens](#4-application-hierarchy-screens)
5. [User Management Screen](#5-user-management-screen)
6. [Technician Assignment Screen](#6-technician-assignment-screen)
7. [Skill Category and Skill Management Screen](#7-skill-category-and-skill-management-screen)
8. [Requirement Version Screen](#8-requirement-version-screen)
9. [Assessment Cycle Screen](#9-assessment-cycle-screen)
10. [Technician Self Assessment Screen](#10-technician-self-assessment-screen)
11. [Lead Manager Review Screen](#11-lead-manager-review-screen)
12. [Gap Analysis Screen](#12-gap-analysis-screen)
13. [Training Recommendation Screen](#13-training-recommendation-screen)
14. [Dashboard Screen](#14-dashboard-screen)
15. [Import / Export Screen](#15-import--export-screen)
16. [Audit Log Screen](#16-audit-log-screen)
17. [Notifications](#17-notifications)

---

## 1. Application Layout

### Shell Components

| Component | Description |
|---|---|
| **Header** | Logo, app name, logged-in user name, role badge, notification bell icon with count, logout button |
| **Sidebar** | Role-based navigation menu, collapsible, active state highlight |
| **Content Area** | Main router outlet for feature screens |
| **Notification Drawer** | Slide-in panel showing recent notifications on bell click |
| **Footer** | App version, environment label (DEV / QA) |

### Sidebar Navigation by Role

**Admin Sidebar:**
```
Dashboard
Master Data
  ├── Lookup Management
  ├── Application Types
  ├── Bundles
  ├── Portfolios
  └── Applications
User Management
Skill Management
  ├── Skill Categories
  └── Skills
Requirement Versions
Assessment Cycles
Technician Assignments
Reports
  ├── Gap Report
  ├── Training Demand
  └── Assessment Status
Import / Export
Audit Log
Notifications
```

**Lead Manager Sidebar:**
```
Dashboard
Pending Reviews
  └── Manager Review
Requirement Versions (Approve/Reject)
Gap Analysis
Training Plans
Reports
Notifications
```

**Technician Sidebar:**
```
My Assessments
Notifications
```

---

## 2. Login Screen

### Fields
| Field | Type | Validation |
|---|---|---|
| Username | Text Input | Required, max 100 chars |
| Password | Password Input | Required, min 8 chars |
| Login Button | Button | Submits form |

### Behaviour
- Show inline error on invalid credentials
- Show "Account locked" message if account is locked
- Redirect to role-appropriate landing page on success
- Store JWT in memory; set refresh token as HTTP-only cookie

---

## 3. Lookup Management Screen

### Screen: Lookup Type List
- Table columns: Type Code, Type Name, System Flag, Active Status, Actions
- Filter: Search by type code or name
- Actions: View values, Edit

### Screen: Lookup Value List (for a selected type)
- Table columns: Value Code, Value Label, Display Order, System Flag, Active, Actions
- Actions: Add, Edit, Toggle Active

### Form: Create / Edit Lookup Value
| Field | Type | Validation |
|---|---|---|
| Value Code | Text | Required, unique within type, uppercase |
| Value Label | Text | Required |
| Description | Textarea | Optional |
| Display Order | Number | Optional, default 0 |
| Is Active | Toggle | Default ON |

### Rules
- System values are read-only (no edit/deactivate)
- Deactivated values hidden from dropdowns but visible in history

---

## 4. Application Hierarchy Screens

### 4.1 Application Type List
- Table: Type Code, Type Name, Status, Active, Actions
- Actions: Create, Edit, Toggle Status

### 4.2 Bundle List
- Table: Bundle Code, Bundle Name, Status, Active, Actions
- Actions: Create, Edit, Toggle Status

### 4.3 Application Portfolio List
- Filters: Account, Application Type, Bundle
- Table: Portfolio Code, Portfolio Name, App Type, Bundle, Status, Actions

### 4.4 Application List
**Filter Bar (cascading dropdowns):**
- Dropdown 1: Application Type (WEB / HOST)
- Dropdown 2: Bundle (B06 / B12 / B20)
- Toggle: Include Deprecated

**Table Columns:**
| Column | Notes |
|---|---|
| Application Code | Unique within portfolio |
| Application Name | Full name |
| Portfolio | App Type + Bundle |
| Lifecycle Status | ACTIVE / DEPRECATED / FUTURE |
| Replacement | Shows replacement app if deprecated |
| Actions | Edit, Deprecate, View Requirements |

### 4.5 Create / Edit Application Form
| Field | Type | Validation |
|---|---|---|
| Application Type | Dropdown | Required |
| Bundle | Dropdown | Required, filtered by type |
| Application Code | Text | Required, unique in portfolio |
| Application Name | Text | Required |
| Lifecycle Status | Dropdown (lookup) | Required |
| Remarks | Textarea | Optional |
| Replacement Application | Dropdown | Only shown for DEPRECATED |

---

## 5. User Management Screen

### User List
- Search/filter: Name, Username, Email, Role, Status
- Table: Full Name, Username, Email, Employee ID, Primary Role, Team, Status, Actions
- Actions: Edit, Toggle Active, Assign Roles, Assign Teams

### Create / Edit User Form
| Field | Type | Validation |
|---|---|---|
| Full Name | Text | Required |
| Username | Text | Required, unique |
| Email | Email | Required, unique |
| Employee ID | Text | Optional |
| Manager | Autocomplete (users) | Optional |
| Primary Role | Dropdown (roles) | Required |
| Additional Roles | Multi-select | Optional |
| Team | Dropdown (teams) | Optional |
| Is Active | Toggle | Default ON |

---

## 6. Technician Assignment Screen

### Filter Bar (cascading)
- Application Type → Bundle → Application → Technician (optional)

### Assignment Table
| Column | Notes |
|---|---|
| Technician Name | User full name |
| Role on Application | Primary / Backup / SME / Trainee |
| Allocation % | 0–100 |
| Effective From | Date |
| Effective To | Date (optional) |
| Status | Active / Expired |
| Actions | Edit, Deactivate |

### Create / Edit Assignment Form
| Field | Type | Validation |
|---|---|---|
| Application Type | Dropdown | Required |
| Bundle | Dropdown | Required |
| Application | Dropdown | Required |
| Technician | Autocomplete | Required |
| Role on Application | Dropdown (lookup) | Required |
| Allocation % | Number (0–100) | Required |
| Effective From | Date Picker | Required |
| Effective To | Date Picker | Optional, must be after From |

---

## 7. Skill Category and Skill Management Screen

### Skill Category List
- Table: Category Code, Category Name, Display Order, Active, Actions
- Actions: Create, Edit, Toggle Active

### Skill List
- Filter: Category, Skill Type, Active only toggle
- Table: Skill Code, Skill Name, Category, Skill Type, Scope, Active, Actions

### Create / Edit Skill Form
| Field | Type | Validation |
|---|---|---|
| Skill Category | Dropdown | Required |
| Skill Code | Text | Required, unique |
| Skill Name | Text | Required |
| Description | Textarea | Optional |
| Skill Type | Dropdown (lookup: TECHNICAL/NON_TECHNICAL) | Required |
| Skill Scope | Dropdown (lookup: APPLICATION/GENERIC) | Optional |
| Is Active | Toggle | Default ON |

---

## 8. Requirement Version Screen

### Filter Bar
- Application Type → Bundle → Application

### Requirement Version List
- Table: Version Code, Version Name, Status (DRAFT/PUBLISHED/APPROVED/REJECTED), Skills Count, Created Date, Actions
- Actions: View/Edit (Draft only), Publish, Approve/Reject (Lead Manager only)

### Requirement Version Detail (skill grid)
| Column | Notes |
|---|---|
| Skill Category | Group header |
| Skill Name | Skill details |
| Expected Level | 1–5 (Admin/LM only; hidden from Technician) |
| Criticality | HIGH / MEDIUM / LOW |
| Min People | Minimum number of qualified staff |
| Mandatory | Yes / No |
| Actions | Edit row, Remove row |

### Approval Panel (Lead Manager)
- Decision: Approve / Reject
- Rejection Reason: Textarea (required if rejecting)
- Confirm button

---

## 9. Assessment Cycle Screen

### Filter Bar
- Application Type → Bundle → Application

### Cycle List
- Table: Cycle Name, Requirement Version, Start Date, End Date, Status (OPEN/CLOSED), Actions
- Actions: Create, Open, Close, View History

### Create Cycle Form
| Field | Type | Validation |
|---|---|---|
| Application | Pre-filled from filter | Required |
| Cycle Name | Text (e.g. FY26-Q2) | Required, unique per app |
| Requirement Version | Dropdown (Approved only) | Required |
| Start Date | Date Picker | Required |
| End Date | Date Picker | Required, after Start |

---

## 10. Technician Self Assessment Screen

### Context Panel (top)
- Shows: Technician name, Application, Cycle name, Cycle dates, Submission status

### Filter / Selector
- Application Dropdown: Shows only **assigned active** applications
- If open cycle exists: Load skill grid automatically

### Skill Assessment Grid
| Column | Visible To | Notes |
|---|---|---|
| Skill Category | Technician | Group rows |
| Skill Name | Technician | Skill description |
| Mandatory | Technician | Indicator (★) |
| Self Rating | Technician | Dropdown 1–5 (required if mandatory) |
| Evidence | Technician | Text field (required if rating ≥ 4) |
| Comment | Technician | Optional free text |
| Expected Level | **HIDDEN** | Never shown to technician |

### Action Buttons
| Button | Behaviour |
|---|---|
| Save Draft | Saves without validation, status stays DRAFT |
| Submit Final | Validates mandatory fields, sets status to SUBMITTED, locks grid |

### Status Indicator
- DRAFT / SUBMITTED / RETURNED / APPROVED — colour coded chip

---

## 11. Lead Manager Review Screen

### Filter Bar
- Application Type → Bundle → Application → Pending Technician dropdown

### Technician Context Panel
- Shows: Technician name, Application, Cycle, Submission date, Current status

### Review Grid
| Column | Visible To | Notes |
|---|---|---|
| Skill Category | Lead Manager | Group header |
| Skill Name | Lead Manager | — |
| Expected Level | Lead Manager | Visible to LM only |
| Self Rating | Lead Manager | Technician's submitted rating |
| Manager Rating | Lead Manager | Editable dropdown 1–5 |
| Final Rating | Lead Manager | Auto-computed or editable |
| Row Decision | Lead Manager | APPROVED / GAP_IDENTIFIED / CLARIFICATION_NEEDED |
| Manager Comment | Lead Manager | Free text per row |

### Bulk Actions Toolbar
- Bulk Approve Selected Rows
- Bulk Mark as Gap
- Bulk Request Clarification

### Overall Decision Panel
| Field | Type |
|---|---|
| Overall Decision Note | Textarea |
| Approve Ratings (button) | Triggers gap analysis |
| Request Clarification (button) | Returns assessment to technician |

---

## 12. Gap Analysis Screen

### Filter Bar
- Application Type → Bundle → Application → Cycle → Technician (optional)

### Gap Summary Cards
- Total Skills Assessed
- Skills with Gaps
- High Severity Gaps
- Medium Severity Gaps
- Low Severity Gaps

### Gap Detail Table
| Column | Notes |
|---|---|
| Technician | Name |
| Skill Category | — |
| Skill Name | — |
| Expected Level | From requirement |
| Final Rating | Manager approved rating |
| Gap | Expected - Final |
| Severity | HIGH / MEDIUM / LOW |
| Criticality | HIGH / MEDIUM / LOW |
| Training Status | Pending / Confirmed |

---

## 13. Training Recommendation Screen

### Filter Bar
- Application → Cycle → Severity → Priority → Status

### Training Plan Table
| Column | Notes |
|---|---|
| Technician | Name |
| Skill | Skill name |
| Application | App name |
| Training Type | From lookup |
| Priority | HIGH / MEDIUM / LOW |
| Target Date | Calculated from gap approval |
| Status | PENDING / CONFIRMED |
| Actions | Confirm plan |

---

## 14. Dashboard Screen

### Executive Summary Cards (top row)
| Card | Metric |
|---|---|
| Total Applications | Count of active applications |
| Applications Assessed | Count with approved assessment in cycle |
| Overall Readiness % | Average readiness across all apps |
| Open Gaps | Count of unresolved gaps |
| Pending Trainings | Open training recommendations |

### Filter Bar
- Application Type | Bundle | Application | Cycle

### Application Readiness Table
| Column | Notes |
|---|---|
| Application | Name + Type + Bundle |
| Skills Assessed | Count |
| Readiness % | % meeting expected level |
| High Risk Skills | Count of HIGH severity gaps |
| SME Coverage | % of required SME roles filled |
| Backup Coverage | % of required backup roles filled |

### Skill Heatmap
- Rows: Technicians
- Columns: Skills
- Cell: Rating value with colour (1=red, 2=orange, 3=yellow, 4=light green, 5=green)
- Filter: Skill Category

### Coverage Risk Panel
- Shows skills below minimum_people_required threshold
- Colour coded by criticality

---

## 15. Import / Export Screen

### Import Section
| Step | UI Element |
|---|---|
| 1. Select Template Type | Dropdown: Applications / Skill Requirements / Assignments |
| 2. Download Template | Button: Download Excel template |
| 3. Upload File | File picker, .xlsx only |
| 4. Validate & Import | Button: triggers backend validation + import |
| 5. Result Display | Success count, Failed rows with row number + error message |

### Import History Table
- Date, Template Type, File Name, Total Rows, Success, Failed, Status, Initiated By

### Export Section
| Export Type | Filters |
|---|---|
| Application Readiness Report | App Type, Bundle, Cycle |
| Skill Gap Report | App Type, Bundle, Cycle, Severity |
| Training Demand Report | App, Cycle, Priority |
| Assessment Status Report | App Type, Bundle, Cycle |
| Audit Log Report | Date range, Action, Entity Type |

---

## 16. Audit Log Screen

### Filter Bar
| Filter | Options |
|---|---|
| Date Range | From / To date pickers |
| User | Autocomplete search |
| Action | Dropdown: LOGIN, CREATE, UPDATE, APPROVE, etc. |
| Entity Type | Dropdown: APPLICATION, USER, ASSESSMENT, etc. |

### Audit Log Table
| Column | Notes |
|---|---|
| Timestamp | Date + Time |
| User | Actor username |
| Action | e.g. SUBMIT, APPROVE |
| Entity Type | e.g. ASSESSMENT |
| Entity ID | Public UUID |
| Summary | Brief change description |
| IP Address | Client IP |

### Expand Row
- Shows: Old Value JSON and New Value JSON side by side

### Export Button
- Export filtered results to Excel

---

## 17. Notifications

### Notification Bell (Header)
- Badge count shows unread notifications
- Click opens notification drawer (slide-in panel)

### Notification Drawer
| Element | Notes |
|---|---|
| Notification list | Most recent first, grouped by date |
| Each notification | Icon + title + message + timestamp |
| Mark as Read | Click notification or "Mark all read" button |
| Link | Clicking notification navigates to related screen |

### Notification Types and Icons
| Event | Icon | Message Example |
|---|---|---|
| Assessment submitted | 📋 | "John Smith submitted ATLAS-deZentral assessment" |
| Assessment returned | 🔄 | "Your ATLAS assessment has been returned for clarification" |
| Assessment approved | ✅ | "Your ATLAS assessment has been approved" |
| Requirement published | 📄 | "New requirement version ATLAS-FY26-Q2-v1 needs approval" |
| Training generated | 🎓 | "Training plan generated for 3 skill gaps" |
| Cycle opened | 🔔 | "Assessment cycle FY26-Q2 is now open for ATLAS" |

---

## 18. Validation Rules Summary

### Common Validations (all forms)
- Required fields show red asterisk
- Inline error messages below each field
- Submit button disabled until form is valid
- Unsaved changes prompt on navigation away

### Critical Business Validations
| Rule | Screen | Error Message |
|---|---|---|
| Mandatory skill must have rating before submit | Self Assessment | "Please rate all mandatory skills before submitting" |
| Evidence required for rating 4 or 5 | Self Assessment | "Evidence is required for ratings of 4 or 5" |
| Effective To must be after Effective From | Assignment | "End date must be after start date" |
| Only one open cycle per application | Assessment Cycle | "An open cycle already exists for this application" |
| Duplicate skill in requirement version | Req. Version | "This skill is already added to the requirement version" |
| Duplicate username | User Management | "Username already exists" |
| Duplicate email | User Management | "Email address already registered" |

---

## 19. Angular Route Structure

```
/login                                  ← Public
/dashboard                              ← Auth required
/master-data/lookup-types               ← Admin
/master-data/lookup-values/:typeCode    ← Admin
/master-data/application-types         ← Admin
/master-data/bundles                    ← Admin
/master-data/portfolios                 ← Admin
/master-data/applications               ← Admin
/users                                  ← Admin
/skills/categories                      ← Admin
/skills/list                            ← Admin
/assignments                            ← Admin / Lead Manager
/requirement-versions                   ← Admin / Lead Manager
/requirement-versions/:id               ← Admin / Lead Manager
/cycles                                 ← Admin
/assessments/my                         ← Technician
/reviews/pending                        ← Lead Manager
/reviews/:cycleId/:technicianId         ← Lead Manager
/gaps                                   ← Lead Manager / Admin
/training-plans                         ← Lead Manager / Admin
/reports/gap                            ← Admin / Lead Manager
/reports/training-demand                ← Admin / Lead Manager
/reports/assessment-status              ← Admin / Lead Manager
/import-export                          ← Admin
/audit-log                              ← Admin
/notifications                          ← All roles
```

---

*End of UI Screen Details Document*
