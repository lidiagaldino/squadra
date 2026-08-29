# Authentication Specification

## Problem Statement

Squadra needs server-enforced access for ATHLETE and ORGANIZER accounts.

## Assumptions & Open Questions

| Decision | Default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Recovery delivery | Mailpit SMTP locally | Reset tokens are never returned or logged. | y |

**Open questions:** none.

## User Stories

### P1: Account access

As an ATHLETE or ORGANIZER, I can register, sign in, refresh/logout, and recover my password securely.

## Acceptance Criteria

1. WHEN valid ATHLETE or ORGANIZER registration is submitted THEN the system SHALL persist a BCrypt hash and return 201 without credentials.
2. WHEN valid credentials are submitted THEN the system SHALL return a 15-minute access token and set a 30-day HttpOnly refresh cookie.
3. IF credentials or refresh token are invalid THEN the system SHALL return 401 without account enumeration.
4. WHEN logout occurs THEN the system SHALL revoke the submitted refresh token and clear its cookie.
5. WHEN recovery is requested THEN the system SHALL return 202 and SHALL deliver a single-use, 30-minute reset token only through Mailpit.
6. WHEN a valid reset is submitted THEN the system SHALL replace the password hash and revoke all refresh tokens.
7. The system SHALL enforce authorization server-side from validated access-token claims.

## Requirement Traceability

| Requirement ID | Status |
| --- | --- |
| AUTH-01 through AUTH-07 | Pending |
