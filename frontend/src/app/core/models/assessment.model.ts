// M4 — Assessment Cycle Management & Technician Self-Assessment models.

// ---------------------------------------------------------------
// Admin — Assessment Cycle
// ---------------------------------------------------------------

export interface AssessmentCycle {
  publicId: string;
  applicationPublicId: string;
  applicationCode: string;
  applicationName: string;
  requirementVersionPublicId: string;
  requirementVersionCode: string;
  cycleName: string;
  periodTypeCode: string | null;
  periodValue: string | null;
  periodYear: number | null;
  cycleStartDate: string;
  cycleEndDate: string;
  statusCode: string | null;
  statusLabel: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface CreateAssessmentCycleRequest {
  applicationPublicId: string;
  requirementVersionPublicId: string;
  cycleName: string;
  periodTypeCode?: string | null;
  periodValue?: string | null;
  periodYear?: number | null;
  cycleStartDate: string;
  cycleEndDate: string;
}

export interface UpdateAssessmentCycleRequest {
  cycleName: string;
  periodTypeCode?: string | null;
  periodValue?: string | null;
  periodYear?: number | null;
  cycleStartDate: string;
  cycleEndDate: string;
  applicationPublicId?: string | null;
  requirementVersionPublicId?: string | null;
}

/** Controlled CLOSED -> OPEN transition request (M4 stabilization). */
export interface ReopenAssessmentCycleRequest {
  reason: string;
  newEndDate?: string | null;
}

// ---------------------------------------------------------------
// Technician — Self Assessment
// CRITICAL: These models intentionally have NO expectedLevel /
// expectedRating / targetLevel field. Do not add one.
// ---------------------------------------------------------------

export interface TechnicianAssessmentSummary {
  assessmentPublicId: string | null;
  cyclePublicId: string;
  cycleName: string;
  applicationCode: string;
  applicationName: string;
  cycleStartDate: string;
  cycleEndDate: string;
  assessmentStatus: 'NOT_STARTED' | 'DRAFT' | 'SUBMITTED' | string;
  totalItems: number;
  ratedItems: number;
  updatedAt: string | null;
}

export interface TechnicianAssessmentItem {
  assessmentItemPublicId: string;
  skillPublicId: string;
  skillCode: string;
  skillName: string;
  categoryCode: string | null;
  categoryName: string | null;
  skillGroup: string | null;
  mandatory: boolean;
  selfRating: number | null;
  comment: string | null;
  evidence: string | null;
}

export interface TechnicianAssessment {
  assessmentPublicId: string;
  cycleName: string;
  applicationCode: string;
  applicationName: string;
  cycleStartDate: string;
  cycleEndDate: string;
  statusCode: string | null;
  statusLabel: string | null;
  submittedAt: string | null;
  items: TechnicianAssessmentItem[];
  /** Client-side optimistic-lock token, mirrors header.version from backend. */
  version?: number;
}

export interface ItemRatingRequest {
  assessmentItemPublicId: string;
  selfRating: number | null;
  comment: string | null;
  evidence: string | null;
}

export interface SaveDraftRequest {
  version: number;
  items: ItemRatingRequest[];
}
