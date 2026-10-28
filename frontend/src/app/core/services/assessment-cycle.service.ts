import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../models/api-response.model';
import { PagedResponse } from '../models/application.model';
import {
  AssessmentCycle,
  CreateAssessmentCycleRequest,
  ReopenAssessmentCycleRequest,
  UpdateAssessmentCycleRequest,
} from '../models/assessment.model';

const API_BASE = 'http://localhost:8080/api/v1/admin/assessment-cycles';

/** M4A — Admin Assessment Cycle Management. */
@Injectable({ providedIn: 'root' })
export class AssessmentCycleService {
  constructor(private http: HttpClient) {}

  getCycles(params: {
    page?: number;
    size?: number;
    search?: string;
    applicationPublicId?: string;
    statusCode?: string;
    sortBy?: string;
    sortDirection?: string;
  }): Observable<PagedResponse<AssessmentCycle>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.applicationPublicId) httpParams = httpParams.set('applicationPublicId', params.applicationPublicId);
    if (params.statusCode) httpParams = httpParams.set('statusCode', params.statusCode);
    if (params.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);
    if (params.sortDirection) httpParams = httpParams.set('sortDirection', params.sortDirection);
    return this.http
      .get<ApiResponse<PagedResponse<AssessmentCycle>>>(API_BASE, { params: httpParams })
      .pipe(map((r) => r.data!));
  }

  getByPublicId(publicId: string): Observable<AssessmentCycle> {
    return this.http.get<ApiResponse<AssessmentCycle>>(`${API_BASE}/${publicId}`).pipe(map((r) => r.data!));
  }

  createCycle(request: CreateAssessmentCycleRequest): Observable<AssessmentCycle> {
    return this.http.post<ApiResponse<AssessmentCycle>>(API_BASE, request).pipe(map((r) => r.data!));
  }

  updateCycle(publicId: string, request: UpdateAssessmentCycleRequest): Observable<AssessmentCycle> {
    return this.http
      .put<ApiResponse<AssessmentCycle>>(`${API_BASE}/${publicId}`, request)
      .pipe(map((r) => r.data!));
  }

  openCycle(publicId: string): Observable<AssessmentCycle> {
    return this.http
      .patch<ApiResponse<AssessmentCycle>>(`${API_BASE}/${publicId}/open`, {})
      .pipe(map((r) => r.data!));
  }

  closeCycle(publicId: string): Observable<AssessmentCycle> {
    return this.http
      .patch<ApiResponse<AssessmentCycle>>(`${API_BASE}/${publicId}/close`, {})
      .pipe(map((r) => r.data!));
  }

  /** Controlled CLOSED -> OPEN transition. Only reopens the assessment period —
   *  never touches existing technician assessment data. */
  reopenCycle(publicId: string, request: ReopenAssessmentCycleRequest): Observable<AssessmentCycle> {
    return this.http
      .patch<ApiResponse<AssessmentCycle>>(`${API_BASE}/${publicId}/reopen`, request)
      .pipe(map((r) => r.data!));
  }
}
