import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap, catchError, throwError, switchMap } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import {
  LoginRequest,
  LoginResponse,
  ChangePasswordRequest,
  TokenResponse,
  UserMe,
} from '../models/auth.model';

const API_BASE = 'http://localhost:8080/api/v1/auth';
const ACCESS_TOKEN_KEY = 'sm_access_token';
const REFRESH_TOKEN_KEY = 'sm_refresh_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSubject = new BehaviorSubject<UserMe | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  // ----------------------------------------------------------------
  // Token helpers
  // ----------------------------------------------------------------

  getAccessToken(): string | null {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY);
  }

  private storeTokens(accessToken: string, refreshToken: string): void {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    sessionStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }

  private clearTokens(): void {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }

  getCurrentUser(): UserMe | null {
    return this.currentUserSubject.getValue();
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user?.roles?.includes(role) ?? false;
  }

  // ----------------------------------------------------------------
  // Auth API calls
  // ----------------------------------------------------------------

  login(credentials: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http
      .post<ApiResponse<LoginResponse>>(`${API_BASE}/login`, credentials)
      .pipe(
        tap((res) => {
          if (res.success && res.data) {
            this.storeTokens(res.data.accessToken, res.data.refreshToken);
          }
        })
      );
  }

  loadCurrentUser(): Observable<ApiResponse<UserMe>> {
    return this.http.get<ApiResponse<UserMe>>(`${API_BASE}/me`).pipe(
      tap((res) => {
        if (res.success && res.data) {
          this.currentUserSubject.next(res.data);
        }
      }),
      catchError((err) => {
        this.currentUserSubject.next(null);
        return throwError(() => err);
      })
    );
  }

  refreshToken(): Observable<ApiResponse<TokenResponse>> {
    const refreshToken = this.getRefreshToken();
    return this.http
      .post<ApiResponse<TokenResponse>>(`${API_BASE}/refresh`, { refreshToken })
      .pipe(
        tap((res) => {
          if (res.success && res.data) {
            sessionStorage.setItem(ACCESS_TOKEN_KEY, res.data.accessToken);
          }
        })
      );
  }

  changePassword(payload: ChangePasswordRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${API_BASE}/change-password`, payload);
  }

  logout(): void {
    const token = this.getAccessToken();
    if (token) {
      this.http
        .post<ApiResponse<void>>(`${API_BASE}/logout`, {})
        .subscribe({ error: () => {} }); // fire and forget
    }
    this.clearTokens();
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  // ----------------------------------------------------------------
  // Post-login redirect based on role
  // ----------------------------------------------------------------

  redirectAfterLogin(user: UserMe): void {
    if (user.roles.includes('ADMIN')) {
      this.router.navigate(['/admin']);
    } else if (user.roles.includes('LEAD_MANAGER')) {
      this.router.navigate(['/lead']);
    } else if (user.roles.includes('TECHNICIAN')) {
      this.router.navigate(['/technician']);
    } else {
      this.router.navigate(['/unauthorized']);
    }
  }

  // ----------------------------------------------------------------
  // Init — restore session on browser refresh
  // ----------------------------------------------------------------

  initSession(): Promise<void> {
    if (!this.isLoggedIn()) {
      return Promise.resolve();
    }
    return new Promise((resolve) => {
      this.loadCurrentUser().subscribe({
        next: () => resolve(),
        error: () => {
          this.clearTokens();
          resolve();
        },
      });
    });
  }
}
