import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpErrorResponse,
} from '@angular/common/http';
import { inject, isDevMode } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

/**
 * Sanitises a URL for safe logging — strips query params that might contain tokens.
 * Never logs the Authorization header, cookies, JWT, or refresh tokens.
 */
function safeUrl(url: string): string {
  try {
    const u = new URL(url, 'http://localhost');
    return u.pathname; // log path only, drop query string
  } catch {
    return url.split('?')[0];
  }
}

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getAccessToken();
  const authReq = token ? addToken(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Safe error logging — never logs token, Authorization header, or request body
      if (isDevMode()) {
        if (error.status >= 500) {
          console.error(`[HTTP] ${error.status} ${safeUrl(req.url)} — ${error.message}`);
        } else if (error.status >= 400 && error.status !== 401) {
          console.warn(`[HTTP] ${error.status} ${safeUrl(req.url)} — ${error.error?.message ?? error.message}`);
        }
        // 401 is handled silently below (token refresh flow)
      }

      // 401 on a non-refresh endpoint → try to refresh once
      if (
        error.status === 401 &&
        !req.url.includes('/auth/refresh') &&
        !req.url.includes('/auth/login')
      ) {
        const refreshToken = authService.getRefreshToken();
        if (refreshToken) {
          return authService.refreshToken().pipe(
            switchMap((res) => {
              if (res.success && res.data) {
                const retryReq = addToken(req, res.data.accessToken);
                return next(retryReq);
              }
              authService.logout();
              return throwError(() => error);
            }),
            catchError(() => {
              authService.logout();
              return throwError(() => error);
            })
          );
        }
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });
}
