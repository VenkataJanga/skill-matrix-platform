// ============================================================
// M3 — Skill Master & Import Models
// ============================================================

export interface SkillCategory {
  publicId: string;
  categoryCode: string;
  categoryName: string;
  description: string | null;
  displayOrder: number;
}

export interface SkillSummary {
  publicId: string;
  skillCode: string;
  skillName: string;
  description: string | null;
  categoryCode: string;
  categoryName: string;
  skillTypeCode: string | null;
  skillTypeLabel: string | null;
  scopeCode: string | null;
  scopeLabel: string | null;
  applicationTypeCode: string | null;
  applicationTypePublicId: string | null;
  bundleCode: string | null;
  bundlePublicId: string | null;
  applicationCode: string | null;
  applicationPublicId: string | null;
  applicationName: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface CreateSkillRequest {
  skillCode: string;
  skillName: string;
  description?: string;
  categoryCode: string;
  skillTypeCode?: string;
  scopeCode: string;
  applicationTypeCode?: string;
  bundleCode?: string;
  applicationPublicId?: string;
}

export interface UpdateSkillRequest {
  skillName: string;
  description?: string;
  categoryCode: string;
  skillTypeCode?: string;
  scopeCode: string;
  applicationTypeCode?: string;
  bundleCode?: string;
  applicationPublicId?: string;
}

export interface SkillStatusRequest {
  active: boolean;
  reason?: string;
}

// ============================================================
// Excel Import — SMART_UPSERT
// ============================================================

/** Row-level preview returned by the validate endpoint. */
export interface SkillImportRowPreview {
  sheetName: string;
  rowNumber: number;
  skillCode: string | null;
  skillName: string | null;
  /** INSERT | UPDATE | SKIP | ERROR */
  action: 'INSERT' | 'UPDATE' | 'SKIP' | 'ERROR';
  reason: string;
  fieldErrors: string[];
}

/** Full validation result with row previews and summary counts. */
export interface SkillValidationResult {
  valid: boolean;      // true when errorRows === 0
  totalRows: number;
  insertRows: number;
  updateRows: number;
  skipRows: number;
  errorRows: number;
  rows: SkillImportRowPreview[];
}

/** Result of the confirm-import endpoint. */
export interface SkillImportResult {
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
// Import History
// ============================================================

export interface ImportHistory {
  publicId: string;
  operationType: string;
  templateType: string;
  fileName: string | null;
  totalRows: number | null;
  successRows: number | null;
  failedRows: number | null;
  statusCode: string | null;
  statusLabel: string | null;
  initiatedByUsername: string;
  initiatedByFullName: string;
  startedAt: string;
  completedAt: string | null;
}
