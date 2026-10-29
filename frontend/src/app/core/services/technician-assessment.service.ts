import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../models/api-response.model';
import {
  SaveDraftRequest,
  TechnicianAssessment,
  TechnicianAssessmentSummary,
} from '../models/assessment.model';

const API_BASE = 'http://localhost:9090/api/v1/technician';

/**
 * M4B — Technician Self-Assessment.
 * Technician identity is always derived server-side from the JWT —
 * this service never sends a technician id in any request.
 */
@Injectable({ providedIn: 'root' })
export class TechnicianAssessmentService {
  constructor(private http: HttpClient) {}

  getMyAssessments(): Observable<TechnicianAssessmentSummary[]> {
    return this.http
      .get<ApiResponse<TechnicianAssessmentSummary[]>>(`${API_BASE}/assessments`)
      .pipe(map((r) => r.data ?? []));
  }

  getAssessment(assessmentPublicId: string): Observable<TechnicianAssessment> {
    return this.http
      .get<ApiResponse<TechnicianAssessment>>(`${API_BASE}/assessments/${assessmentPublicId}`)
      .pipe(map((r) => r.data!));
  }

  startAssessment(cyclePublicId: string): Observable<TechnicianAssessment> {
    return this.http
      .post<ApiResponse<TechnicianAssessment>>(`${API_BASE}/assessment-cycles/${cyclePublicId}/start`, {})
      .pipe(map((r) => r.data!));
  }

  saveDraft(assessmentPublicId: string, request: SaveDraftRequest): Observable<TechnicianAssessment> {
    return this.http
      .patch<ApiResponse<TechnicianAssessment>>(`${API_BASE}/assessments/${assessmentPublicId}/draft`, request)
      .pipe(map((r) => r.data!));
  }

  submit(assessmentPublicId: string): Observable<TechnicianAssessment> {
    return this.http
      .post<ApiResponse<TechnicianAssessment>>(`${API_BASE}/assessments/${assessmentPublicId}/submit`, {})
      .pipe(map((r) => r.data!));
  }
}
