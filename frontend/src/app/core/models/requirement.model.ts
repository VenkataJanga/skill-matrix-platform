// ============================================================
// M3.1 — Application Skill Requirement Mapping Models
// ============================================================

export interface RequirementVersion {
  publicId: string;
  applicationPublicId: string;
  applicationCode: string;
  applicationName: string;
  versionCode: string;
  versionName: string;
  description: string | null;
  statusCode: string | null;
  statusLabel: string | null;
  publishedAt: string | null;
  approvedAt: string | null;
  approvedBy: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
  /**
   * Count of ACTIVE skill requirements mapped to this version. Used by the
   * Assessment Cycle "Requirement Version" dropdown to warn Admin BEFORE cycle
   * creation when a version has zero active requirements. The backend Open-time
   * safeguard in AssessmentCycleService.openCycle() remains the final authoritative
   * check and is unaffected by this field.
   */
  activeRequirementCount: number;
}

export interface CreateRequirementVersionRequest {
  applicationPublicId: string;
  versionCode: string;
  versionName: string;
  description?: string;
}

export interface SkillRequirement {
  publicId: string;
  requirementVersionPublicId: string;
  versionCode: string;
  applicationPublicId: string;
  applicationCode: string;
  applicationName: string;
  skillPublicId: string;
  skillCode: string;
  skillName: string;
  skillGroup: string | null;
  categoryCode: string | null;
  categoryName: string | null;
  expectedLevel: number;
  mandatory: boolean;
  criticalityCode: string | null;
  criticalityLabel: string | null;
  roleCode: string | null;
  roleLabel: string | null;
  gradeCode: string | null;
  gradeLabel: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface CreateSkillRequirementRequest {
  requirementVersionPublicId: string;
  skillPublicId: string;
  expectedLevel: number;
  mandatory: boolean;
  criticalityCode?: string;
  roleCode?: string;
  gradeCode?: string;
}

export interface UpdateSkillRequirementRequest {
  expectedLevel: number;
  mandatory: boolean;
  criticalityCode?: string;
  roleCode?: string;
  gradeCode?: string;
}

export interface RequirementStatusRequest {
  active: boolean;
  reason?: string;
}

// ============================================================
// Excel Import — SMART_UPSERT
// ============================================================

export interface RequirementImportRowPreview {
  sheetName: string;
  rowNumber: number;
  applicationCode: string | null;
  skillCode: string | null;
  skillName: string | null;
  expectedLevel: number | null;
  mandatoryFlag: string | null;
  action: 'INSERT' | 'UPDATE' | 'SKIP' | 'ERROR';
  reason: string;
  fieldErrors: string[];
}

export interface RequirementValidationResult {
  valid: boolean;
  totalRows: number;
  insertRows: number;
  updateRows: number;
  skipRows: number;
  errorRows: number;
  rows: RequirementImportRowPreview[];
}

export interface RequirementImportResult {
  success: boolean;
  totalRows: number;
  insertedRows: number;
  updatedRows: number;
  skippedRows: number;
  errorRows: number;
  importHistoryPublicId: string | null;
  message: string;
}
