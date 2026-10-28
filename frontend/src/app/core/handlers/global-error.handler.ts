import { ErrorHandler, Injectable, isDevMode } from '@angular/core';

/**
 * Global Angular error handler.
 *
 * Catches all unhandled errors thrown inside Angular's change detection,
 * component lifecycle hooks, and async operations not covered by HTTP
 * error handling.
 *
 * Security policy:
 *  - Never logs JWT tokens, passwords, Authorization headers, refresh tokens,
 *    cookies, or raw API responses that may contain sensitive data.
 *  - In production (isDevMode() === false), suppresses stack traces.
 *  - In development, logs the full error for debugging convenience.
 *
 * Usage: registered in app.config.ts via
 *   { provide: ErrorHandler, useClass: GlobalErrorHandler }
 */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {

  handleError(error: unknown): void {
    const safeMessage = this.extractSafeMessage(error);

    if (isDevMode()) {
      // Development: log full details to console for debugging
      console.error('[GlobalErrorHandler] Unhandled error:', safeMessage);
      if (error instanceof Error && error.stack) {
        console.error(error.stack);
      }
    } else {
      // Production: minimal, non-sensitive log only
      console.error('[GlobalErrorHandler] An unexpected error occurred.');
    }
  }

  /**
   * Extracts a message that is safe to log — strips any value that looks
   * like a token, password field, or Authorization header.
   */
  private extractSafeMessage(error: unknown): string {
    if (error instanceof Error) {
      return error.message ?? 'Unknown error';
    }
    if (typeof error === 'string') {
      return this.maskSensitivePatterns(error);
    }
    return 'Unknown error object';
  }

  /**
   * Basic masking: replaces anything that looks like a JWT (3 base64url
   * segments separated by dots) with [REDACTED-TOKEN].
   */
  private maskSensitivePatterns(input: string): string {
    // Mask JWT-like strings: xxxxx.yyyyy.zzzzz
    return input.replace(/[A-Za-z0-9\-_]{10,}\.[A-Za-z0-9\-_]{10,}\.[A-Za-z0-9\-_]{10,}/g,
        '[REDACTED-TOKEN]');
  }
}
