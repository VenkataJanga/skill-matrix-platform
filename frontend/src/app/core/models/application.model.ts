// ============================================================
// Milestone 2 — Application Hierarchy & Assignment Models
// ============================================================

export interface ApplicationType {
  publicId: string;
  typeCode: string;
  typeName: string;
  description: string | null;
}

export interface Bundle {
  publicId: string;
  bundleCode: string;
  bundleName: string;
  description: string | null;
}

export interface Application {
  publicId: string;
  applicationCode: string;
  applicationName: string;
  description: string | null;
  portfolioCode: string;
  portfolioName: string;
  typeCode: string;
  typeName: string;
  bundleCode: string;
  bundleName: string;
}

export interface UserSummary {
  publicId: string;
  username: string;
  fullName: string;
  email: string;
  active: boolean;
  roles: string[];
}

export interface ApplicationAssignment {
  publicId: string;
  userPublicId: string;
  username: string;
  fullName: string;
  applicationPublicId: string;
  applicationCode: string;
  applicationName: string;
  typeCode: string;
  bundleCode: string;
  allocationPercentage: number;
  effectiveFrom: string;   // ISO date string
  effectiveTo: string | null;
  active: boolean;
}

export interface AssignmentRequest {
  userPublicId: string;
  applicationPublicId: string;
  allocationPercentage: number;
  effectiveFrom: string;   // ISO date string
  effectiveTo: string | null;
}

// ============================================================
// M2.1 — User Management models
// ============================================================

export interface UserDetail {
  publicId: string;
  username: string;
  fullName: string;
  email: string;
  employeeId: string | null;
  primaryRoleCode: string | null;
  managerPublicId: string | null;
  managerUsername: string | null;
  active: boolean;
  mustChangePassword: boolean;
  accountLocked: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface CreateUserRequest {
  username: string;
  fullName: string;
  email: string;
  roleCode: string;
  employeeId?: string;
  managerPublicId?: string;
  mustChangePassword: boolean;
}

export interface UpdateUserRequest {
  fullName: string;
  email: string;
  roleCode: string;
  employeeId?: string;
  managerPublicId?: string;
  mustChangePassword: boolean;
}

export interface UserStatusRequest {
  active: boolean;
  reason?: string;
}

// ============================================================
// M2.1 — Pagination
// ============================================================

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// ============================================================
// M2.1 — Assignment Management models
// ============================================================

export interface AssignmentStatusRequest {
  active: boolean;
  reason?: string;
}

export interface BulkStatusRequest {
  assignmentPublicIds: string[];
  active: boolean;
  reason?: string;
}

export interface BulkStatusResponse {
  requestedCount: number;
  successCount: number;
  failedCount: number;
  failures: Array<{ publicId: string; reason: string }>;
}

export interface DuplicateCheckResponse {
  isDuplicate: boolean;
  existingAssignmentPublicId: string | null;
  message: string;
}
