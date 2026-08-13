import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../models/api-response.model';
import {
  Application,
  ApplicationAssignment,
  ApplicationType,
  AssignmentRequest,
  AssignmentStatusRequest,
  BulkStatusRequest,
  BulkStatusResponse,
  Bundle,
  CreateUserRequest,
  DuplicateCheckResponse,
  PagedResponse,
  UpdateUserRequest,
  UserDetail,
  UserStatusRequest,
  UserSummary,
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
}
