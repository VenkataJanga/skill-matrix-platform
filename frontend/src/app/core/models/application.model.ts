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
  roleOnAppCode: string | null;
  primary: boolean;
}

export interface AssignmentRequest {
  userPublicId: string;
  applicationPublicId: string;
  allocationPercentage: number;
  effectiveFrom: string;   // ISO date string
  effectiveTo: string | null;
}

// ============================================================
// M4 — Multi-Technician Assignment models (Application Master
// & Multi-Assignment stabilization enhancement)
// ============================================================

export interface BulkAssignmentEntry {
  userPublicId: string;
  allocationPercentage: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  roleOnAppCode?: string | null;
}

export interface BulkAssignmentRequest {
  applicationPublicId: string;
  assignments: BulkAssignmentEntry[];
}

export interface BulkAssignmentRowResult {
  userPublicId: string;
  username: string;
  action: 'CREATED' | 'UPDATED' | 'SKIPPED' | 'FAILED';
  reason: string | null;
  assignmentPublicId: string | null;
}

export interface BulkAssignmentResult {
  requested: number;
  created: number;
  updated: number;
  skipped: number;
  failed: number;
  rows: BulkAssignmentRowResult[];
}

export interface AssignmentImportRowPreview {
  rowNumber: number;
  username: string | null;
  applicationCode: string | null;
  action: 'INSERT' | 'UPDATE' | 'SKIP' | 'ERROR';
  reason: string | null;
  errors: string[];
}

export interface AssignmentValidationResult {
  valid: boolean;
  totalRows: number;
  insertRows: number;
  updateRows: number;
  skipRows: number;
  errorRows: number;
  rows: AssignmentImportRowPreview[];
}

export interface AssignmentImportResult {
  success: boolean;
  totalRows: number;
  insertedRows: number;
  updatedRows: number;
  skippedRows: number;
  errorRows: number;
  importHistoryPublicId: string | null;
  message: string;
  status: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
}

// ============================================================
// M4 — Application Master models
// ============================================================

export interface ApplicationMaster {
  publicId: string;
  applicationCode: string;
  applicationName: string;
  description: string | null;
  remarks: string | null;
  portfolioCode: string;
  portfolioName: string;
  typeCode: string;
  typeName: string;
  bundleCode: string;
  bundleName: string;
  lifecycleStatusCode: string | null;
  lifecycleStatusLabel: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface CreateApplicationRequest {
  applicationTypeCode: string;
  bundleCode: string;
  portfolioCode: string;
  applicationCode: string;
  applicationName: string;
  lifecycleStatusCode?: string | null;
  description?: string | null;
  remarks?: string | null;
}

export interface UpdateApplicationRequest {
  applicationTypeCode?: string | null;
  bundleCode?: string | null;
  portfolioCode?: string | null;
  applicationName: string;
  lifecycleStatusCode?: string | null;
  description?: string | null;
  remarks?: string | null;
}

export interface ApplicationStatusRequest {
  active: boolean;
  reason?: string;
}

export interface DeleteApplicationResult {
  applicationPublicId: string;
  applicationCode: string;
  status: 'DELETED' | 'BLOCKED';
  reason: string | null;
}

export interface ApplicationImportRowPreview {
  rowNumber: number;
  applicationCode: string | null;
  applicationName: string | null;
  typeCode: string | null;
  bundleCode: string | null;
  portfolioCode: string | null;
  action: 'INSERT' | 'UPDATE' | 'SKIP' | 'ERROR';
  reason: string | null;
  errors: string[];
}

export interface ApplicationValidationResult {
  valid: boolean;
  totalRows: number;
  insertRows: number;
  updateRows: number;
  skipRows: number;
  errorRows: number;
  rows: ApplicationImportRowPreview[];
}

export interface ApplicationImportResult {
  success: boolean;
  totalRows: number;
  insertedRows: number;
  updatedRows: number;
  skippedRows: number;
  errorRows: number;
  importHistoryPublicId: string | null;
  message: string;
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

export interface AdminPasswordResetResult {
  userPublicId: string;
  username: string;
  message: string;
  devResetLink?: string;
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

// ============================================================
// M2.2 — User Import / Export models
// ============================================================

export interface UserImportRowPreview {
  rowNumber: number;
  username: string | null;
  fullName: string | null;
  email: string | null;
  roleCode: string | null;
  action: 'INSERT' | 'UPDATE' | 'SKIP' | 'ERROR';
  reason: string | null;
  errors: string[];
}

export interface UserValidationResult {
  valid: boolean;
  totalRows: number;
  insertRows: number;
  updateRows: number;
  skipRows: number;
  errorRows: number;
  rows: UserImportRowPreview[];
}

export interface UserImportResult {
  success: boolean;
  totalRows: number;
  insertedRows: number;
  updatedRows: number;
  skippedRows: number;
  errorRows: number;
  importHistoryPublicId: string | null;
  message: string;
  importMode: 'STRICT' | 'PARTIAL';
  partialSuccess: boolean;
}

// ============================================================
// M2.3 — User Delete models
// ============================================================

export interface DeleteUserResult {
  userPublicId: string;
  username: string;
  status: 'DELETED' | 'BLOCKED' | 'FAILED';
  reason: string | null;
}

export interface BulkDeleteRequest {
  userPublicIds: string[];
  reason?: string;
}

export interface BulkDeleteResponse {
  requestedCount: number;
  deletedCount: number;
  blockedCount: number;
  failedCount: number;
  /** Only non-DELETED entries; successful deletes are counted in deletedCount only. */
  results: DeleteUserResult[];
}

export interface UserImportHistoryEntry {
  publicId: string;
  fileName: string;
  templateType: string;
  operationType: string;
  totalRows: number;
  insertedRows: number;
  updatedRows: number;
  skippedRows: number;
  failedRows: number;
  statusCode: string;
  statusLabel: string;
  uploadedBy: string;
  startedAt: string;
  completedAt: string | null;
}
