# Organizer Profile Specification

## Problem Statement

Organizers need a persistent organization profile that supplies the official organization information used by tournaments. Today an organizer can authenticate, but Squadra has no dedicated place to store the organization name, contact details, location, or public-facing description that tournament participants should see.

## Goals

- [x] Authenticated ORGANIZER users can create their organization profile.
- [x] Authenticated ORGANIZER users can edit their organization profile.
- [x] The backend persists one organizer-owned profile that future tournaments can reference for organization information.
- [x] The frontend provides a mobile-ready organizer profile screen inside the authenticated app shell.

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
| --- | --- |
| Multiple organizations per user | MVP identity currently has one global `ORGANIZER` role and no organization membership model. |
| Multiple admins per organization | Contextual organization administration roles are not defined in the current role model. |
| Public organizer pages | This feature stores tournament organization information; public discovery is a separate surface. |
| Tournament creation or tournament organizer assignment | Tournament features will consume this profile later; this feature does not create tournaments. |
| Logo/photo upload | Requires storage, scanning and image policy decisions outside this feature. |
| Payment recipient/KYC data | Payment docs require separate secure payment/provider design. |
| Social links and marketing website fields | Not required for tournament operation and can expand later. |

---

## Assumptions & Open Questions

Every ambiguity is resolved or recorded here - nothing is left silently unclear.

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Profile ownership | One profile belongs to the authenticated ORGANIZER user | Existing auth model has global `ORGANIZER` users and `OrganizerProfile` appears under Identity in the domain model. | n |
| API shape | Use `/api/v1/organizers/me/profile` with `GET` and idempotent `PUT` upsert | Mirrors the athlete profile "me resource" pattern and avoids trusting client-supplied owner IDs. | n |
| Initial fields | `organizationName`, `contactName`, `contactEmail`, `phone`, `city`, `state`, `website`, `description` | These fields support tournament display/contact needs without adding payment, storage or membership scope. | n |
| Contact email default | `contactEmail` is required and may differ from login email | Organizations often publish a contact mailbox distinct from account credentials. | n |
| Phone and website | Optional validated strings | Useful for tournaments, but not required to create a profile. | n |
| Visibility | Profile is private to the organizer and authenticated product surfaces in this feature | Public organizer pages and tournament publication behavior are out of scope. | n |
| Deletion | No organizer profile deletion in MVP | Tournaments need stable organization information; deletion/lifecycle rules belong with tournament history. | n |

**Open questions:** none - all unresolved decisions are logged as assumptions above.

---

## User Stories

### P1: Create My Organization Profile ⭐ MVP

**User Story**: As an ORGANIZER, I want to create my organization profile so that Squadra has official organization information for my tournaments.

**Why P1**: This is the minimum vertical slice for organizer-owned tournament context.

**Acceptance Criteria**:

1. WHEN an authenticated ORGANIZER submits a valid organization profile for the first time THEN the system SHALL persist the profile linked to that organizer's user ID and return `200` with the saved profile.
2. WHEN an authenticated ORGANIZER requests their profile before creating one THEN the system SHALL return `404` with a non-sensitive `ORGANIZER_PROFILE_NOT_FOUND` error.
3. IF an ATHLETE or unauthenticated request attempts to create an organizer profile THEN the system SHALL return `403` or `401` without writing profile data.
4. The system SHALL never accept a client-supplied user ID as organizer profile ownership proof.

**Independent Test**: Register/login as ORGANIZER, submit the organizer profile form, and see the saved profile returned from `/api/v1/organizers/me/profile`.

---

### P1: Edit My Organization Profile ⭐ MVP

**User Story**: As an ORGANIZER, I want to edit my organization profile so that tournament participants see current organization information.

**Why P1**: Tournament organization data changes over time and must remain organizer-managed.

**Acceptance Criteria**:

1. WHEN an authenticated ORGANIZER submits valid changes to an existing profile THEN the system SHALL update only that organizer's profile and return `200` with the latest profile.
2. WHEN an authenticated ORGANIZER repeats the same profile submission THEN the system SHALL keep one profile row for the organizer and return `200` with the same values.
3. The system SHALL update the profile `updatedAt` timestamp when saved.

