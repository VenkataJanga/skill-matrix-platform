import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../models/api-response.model';
import {
  AdminPasswordResetResult,
  Application,
  ApplicationAssignment,
  ApplicationImportResult,
  ApplicationMaster,
  ApplicationType,
  ApplicationValidationResult,
  AssignmentImportResult,
  AssignmentRequest,
  AssignmentStatusRequest,
  AssignmentValidationResult,
  BulkAssignmentRequest,
  BulkAssignmentResult,
  BulkDeleteRequest,
  BulkDeleteResponse,
  BulkStatusRequest,
  BulkStatusResponse,
  Bundle,
  CreateApplicationRequest,
  CreateUserRequest,
  DeleteApplicationResult,
  DeleteUserResult,
  DuplicateCheckResponse,
  PagedResponse,
  ApplicationStatusRequest,
  UpdateApplicationRequest,
  UpdateUserRequest,
  UserDetail,
  UserImportResult,
  UserImportHistoryEntry,
  UserStatusRequest,
  UserSummary,
  UserValidationResult,
} from '../models/application.model';

const API_BASE = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  constructor(private http: HttpClient) {}

  // ----------------------------------------------------------------
  // Application Hierarchy (all authenticated users)
  // ----------------------------------------------------------------

  getApplicationTypes(): Observable<ApplicationType[]> {
    return this.http
      .get<ApiResponse<ApplicationType[]>>(`${API_BASE}/application-types`)
      .pipe(map((r) => r.data ?? []));
  }

  getBundles(): Observable<Bundle[]> {
    return this.http
      .get<ApiResponse<Bundle[]>>(`${API_BASE}/bundles`)
      .pipe(map((r) => r.data ?? []));
  }

  getApplications(typeCode?: string, bundleCode?: string): Observable<Application[]> {
    let params = new HttpParams();
    if (typeCode) params = params.set('typeCode', typeCode);
    if (bundleCode) params = params.set('bundleCode', bundleCode);
    return this.http
      .get<ApiResponse<Application[]>>(`${API_BASE}/applications`, { params })
      .pipe(map((r) => r.data ?? []));
  }

  // ----------------------------------------------------------------
  // Admin — Users (paginated M2.1)
  // ----------------------------------------------------------------

  getUsersPaged(params: {
    page?: number;
    size?: number;
    search?: string;
    roleCode?: string;
    status?: string;
    sortBy?: string;
    sortDirection?: string;
  }): Observable<PagedResponse<UserSummary>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.search)        httpParams = httpParams.set('search',        params.search);
    if (params.roleCode)      httpParams = httpParams.set('roleCode',      params.roleCode);
    if (params.status)        httpParams = httpParams.set('status',        params.status);
    if (params.sortBy)        httpParams = httpParams.set('sortBy',        params.sortBy);
    if (params.sortDirection) httpParams = httpParams.set('sortDirection', params.sortDirection);
    return this.http
      .get<ApiResponse<PagedResponse<UserSummary>>>(`${API_BASE}/admin/users`, { params: httpParams })
      .pipe(map((r) => r.data!));
  }

  getUserByPublicId(publicId: string): Observable<UserDetail> {
    return this.http
      .get<ApiResponse<UserDetail>>(`${API_BASE}/admin/users/${publicId}`)
      .pipe(map((r) => r.data!));
  }

  createUser(request: CreateUserRequest): Observable<UserDetail> {
    return this.http
      .post<ApiResponse<UserDetail>>(`${API_BASE}/admin/users`, request)
      .pipe(map((r) => r.data!));
  }

  updateUser(publicId: string, request: UpdateUserRequest): Observable<UserDetail> {
    return this.http
      .put<ApiResponse<UserDetail>>(`${API_BASE}/admin/users/${publicId}`, request)
      .pipe(map((r) => r.data!));
  }

  updateUserStatus(publicId: string, request: UserStatusRequest): Observable<UserDetail> {
    return this.http
      .patch<ApiResponse<UserDetail>>(`${API_BASE}/admin/users/${publicId}/status`, request)
      .pipe(map((r) => r.data!));
  }

  adminResetPassword(publicId: string): Observable<AdminPasswordResetResult> {
    return this.http
      .post<ApiResponse<AdminPasswordResetResult>>(`${API_BASE}/admin/users/${publicId}/password-reset`, {})
      .pipe(map((r) => r.data!));
  }

  /** Legacy flat list — still used by technician-assign dropdown */
  getAllUsers(): Observable<UserSummary[]> {
    return this.getUsersPaged({ size: 500 }).pipe(map((r) => r.content));
  }

  getTechnicians(): Observable<UserSummary[]> {
    return this.getUsersPaged({ roleCode: 'TECHNICIAN', size: 500, status: 'ACTIVE' })
      .pipe(map((r) => r.content));
  }

  // ----------------------------------------------------------------
  // Admin — Assignments (paginated M2.1)
  // ----------------------------------------------------------------

  getAssignmentsPaged(params: {
    page?: number;
    size?: number;
    search?: string;
    applicationPublicId?: string;
    technicianPublicId?: string;
    typeCode?: string;
    bundleCode?: string;
    status?: string;
    sortBy?: string;
    sortDirection?: string;
  }): Observable<PagedResponse<ApplicationAssignment>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.search)               httpParams = httpParams.set('search',               params.search);
    if (params.applicationPublicId)  httpParams = httpParams.set('applicationPublicId',  params.applicationPublicId);
    if (params.technicianPublicId)   httpParams = httpParams.set('technicianPublicId',   params.technicianPublicId);
    if (params.typeCode)             httpParams = httpParams.set('typeCode',             params.typeCode);
    if (params.bundleCode)           httpParams = httpParams.set('bundleCode',           params.bundleCode);
    if (params.status)               httpParams = httpParams.set('status',               params.status);
    if (params.sortBy)               httpParams = httpParams.set('sortBy',               params.sortBy);
    if (params.sortDirection)        httpParams = httpParams.set('sortDirection',        params.sortDirection);
    return this.http
      .get<ApiResponse<PagedResponse<ApplicationAssignment>>>(
        `${API_BASE}/admin/application-assignments`, { params: httpParams })
      .pipe(map((r) => r.data!));
  }

  checkDuplicate(userPublicId: string, applicationPublicId: string): Observable<DuplicateCheckResponse> {
    const params = new HttpParams()
      .set('userPublicId', userPublicId)
      .set('applicationPublicId', applicationPublicId);
    return this.http
      .get<ApiResponse<DuplicateCheckResponse>>(
        `${API_BASE}/admin/application-assignments/check-duplicate`, { params })
      .pipe(map((r) => r.data!));
  }

  createAssignment(request: AssignmentRequest): Observable<ApplicationAssignment> {
    return this.http
      .post<ApiResponse<ApplicationAssignment>>(
        `${API_BASE}/admin/application-assignments`, request)
      .pipe(map((r) => r.data!));
  }

  updateAssignment(publicId: string, request: AssignmentRequest): Observable<ApplicationAssignment> {
    return this.http
      .put<ApiResponse<ApplicationAssignment>>(
        `${API_BASE}/admin/application-assignments/${publicId}`, request)
      .pipe(map((r) => r.data!));
  }

  updateAssignmentStatus(publicId: string, request: AssignmentStatusRequest): Observable<ApplicationAssignment> {
    return this.http
      .patch<ApiResponse<ApplicationAssignment>>(
        `${API_BASE}/admin/application-assignments/${publicId}/status`, request)
      .pipe(map((r) => r.data!));
  }

  bulkUpdateAssignmentStatus(request: BulkStatusRequest): Observable<BulkStatusResponse> {
    return this.http
      .patch<ApiResponse<BulkStatusResponse>>(
        `${API_BASE}/admin/application-assignments/bulk-status`, request)
      .pipe(map((r) => r.data!));
  }

  /** Legacy — get all assignments flat (used during dropdown fallback) */
  getAllAssignments(): Observable<ApplicationAssignment[]> {
    return this.getAssignmentsPaged({ size: 500, status: 'ACTIVE' })
      .pipe(map((r) => r.content));
  }

  deactivateAssignment(publicId: string): Observable<ApplicationAssignment> {
    return this.updateAssignmentStatus(publicId, { active: false });
  }

  // ----------------------------------------------------------------
  // Admin — User Import / Export (M2.2)
  // ----------------------------------------------------------------

  downloadUserTemplate(): void {
    this.http
      .get(`${API_BASE}/admin/users/template`, { responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'user_import_template.xlsx';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
        },
        error: () => { /* auth interceptor handles 401 */ },
      });
  }

  exportUsers(params?: { search?: string; roleCode?: string; status?: string }): void {
    let httpParams = new HttpParams();
    if (params?.search)   httpParams = httpParams.set('search',   params.search);
    if (params?.roleCode) httpParams = httpParams.set('roleCode', params.roleCode);
    if (params?.status)   httpParams = httpParams.set('status',   params.status);

    this.http
      .get(`${API_BASE}/admin/users/export`, { responseType: 'blob', params: httpParams })
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'users_export.xlsx';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
        },
        error: () => { /* auth interceptor handles 401 */ },
      });
  }

  validateUserImport(file: File): Observable<UserValidationResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<UserValidationResult>>(
        `${API_BASE}/admin/users/import/validate`, formData)
      .pipe(map((r) => r.data!));
  }

  confirmUserImport(file: File, importMode: 'STRICT' | 'PARTIAL' = 'STRICT'): Observable<UserImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('importMode', importMode);
    return this.http
      .post<ApiResponse<UserImportResult>>(
        `${API_BASE}/admin/users/import/confirm`, formData)
      .pipe(map((r) => r.data!));
  }

  // ----------------------------------------------------------------
  // Admin — User Delete (M2.3)
  // ----------------------------------------------------------------

  deleteUser(publicId: string, reason?: string): Observable<DeleteUserResult> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http
      .delete<ApiResponse<DeleteUserResult>>(
        `${API_BASE}/admin/users/${publicId}`, { params })
      .pipe(map((r) => r.data!));
  }

  bulkDeleteUsers(request: BulkDeleteRequest): Observable<BulkDeleteResponse> {
    return this.http
      .post<ApiResponse<BulkDeleteResponse>>(
        `${API_BASE}/admin/users/bulk-delete`, request)
      .pipe(map((r) => r.data!));
  }

  getUserImportHistory(params: {
    page?: number;
    size?: number;
    statusCode?: string;
    uploadedBy?: string;
  }): Observable<PagedResponse<UserImportHistoryEntry>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.statusCode)  httpParams = httpParams.set('statusCode',  params.statusCode);
    if (params.uploadedBy)  httpParams = httpParams.set('uploadedBy',  params.uploadedBy);
    return this.http
      .get<ApiResponse<PagedResponse<UserImportHistoryEntry>>>(
        `${API_BASE}/admin/users/import/history`, { params: httpParams })
      .pipe(map((r) => r.data!));
  }

  // ----------------------------------------------------------------
  // Technician — My Applications
  // ----------------------------------------------------------------

  getMyApplications(): Observable<Application[]> {
    return this.http
      .get<ApiResponse<Application[]>>(`${API_BASE}/technician/my-applications`)
      .pipe(map((r) => r.data ?? []));
  }

  // ----------------------------------------------------------------
  // Lead — Team Applications
  // ----------------------------------------------------------------

  getLeadApplications(): Observable<Application[]> {
    return this.http
      .get<ApiResponse<Application[]>>(`${API_BASE}/lead/applications`)
      .pipe(map((r) => r.data ?? []));
  }

  // ----------------------------------------------------------------
  // Admin — Application Master (M4 stabilization enhancement)
  // ----------------------------------------------------------------

  getApplicationsPaged(params: {
    page?: number;
    size?: number;
    search?: string;
    typeCode?: string;
    bundleCode?: string;
    portfolioCode?: string;
    lifecycleStatusCode?: string;
    status?: string;
    sortBy?: string;
    sortDirection?: string;
  }): Observable<PagedResponse<ApplicationMaster>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.typeCode) httpParams = httpParams.set('typeCode', params.typeCode);
    if (params.bundleCode) httpParams = httpParams.set('bundleCode', params.bundleCode);
    if (params.portfolioCode) httpParams = httpParams.set('portfolioCode', params.portfolioCode);
    if (params.lifecycleStatusCode) httpParams = httpParams.set('lifecycleStatusCode', params.lifecycleStatusCode);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);
    if (params.sortDirection) httpParams = httpParams.set('sortDirection', params.sortDirection);
    return this.http
      .get<ApiResponse<PagedResponse<ApplicationMaster>>>(`${API_BASE}/admin/applications`, { params: httpParams })
      .pipe(map((r) => r.data!));
  }

  getApplicationByPublicId(publicId: string): Observable<ApplicationMaster> {
    return this.http
      .get<ApiResponse<ApplicationMaster>>(`${API_BASE}/admin/applications/${publicId}`)
      .pipe(map((r) => r.data!));
  }

  createApplication(request: CreateApplicationRequest): Observable<ApplicationMaster> {
    return this.http
      .post<ApiResponse<ApplicationMaster>>(`${API_BASE}/admin/applications`, request)
      .pipe(map((r) => r.data!));
  }

  updateApplication(publicId: string, request: UpdateApplicationRequest): Observable<ApplicationMaster> {
    return this.http
      .put<ApiResponse<ApplicationMaster>>(`${API_BASE}/admin/applications/${publicId}`, request)
      .pipe(map((r) => r.data!));
  }

  updateApplicationStatus(publicId: string, request: ApplicationStatusRequest): Observable<ApplicationMaster> {
    return this.http
      .patch<ApiResponse<ApplicationMaster>>(`${API_BASE}/admin/applications/${publicId}/status`, request)
      .pipe(map((r) => r.data!));
  }

  deleteApplication(publicId: string): Observable<DeleteApplicationResult> {
    return this.http
      .delete<ApiResponse<DeleteApplicationResult>>(`${API_BASE}/admin/applications/${publicId}`)
      .pipe(map((r) => r.data!));
  }

  downloadApplicationTemplate(): void {
    this.http
      .get(`${API_BASE}/admin/applications/template`, { responseType: 'blob' })
      .subscribe({
        next: (blob) => this.downloadBlob(blob, 'Application_Master_Import_Template.xlsx'),
        error: () => { /* auth interceptor handles 401 */ },
      });
  }

  exportApplications(params?: {
    search?: string; typeCode?: string; bundleCode?: string;
    portfolioCode?: string; lifecycleStatusCode?: string; status?: string;
  }): void {
    let httpParams = new HttpParams();
    if (params?.search) httpParams = httpParams.set('search', params.search);
    if (params?.typeCode) httpParams = httpParams.set('typeCode', params.typeCode);
    if (params?.bundleCode) httpParams = httpParams.set('bundleCode', params.bundleCode);
    if (params?.portfolioCode) httpParams = httpParams.set('portfolioCode', params.portfolioCode);
    if (params?.lifecycleStatusCode) httpParams = httpParams.set('lifecycleStatusCode', params.lifecycleStatusCode);
    if (params?.status) httpParams = httpParams.set('status', params.status);

    this.http
      .get(`${API_BASE}/admin/applications/export`, { responseType: 'blob', params: httpParams })
      .subscribe({
        next: (blob) => this.downloadBlob(blob, 'applications_export.xlsx'),
        error: () => { /* auth interceptor handles 401 */ },
      });
  }

  validateApplicationImport(file: File): Observable<ApplicationValidationResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<ApplicationValidationResult>>(`${API_BASE}/admin/applications/import/validate`, formData)
      .pipe(map((r) => r.data!));
  }

  confirmApplicationImport(file: File): Observable<ApplicationImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<ApplicationImportResult>>(`${API_BASE}/admin/applications/import/confirm`, formData)
      .pipe(map((r) => r.data!));
  }

  // ----------------------------------------------------------------
  // Admin — Multi-Technician Assignment (M4 stabilization enhancement)
  // ----------------------------------------------------------------

  createBulkAssignments(request: BulkAssignmentRequest): Observable<BulkAssignmentResult> {
    return this.http
      .post<ApiResponse<BulkAssignmentResult>>(`${API_BASE}/admin/application-assignments/bulk`, request)
      .pipe(map((r) => r.data!));
  }

  downloadAssignmentTemplate(): void {
    this.http
      .get(`${API_BASE}/admin/application-assignments/template`, { responseType: 'blob' })
      .subscribe({
        next: (blob) => this.downloadBlob(blob, 'Technician_Assignment_Import_Template.xlsx'),
        error: () => { /* auth interceptor handles 401 */ },
      });
  }

  exportAssignmentsExcel(params?: {
    technicianPublicId?: string; typeCode?: string; bundleCode?: string;
    applicationPublicId?: string; status?: string;
  }): void {
    let httpParams = new HttpParams();
    if (params?.technicianPublicId) httpParams = httpParams.set('technicianPublicId', params.technicianPublicId);
    if (params?.typeCode) httpParams = httpParams.set('typeCode', params.typeCode);
    if (params?.bundleCode) httpParams = httpParams.set('bundleCode', params.bundleCode);
    if (params?.applicationPublicId) httpParams = httpParams.set('applicationPublicId', params.applicationPublicId);
    if (params?.status) httpParams = httpParams.set('status', params.status);

    this.http
      .get(`${API_BASE}/admin/application-assignments/export`, { responseType: 'blob', params: httpParams })
      .subscribe({
        next: (blob) => this.downloadBlob(blob, 'assignments_export.xlsx'),
        error: () => { /* auth interceptor handles 401 */ },
      });
  }

  validateAssignmentImport(file: File): Observable<AssignmentValidationResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<AssignmentValidationResult>>(`${API_BASE}/admin/application-assignments/import/validate`, formData)
      .pipe(map((r) => r.data!));
  }

  confirmAssignmentImport(file: File): Observable<AssignmentImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<ApiResponse<AssignmentImportResult>>(`${API_BASE}/admin/application-assignments/import/confirm`, formData)
      .pipe(map((r) => r.data!));
  }

  // ----------------------------------------------------------------
  // Shared blob download helper
  // ----------------------------------------------------------------

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }
}
