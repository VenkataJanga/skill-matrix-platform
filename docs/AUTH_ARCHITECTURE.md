# Authentication Architecture — Forgot Password & Okta-Readiness

**Status:** Development / Pilot implementation
**Scope:** M3+ enhancement — local Forgot Password + Admin-assisted fallback
**Out of scope:** Okta / Corporate SSO integration (explicitly not implemented)

## 1. Why this document exists

The Skill Matrix Platform currently authenticates users against its own
`users` table (username/password + JWT access/refresh tokens — see
`AuthService`, `JwtService`, `RefreshTokenService`). This is referred to
below as **LOCAL** authentication mode.

A future milestone may introduce Okta or another corporate SSO provider for
production deployment. This document describes how the Forgot Password
feature was built so that transition requires **no changes** to:

- User Master (`User` entity, roles, RBAC)
- Application assignments, Skill Management, Assessment workflows
- Any `@PreAuthorize` authorization rule in the codebase

## 2. Current Authentication Mode

```yaml
app:
  auth:
    mode: LOCAL   # LOCAL (today) | OKTA (future placeholder, NOT implemented)
```

- Implemented by `AuthModeProperties` (`backend/.../config/AuthModeProperties.java`).
- `mode: LOCAL` is the only functioning value today. `OKTA` is a documented
  placeholder for a future task — no Okta client, redirect URIs, or secrets
  exist anywhere in this codebase.
- All business services (`ApplicationMasterService`, `AdminAssignmentService`,
  `SkillMasterService`, assessment services, etc.) depend only on
  `UserPrincipal` / `User`, never on "how" the user authenticated. This means
  swapping in an OIDC-based `AuthenticationProvider` later does not require
  touching any of those services.

## 3. Forgot Password — Design

### 3.1 Token lifecycle (mirrors `RefreshToken`)

- `PasswordResetService.requestReset()` generates a cryptographically random
  token (32 bytes, URL-safe Base64), computes its SHA-256 hash, and persists
  **only the hash** in `password_reset_tokens` (`PasswordResetToken` entity).
- The raw token is never logged, never stored, and is only ever present in
  the HTTP response body or outbound email link.
- Tokens expire after `app.password-reset.expiry-minutes` (default 20) and
  are single-use (`used_at` set + `is_active=false` on consumption).
- Requesting a new reset immediately invalidates any previously issued,
  still-active tokens for that user (`invalidateAllActiveForUser`).

### 3.2 Account enumeration prevention

`POST /api/v1/auth/forgot-password` always returns the identical generic
message regardless of whether:
- the account exists,
- the account is inactive,
- or the identifier is simply unknown.

No token is issued for inactive accounts, and inactive accounts are **never**
reactivated by a password reset (checked again at `resetPassword()` time).

### 3.3 Session invalidation

On a successful reset (self-service or admin-assisted), all of the user's
refresh tokens are revoked via `RefreshTokenService.revokeAllForUser()`,
immediately invalidating any other active sessions.

### 3.4 Admin-assisted fallback

No SMTP/email infrastructure exists in this project (`pom.xml` has no mail
starter, no mail config in any `application-*.yml`). Per the task's explicit
constraint, we did **not** add one. Instead:

- `POST /api/v1/admin/users/{publicId}/password-reset` (ADMIN-only) generates
  the same secure one-time reset link.
- The Admin **never** sees, retrieves, or chooses the user's permanent
  password — only a link (or, in `dev`, the link is additionally returned in
  the API response for local testing convenience).
- `must_change_password` is set as a safety net.
- The target user's active sessions are revoked immediately.

### 3.5 DEV-only convenience

```yaml
# application-dev.yml ONLY
app:
  password-reset:
    dev-expose-token: true
```

`PasswordResetService` only returns the raw reset link in the API response
when **both** of the following are true:
1. `app.password-reset.dev-expose-token=true`
2. The active Spring profile is `dev`

This double-guard (property *and* code-level profile check) ensures the raw
token can never leak in `qa`/`prod` even if the property were accidentally
copied into another profile file.

## 4. Endpoints

| Method | Path                                             | Auth         | Notes |
|--------|---------------------------------------------------|--------------|-------|
| POST   | `/api/v1/auth/forgot-password`                     | Public       | Generic response always |
| POST   | `/api/v1/auth/reset-password`                       | Public       | Consumes one-time token |
| POST   | `/api/v1/admin/users/{publicId}/password-reset`     | ADMIN only   | Admin-assisted fallback |

Both public endpoints are listed in `SecurityConfig.PUBLIC_ENDPOINTS`.

## 5. Future Okta Migration Checklist (not implemented now)

When a future milestone introduces Okta:

1. Add an `OktaAuthenticationProvider` (or OIDC resource server config)
   alongside the existing `DaoAuthenticationProvider` — do not remove LOCAL
   mode outright if a transition/parallel-run period is needed.
2. Set `app.auth.mode=OKTA` and gate `/forgot-password`, `/reset-password`,
   and the admin password-reset endpoint behind
   `AuthModeProperties.isLocalModeActive()` (disable them when Okta is
   active, since Okta owns password lifecycle).
3. No changes required to `User`, `UserRole`, `Role`, RBAC checks,
   `UserPrincipal`, or any feature module (applications, skills,
   assignments, assessments) — they all already depend only on the
   authenticated principal, not the auth mechanism.
4. Map Okta claims (e.g. `email`, `groups`) to existing `User`/`Role`
   records via username or email lookup — the existing unique `username`
   and `email` columns are sufficient join keys; no schema change needed.

## 6. Password Policy

Reset passwords are subject to the exact same policy already enforced for
self-service `ChangePasswordRequest`: minimum length 8 characters
(`@Size(min = 8)`), enforced identically in `ResetPasswordRequest`. No new or
conflicting password rules were introduced.