**Independent Test**: Save a profile, change the organization name or description, refresh the page, and see the updated values.

---

### P1: Validate Organization Profile Input ⭐ MVP

**User Story**: As an ORGANIZER, I want clear validation so that organization information is usable and safe for tournaments.

**Why P1**: Tournament-facing organization data must be reliable and bounded from the first release.

**Acceptance Criteria**:

1. IF `organizationName` is blank, shorter than 2 characters, or longer than 100 characters THEN the system SHALL return `422`.
2. IF `contactName` is blank, shorter than 2 characters, or longer than 100 characters THEN the system SHALL return `422`.
3. IF `contactEmail` is blank, malformed, or longer than 320 characters THEN the system SHALL return `422`.
4. IF `phone` is supplied and longer than 30 characters THEN the system SHALL return `422`.
5. IF `city` is blank, shorter than 2 characters, or longer than 80 characters THEN the system SHALL return `422`.
6. IF `state` is blank, shorter than 2 characters, or longer than 80 characters THEN the system SHALL return `422`.
7. IF `website` is supplied and is not an `http://` or `https://` URL with at most 200 characters THEN the system SHALL return `422`.
8. IF `description` is supplied and longer than 500 characters THEN the system SHALL return `422`.

**Independent Test**: Submit invalid values through API and frontend tests and verify validation failures without persistence.

---

### P1: Use the Organizer Profile Screen ⭐ MVP

**User Story**: As an ORGANIZER, I want an authenticated profile screen so that I can manage tournament organization information without leaving the app.

**Why P1**: The feature is incomplete without a role-appropriate organizer UI.

**Acceptance Criteria**:

1. WHEN an authenticated ORGANIZER opens `/organizer/profile` THEN the frontend SHALL show an organizer-focused profile form with existing values when present.
2. WHEN no organizer profile exists THEN the frontend SHALL show an empty-state prompt to create the organization profile.
3. WHEN the profile save succeeds THEN the frontend SHALL show the saved profile values and a success status without requiring a full page reload.
4. IF the backend returns `401` during profile load or save THEN the frontend SHALL try one refresh-token recovery and retry the request once.
5. IF profile load or save fails after recovery THEN the frontend SHALL show a non-sensitive error message and keep the organizer's typed values.
6. WHILE the authenticated user role is `ORGANIZER` THEN the Home sidebar SHALL link to `/organizer/profile` as the organizer profile destination.

**Independent Test**: Render the profile screen with empty, saved, validation-error and expired-access-token states.

---

## Edge Cases

- IF two profile saves race for the same ORGANIZER THEN the system SHALL keep a single profile row per organizer and persist one complete valid submission.
- IF the persisted profile belongs to a deleted user THEN the database SHALL remove or reject orphaned profile data through the user foreign key.
- IF a request includes fields outside the profile contract THEN the system SHALL ignore or reject them without persisting unknown data.
- WHILE the profile is private in the MVP, the system SHALL not expose it through any public endpoint.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| OPROF-01 | P1: Create My Organization Profile | Execute | Verified |
| OPROF-02 | P1: Edit My Organization Profile | Execute | Verified |
| OPROF-03 | P1: Validate Organization Profile Input | Execute | Verified |
| OPROF-04 | P1: Use the Organizer Profile Screen | Execute | Verified |
| OPROF-05 | Edge Cases | Execute | Verified |

**Coverage:** 5 total, 5 mapped to tasks, 0 unmapped.

---

## Success Criteria

- [x] ORGANIZER users can create and edit an organization profile from the UI.
- [x] API authorization prevents ATHLETE and unauthenticated organizer profile writes.
- [x] Backend tests prove ownership, validation and upsert behavior.
- [x] Frontend tests prove empty, edit, save and auth-recovery states.
- [x] Backend coverage remains at or above 80%.
- [x] Frontend checks pass.
