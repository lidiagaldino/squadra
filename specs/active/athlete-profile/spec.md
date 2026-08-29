# Athlete Profile Specification

## Problem Statement

Athletes need a permanent sports profile that belongs to their identity, not to a team or tournament. The profile lets an authenticated athlete record the basic football information Squadra needs for future teams, registrations, sports history, statistics and achievements without exposing public athlete search or social pages in the MVP.

## Goals

- [ ] Authenticated ATHLETE users can create their own sports profile.
- [ ] Authenticated ATHLETE users can edit their own sports profile.
- [ ] The backend persists profile data as athlete-owned, team-independent information.
- [ ] The frontend provides a mobile-ready athlete profile screen that feels personal, sporty and consistent with Squadra.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Public athlete pages | MVP docs explicitly exclude public search/social surfaces. |
| Athlete search, followers, feed or chat | MVP docs explicitly exclude social discovery and chat. |
| Organizer editing athlete profiles | Profiles are self-managed and organizer permissions are contextual to tournaments/teams. |
| Teams, invitations and roster membership | Those belong to future team/registration features. |
| Official stats, badges and sports history timeline entries | This feature prepares the identity profile; official history is produced by tournament/statistics features. |
| Profile photo upload | Requires storage, content scanning and image policy decisions outside this feature. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Profile ownership | Only the authenticated ATHLETE can create or edit their own profile | Product docs say the athlete is permanent and security docs require backend authorization. | n |
| API shape | Use `/api/v1/athletes/me/profile` with `GET` and idempotent `PUT` upsert | Avoids trusting client-supplied user IDs and supports create/edit through one stable resource. | n |
| Initial profile fields | `displayName`, `fullName`, `birthDate`, `primaryPosition`, `secondaryPositions`, `dominantFoot`, `jerseyNumber`, `city`, `state`, `bio` | Captures useful sports identity without public/social data or file storage. | n |
| Birth date | Optional but stored as `DATE` when supplied | Future categories may need age, but privacy rules require collecting only necessary data. | n |
| Positions | Fixed enum: `GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, `FORWARD` | Keeps validation simple for MVP while matching football profile needs. | n |
| Dominant foot | Optional enum: `LEFT`, `RIGHT`, `BOTH` | Useful sports detail; optional to avoid blocking profile completion. | n |
| Visibility | Profile is private to the athlete in this feature | MVP has no public athlete pages and no public search. | n |

**Open questions:** none - all unresolved decisions are logged as assumptions above.

---

## User Stories

### P1: Create My Sports Profile ⭐ MVP

**User Story**: As an ATHLETE, I want to create my sports profile so that Squadra can start representing my football identity independently of any team.

**Why P1**: This is the minimum vertical slice for athlete-owned profile data and future registrations.

**Acceptance Criteria**:

1. WHEN an authenticated ATHLETE submits a valid profile for the first time THEN the system SHALL persist the profile linked to that athlete's user ID and return `200` with the saved profile.
2. WHEN an authenticated ATHLETE requests their profile before creating one THEN the system SHALL return `404` with a non-sensitive `PROFILE_NOT_FOUND` error.
3. IF an ORGANIZER or unauthenticated request attempts to create an athlete profile THEN the system SHALL return `403` or `401` without writing profile data.
4. The system SHALL never accept a client-supplied user ID as profile ownership proof.

**Independent Test**: Register/login as ATHLETE, submit the profile form, and see the saved profile returned from `/api/v1/athletes/me/profile`.

---

### P1: Edit My Sports Profile ⭐ MVP

**User Story**: As an ATHLETE, I want to edit my sports profile so that my football information stays current.

**Why P1**: A profile must remain athlete-managed after initial creation.

**Acceptance Criteria**:

1. WHEN an authenticated ATHLETE submits valid changes to an existing profile THEN the system SHALL update only that athlete's profile and return `200` with the latest profile.
2. WHEN an authenticated ATHLETE repeats the same profile submission THEN the system SHALL keep one profile row for the athlete and return `200` with the same values.
3. The system SHALL update the profile `updatedAt` timestamp when saved.

**Independent Test**: Save a profile, change the primary position and bio, refresh the page, and see the updated values.

---

### P1: Validate Sports Profile Input ⭐ MVP

**User Story**: As an ATHLETE, I want clear validation so that my profile contains useful and safe sports information.

**Why P1**: Validation protects data quality and privacy from the first profile release.

**Acceptance Criteria**:

1. IF `displayName` is blank, shorter than 2 characters, or longer than 60 characters THEN the system SHALL return `422`.
2. IF `fullName` is supplied and longer than 120 characters THEN the system SHALL return `422`.
3. IF `birthDate` is in the future THEN the system SHALL return `422`.
4. IF `primaryPosition` is not one of `GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, or `FORWARD` THEN the system SHALL return `422`.
5. IF `secondaryPositions` includes duplicates or includes the `primaryPosition` THEN the system SHALL return `422`.
6. IF `dominantFoot` is supplied and is not one of `LEFT`, `RIGHT`, or `BOTH` THEN the system SHALL return `422`.
7. IF `jerseyNumber` is supplied and is outside `1..99` THEN the system SHALL return `422`.
8. IF `bio` is supplied and longer than 280 characters THEN the system SHALL return `422`.

**Independent Test**: Submit invalid values from the frontend form and API tests and verify field-safe validation failures.

---

### P1: Use the Athlete Profile Screen ⭐ MVP

**User Story**: As an ATHLETE, I want a focused profile screen so that managing my football story feels personal and easy.

**Why P1**: The feature is incomplete without the athlete-facing UI.

**Acceptance Criteria**:

1. WHEN an authenticated ATHLETE opens `/athlete/profile` THEN the frontend SHALL show a sports-oriented profile form with existing values when present.
2. WHEN no profile exists THEN the frontend SHALL show an empty-state prompt to create the athlete's profile.
3. WHEN the profile save succeeds THEN the frontend SHALL show the saved profile values and a success status without requiring a full page reload.
4. IF the backend returns `401` during profile load or save THEN the frontend SHALL try one refresh-token recovery and retry the request once.
5. IF profile load or save fails after recovery THEN the frontend SHALL show a non-sensitive error message and keep the athlete's typed values.

**Independent Test**: Render the profile screen with empty, saved, validation-error and expired-access-token states.

---

## Edge Cases

- IF two profile saves race for the same ATHLETE THEN the system SHALL keep a single profile row per athlete and persist one complete valid submission.
- IF the persisted profile belongs to a deleted user THEN the database SHALL remove or reject orphaned profile data through the user foreign key.
- IF a request includes fields outside the profile contract THEN the system SHALL ignore or reject them without persisting unknown data.
- WHILE the profile is private in the MVP, the system SHALL not expose it through any public endpoint.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| APROF-01 | P1: Create My Sports Profile | Verified | Verified |
| APROF-02 | P1: Edit My Sports Profile | Verified | Verified |
| APROF-03 | P1: Validate Sports Profile Input | Verified | Verified |
| APROF-04 | P1: Use the Athlete Profile Screen | Verified | Verified |
| APROF-05 | Edge Cases | Verified | Verified |

**Coverage:** 5 total, 5 mapped to tasks, 0 unmapped.

---

## Success Criteria

- [ ] ATHLETE users can create and edit a profile from the UI.
- [ ] API authorization prevents ORGANIZER and unauthenticated profile writes.
- [ ] Backend tests prove ownership, validation and upsert behavior.
- [ ] Frontend tests prove empty, edit, save and auth-recovery states.
- [ ] Backend coverage remains at or above 80%.
- [ ] Frontend checks pass.
