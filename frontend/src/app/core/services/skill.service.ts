import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../models/api-response.model';
import { PagedResponse } from '../models/application.model';
import {
  CreateSkillRequest,
  ImportHistory,
  SkillCategory,
  SkillImportResult,
  SkillStatusRequest,
  SkillSummary,
  SkillValidationResult,
  UpdateSkillRequest,
} from '../models/skill.model';

const API_BASE = 'http://localhost:8080/api/v1/admin';

@Injectable({ providedIn: 'root' })
export class SkillService {
  constructor(private http: HttpClient) {}

  // ----------------------------------------------------------------
  // Categories
  // ----------------------------------------------------------------

  getCategories(): Observable<SkillCategory[]> {
    return this.http
      .get<ApiResponse<SkillCategory[]>>(`${API_BASE}/skill-categories`)
      .pipe(map((r) => r.data ?? []));
  }

  // ----------------------------------------------------------------
  // Skill CRUD
  // ----------------------------------------------------------------

  getSkillsPaged(params: {
    page?: number;
    size?: number;
    search?: string;
    categoryCode?: string;
    scopeCode?: string;
    applicationTypeCode?: string;
    bundleCode?: string;
    applicationPublicId?: string;
    status?: string;
    sortBy?: string;
    sortDirection?: string;
  }): Observable<PagedResponse<SkillSummary>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.search)              httpParams = httpParams.set('search',              params.search);
    if (params.categoryCode)        httpParams = httpParams.set('categoryCode',        params.categoryCode);
    if (params.scopeCode)           httpParams = httpParams.set('scopeCode',           params.scopeCode);
    if (params.applicationTypeCode) httpParams = httpParams.set('applicationTypeCode', params.applicationTypeCode);
    if (params.bundleCode)          httpParams = httpParams.set('bundleCode',          params.bundleCode);
    if (params.applicationPublicId) httpParams = httpParams.set('applicationPublicId', params.applicationPublicId);
    if (params.status)              httpParams = httpParams.set('status',              params.status);
    if (params.sortBy)              httpParams = httpParams.set('sortBy',              params.sortBy);
    if (params.sortDirection)       httpParams = httpParams.set('sortDirection',       params.sortDirection);
    return this.http
      .get<ApiResponse<PagedResponse<SkillSummary>>>(`${API_BASE}/skills`, { params: httpParams })
      .pipe(map((r) => r.data!));
  }

  getSkillByPublicId(publicId: string): Observable<SkillSummary> {
    return this.http
      .get<ApiResponse<SkillSummary>>(`${API_BASE}/skills/${publicId}`)
      .pipe(map((r) => r.data!));
  }

  createSkill(request: CreateSkillRequest): Observable<SkillSummary> {
    return this.http
      .post<ApiResponse<SkillSummary>>(`${API_BASE}/skills`, request)
      .pipe(map((r) => r.data!));
  }

  updateSkill(publicId: string, request: UpdateSkillRequest): Observable<SkillSummary> {
    return this.http
      .patch<ApiResponse<SkillSummary>>(`${API_BASE}/skills/${publicId}`, request)
      .pipe(map((r) => r.data!));
  }

  updateSkillStatus(publicId: string, request: SkillStatusRequest): Observable<SkillSummary> {
    return this.http
      .patch<ApiResponse<SkillSummary>>(`${API_BASE}/skills/${publicId}/status`, request)
      .pipe(map((r) => r.data!));
  }

  // ----------------------------------------------------------------
  // Excel Import
  // ----------------------------------------------------------------

  downloadTemplate(): void {
    this.http.get(`${API_BASE}/import/skill-template`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'skill_import_template.xlsx';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        /* auth interceptor will redirect to login on 401 */
      },
    });
  }

  exportSkills(params?: {
    search?: string;
    categoryCode?: string;
    scopeCode?: string;
    applicationTypeCode?: string;
    bundleCode?: string;
    applicationPublicId?: string;
    status?: string;
  }): void {
    let httpParams = new HttpParams();
    if (params?.search)              httpParams = httpParams.set('search',              params.search);
    if (params?.categoryCode)        httpParams = httpParams.set('categoryCode',        params.categoryCode);
    if (params?.scopeCode)           httpParams = httpParams.set('scopeCode',           params.scopeCode);
    if (params?.applicationTypeCode) httpParams = httpParams.set('applicationTypeCode', params.applicationTypeCode);
    if (params?.bundleCode)          httpParams = httpParams.set('bundleCode',          params.bundleCode);
    if (params?.applicationPublicId) httpParams = httpParams.set('applicationPublicId', params.applicationPublicId);
    if (params?.status)              httpParams = httpParams.set('status',              params.status);

    this.http
      .get(`${API_BASE}/skills/export`, { responseType: 'blob', params: httpParams })
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'skills_export.xlsx';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
        },
        error: () => {
          /* auth interceptor will redirect to login on 401 */
        },
      });
  }

  validateImport(file: File): Observable<SkillValidationResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<SkillValidationResult>>(`${API_BASE}/import/skills/validate`, formData)
      .pipe(map((r) => r.data!));
  }

  confirmImport(file: File): Observable<SkillImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<SkillImportResult>>(`${API_BASE}/import/skills/confirm`, formData)
      .pipe(map((r) => r.data!));
  }

  // ----------------------------------------------------------------
  // Import History
  // ----------------------------------------------------------------

  getImportHistoryPaged(params: {
    page?: number;
    size?: number;
    templateType?: string;
    statusCode?: string;
    uploadedBy?: string;
    fromDate?: string;
    toDate?: string;
  }): Observable<PagedResponse<ImportHistory>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.templateType) httpParams = httpParams.set('templateType', params.templateType);
    if (params.statusCode)   httpParams = httpParams.set('statusCode',   params.statusCode);
    if (params.uploadedBy)   httpParams = httpParams.set('uploadedBy',   params.uploadedBy);
    if (params.fromDate)     httpParams = httpParams.set('fromDate',     params.fromDate);
    if (params.toDate)       httpParams = httpParams.set('toDate',       params.toDate);
    return this.http
      .get<ApiResponse<PagedResponse<ImportHistory>>>(`${API_BASE}/import/history`, { params: httpParams })
      .pipe(map((r) => r.data!));
  }
}
