import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../models/api-response.model';
import { PagedResponse } from '../models/application.model';
import { ImportHistory } from '../models/skill.model';
import {
  CreateRequirementVersionRequest,
  CreateSkillRequirementRequest,
  RequirementImportResult,
  RequirementStatusRequest,
  RequirementValidationResult,
  RequirementVersion,
  SkillRequirement,
  UpdateSkillRequirementRequest,
} from '../models/requirement.model';

const API_BASE = 'http://localhost:9090/api/v1/admin';

@Injectable({ providedIn: 'root' })
export class RequirementService {
  constructor(private http: HttpClient) {}

  // ----------------------------------------------------------------
  // Requirement Versions
  // ----------------------------------------------------------------

  getVersionsForApplication(applicationPublicId: string, eligibleOnly = false): Observable<RequirementVersion[]> {
    let params = new HttpParams().set('applicationPublicId', applicationPublicId);
    if (eligibleOnly) params = params.set('eligibleOnly', 'true');
    return this.http
      .get<ApiResponse<RequirementVersion[]>>(`${API_BASE}/requirement-versions`, { params })
      .pipe(map((r) => r.data ?? []));
  }

  createVersion(request: CreateRequirementVersionRequest): Observable<RequirementVersion> {
    return this.http
      .post<ApiResponse<RequirementVersion>>(`${API_BASE}/requirement-versions`, request)
      .pipe(map((r) => r.data!));
  }

  // ----------------------------------------------------------------
  // Skill Requirements
  // ----------------------------------------------------------------

  getRequirementsPaged(params: {
    page?: number;
    size?: number;
    requirementVersionPublicId?: string;
    applicationPublicId?: string;
    search?: string;
    skillGroup?: string;
    mandatory?: string;
    status?: string;
    sortBy?: string;
    sortDirection?: string;
  }): Observable<PagedResponse<SkillRequirement>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.requirementVersionPublicId)
      httpParams = httpParams.set('requirementVersionPublicId', params.requirementVersionPublicId);
    if (params.applicationPublicId)
      httpParams = httpParams.set('applicationPublicId', params.applicationPublicId);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.skillGroup) httpParams = httpParams.set('skillGroup', params.skillGroup);
    if (params.mandatory) httpParams = httpParams.set('mandatory', params.mandatory);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);
    if (params.sortDirection) httpParams = httpParams.set('sortDirection', params.sortDirection);
    return this.http
      .get<ApiResponse<PagedResponse<SkillRequirement>>>(`${API_BASE}/skill-requirements`, { params: httpParams })
      .pipe(map((r) => r.data!));
  }

  createRequirement(request: CreateSkillRequirementRequest): Observable<SkillRequirement> {
    return this.http
      .post<ApiResponse<SkillRequirement>>(`${API_BASE}/skill-requirements`, request)
      .pipe(map((r) => r.data!));
  }

  updateRequirement(publicId: string, request: UpdateSkillRequirementRequest): Observable<SkillRequirement> {
    return this.http
      .patch<ApiResponse<SkillRequirement>>(`${API_BASE}/skill-requirements/${publicId}`, request)
      .pipe(map((r) => r.data!));
  }

  updateStatus(publicId: string, request: RequirementStatusRequest): Observable<SkillRequirement> {
    return this.http
      .patch<ApiResponse<SkillRequirement>>(`${API_BASE}/skill-requirements/${publicId}/status`, request)
      .pipe(map((r) => r.data!));
  }

  // ----------------------------------------------------------------
  // Excel Import
  // ----------------------------------------------------------------

  downloadTemplate(): void {
    this.http.get(`${API_BASE}/skill-requirements/template`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = '04_APPLICATION_SKILL_REQUIREMENT_MAPPING.xlsx';
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

  validateImport(file: File): Observable<RequirementValidationResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<RequirementValidationResult>>(`${API_BASE}/skill-requirements/import/validate`, formData)
      .pipe(map((r) => r.data!));
  }

  confirmImport(file: File): Observable<RequirementImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<RequirementImportResult>>(`${API_BASE}/skill-requirements/import/confirm`, formData)
      .pipe(map((r) => r.data!));
  }

  // ----------------------------------------------------------------
  // Import History
  // ----------------------------------------------------------------

  getImportHistoryPaged(params: {
    page?: number;
    size?: number;
    statusCode?: string;
    uploadedBy?: string;
    fromDate?: string;
    toDate?: string;
  }): Observable<PagedResponse<ImportHistory>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.statusCode) httpParams = httpParams.set('statusCode', params.statusCode);
    if (params.uploadedBy) httpParams = httpParams.set('uploadedBy', params.uploadedBy);
    if (params.fromDate)   httpParams = httpParams.set('fromDate', params.fromDate);
    if (params.toDate)     httpParams = httpParams.set('toDate', params.toDate);
    return this.http
      .get<ApiResponse<PagedResponse<ImportHistory>>>(`${API_BASE}/skill-requirements/import/history`, { params: httpParams })
      .pipe(map((r) => r.data!));
  }
}
