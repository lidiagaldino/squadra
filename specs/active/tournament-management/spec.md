# Tournament Management Specification

## Problem Statement

Organizers need a private tournament workspace where they can create, maintain, publish, and manage tournament information before registration and competition features consume it. Tournament categories must remain separate entities created only after the tournament exists, because each category owns its own registrations, payments, limits, format, rules, and schedule.

## Goals

- [ ] Authenticated `ORGANIZER` users can create private draft tournaments.
- [ ] Tournament owners can edit draft and published tournament information with deterministic validation.
- [ ] Tournament owners can publish tournaments without creating categories implicitly.
- [ ] Tournament owners can close, cancel, and reopen published tournaments within allowed status transitions.
- [ ] Categories remain separate entities and are not accepted in tournament create/edit payloads.
- [ ] The frontend provides mobile-ready `/tournaments` and `/tournaments/{tournamentId}` screens inside the authenticated app shell.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Tournament category CRUD | Categories are separate entities created after the tournament; this feature only preserves that boundary. |
| Tournament registration | Registration belongs to `Team + Tournament + Category` and requires team/category flows. |
| Payments, refunds, webhooks | Financial integrity and gateway integration are separate features. |
| Competition engine, draw, groups, brackets, standings | Only approved registrations enter competition; no registration exists in this feature. |
| Match scheduling and venues/courts | Scheduling belongs to category/competition operations. |
| Public anonymous tournament pages | MVP tournaments remain authenticated/private unless a later publication feature opens public discovery. |
| Organizer collaboration | Only the creating organizer owns and manages the tournament in this MVP. |
| Tournament deletion | Lifecycle/history/audit rules are needed before deletion is safe. |
| Rich audit log UI | Status transitions should be timestamped now; full audit trail is future scope. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Tournament owner | The authenticated creating `ORGANIZER` is the sole tournament owner | Security docs say organizers manage only authorized tournaments; collaboration is out of scope. | n |
| Initial status | New tournaments start as `DRAFT` | Drafts let organizers complete information before publication. | n |
| Published status | Publishing changes `DRAFT` to `OPEN` | Existing docs list `OPEN` as the registration-ready public state. | n |
| Manage statuses | MVP supports `DRAFT`, `OPEN`, `CLOSED`, and `CANCELLED` | `IN_PROGRESS` and `FINISHED` require registrations/competition engine state. | n |
| Reopen behavior | Owners can reopen `CLOSED` tournaments to `OPEN` before competition exists | Without registrations/competition, reopening is a safe management correction. | n |
| Cancellation behavior | Owners can cancel `DRAFT`, `OPEN`, or `CLOSED`; cancelled tournaments are read-only except status is visible | Prevents silent mutation of cancelled official context. | n |
| Organizer profile dependency | Tournament creation does not require an organizer profile row | Organizer Profile exists, but blocking creation would add a dependency not stated in docs. | n |
| Date fields | `startDate`, `endDate`, and `registrationDeadline` use ISO local dates | Existing frontend uses date inputs; timezone-specific scheduling is later scope. | n |
| Date ordering | `registrationDeadline <= startDate <= endDate` | Registration should close no later than tournament start, and end cannot precede start. | n |
| Category boundary | Create/edit payloads reject `categories`, `categoryIds`, or category-like fields with `422` | Categories must be created separately after the tournament. | n |
| Unknown request fields | Unknown fields return `422` | Matches Team Management contract and prevents silent persistence ambiguity. | n |
| Visibility | Organizers list only tournaments they own | No organizer collaboration or public discovery in MVP. | n |
| Frontend routes | `/tournaments` lists and creates; `/tournaments/{tournamentId}` edits and manages status | Matches existing team list/detail route pattern. | n |

**Open questions:** none - all unresolved decisions are logged as assumptions above.

---

## User Stories

### P1: Create Tournament ⭐ MVP

**User Story**: As an `ORGANIZER`, I want to create a tournament so that I can prepare the event before creating categories or accepting registrations.

**Acceptance Criteria**:

1. WHEN an authenticated `ORGANIZER` submits a valid tournament THEN the system SHALL persist it with status `DRAFT` and return `201`.
2. WHEN a tournament is created THEN the system SHALL set `ownerOrganizerUserId` from the verified access token.
3. WHEN a tournament is created THEN the system SHALL NOT create tournament categories.
4. IF an unauthenticated request attempts to create a tournament THEN the system SHALL return `401` without writing data.
5. IF an authenticated `ATHLETE` attempts to create a tournament THEN the system SHALL return `403` without writing data.
6. IF a create payload includes `categories`, `categoryIds`, or category configuration fields THEN the system SHALL return `422` without writing data.
7. IF a create payload includes unknown fields THEN the system SHALL return `422` without writing data.

**Independent Test**: Register/login as organizer, create a valid tournament, and verify a `DRAFT` tournament exists with no categories.

---

### P1: Validate Tournament Information ⭐ MVP

**User Story**: As an `ORGANIZER`, I want tournament fields validated so that published tournament information is usable and consistent.

**Acceptance Criteria**:

1. IF `name` is blank, shorter than 3 characters, or longer than 120 characters THEN the system SHALL return `422`.
2. IF `description` is supplied and longer than 1000 characters THEN the system SHALL return `422`.
3. IF `city` is blank, shorter than 2 characters, or longer than 80 characters THEN the system SHALL return `422`.
4. IF `state` is blank, shorter than 2 characters, or longer than 80 characters THEN the system SHALL return `422`.
5. IF `venueName` is supplied and longer than 120 characters THEN the system SHALL return `422`.
6. IF `rules` is supplied and longer than 5000 characters THEN the system SHALL return `422`.
7. IF `startDate` is missing THEN the system SHALL return `422`.
8. IF `endDate` is missing THEN the system SHALL return `422`.
9. IF `registrationDeadline` is missing THEN the system SHALL return `422`.
10. IF `endDate` is before `startDate` THEN the system SHALL return `422`.
11. IF `registrationDeadline` is after `startDate` THEN the system SHALL return `422`.

**Independent Test**: Submit each invalid field independently and verify `422`; submit valid data and verify persistence.

---

### P1: List My Tournaments ⭐ MVP

**User Story**: As an `ORGANIZER`, I want to see tournaments I own so that I can continue setup or manage published events.

**Acceptance Criteria**:

1. WHEN an authenticated `ORGANIZER` requests `/tournaments` THEN the system SHALL return only tournaments owned by that organizer.
2. WHEN an authenticated `ORGANIZER` has no tournaments THEN the system SHALL return `200` with an empty list.
3. IF an unauthenticated request attempts to list tournaments THEN the system SHALL return `401`.
4. IF an authenticated `ATHLETE` attempts to list organizer tournaments THEN the system SHALL return `403`.
5. The system SHALL include `status`, core identity fields, and category count for each tournament.

**Independent Test**: Create tournaments for two organizers and verify each list contains only the caller's tournaments.

---

### P1: View Tournament Detail ⭐ MVP

**User Story**: As an `ORGANIZER`, I want to open a tournament detail page so that I can review status, official information, and next management actions.

**Acceptance Criteria**:

1. WHEN a tournament owner requests `/tournaments/{tournamentId}` THEN the system SHALL return tournament identity, date fields, status, owner, timestamps, and category count.
2. IF an authenticated organizer who does not own the tournament requests detail THEN the system SHALL return `404`.
3. IF an unauthenticated request attempts to view detail THEN the system SHALL return `401`.
4. IF an authenticated `ATHLETE` attempts to view organizer tournament detail THEN the system SHALL return `403`.
5. The frontend SHALL prioritize Tournament Identity, Registration Window, Current Status, Categories Next Step, and Management actions in that order.

**Independent Test**: Open detail as owner, other organizer, athlete, and unauthenticated user and verify the expected data or error.

---

### P1: Edit Tournament ⭐ MVP

**User Story**: As a tournament owner, I want to edit tournament information so that event details stay current before and after publication.

**Acceptance Criteria**:

1. WHEN a tournament owner submits valid edits for a `DRAFT`, `OPEN`, or `CLOSED` tournament THEN the system SHALL update the tournament and return `200`.
2. WHEN tournament information is updated THEN the system SHALL update `updatedAt`.
3. IF the tournament is `CANCELLED` THEN the system SHALL return `409` for edit attempts without changing data.
4. IF an authenticated organizer who does not own the tournament attempts to edit THEN the system SHALL return `404`.
5. IF an authenticated `ATHLETE` attempts to edit THEN the system SHALL return `403`.
6. IF an edit payload includes `categories`, `categoryIds`, or category configuration fields THEN the system SHALL return `422`.
7. IF invalid tournament information is submitted THEN the system SHALL return `422` without changing data.

**Independent Test**: Edit as owner and verify updated fields; repeat as other organizer, athlete, invalid payload, and cancelled tournament.

---

### P1: Publish Tournament ⭐ MVP

**User Story**: As a tournament owner, I want to publish a complete draft tournament so that it becomes open for later category and registration setup.

**Acceptance Criteria**:

1. WHEN a tournament owner publishes a valid `DRAFT` tournament THEN the system SHALL change status to `OPEN` and return `200`.
2. WHEN a tournament is published THEN the system SHALL NOT create tournament categories.
3. IF a tournament owner publishes an `OPEN` tournament THEN the system SHALL return `409` without changing data.
4. IF a tournament owner publishes a `CLOSED` or `CANCELLED` tournament THEN the system SHALL return `409` without changing data.
5. IF a tournament has invalid required information at publish time THEN the system SHALL return `422` without changing status.
6. IF an authenticated organizer who does not own the tournament attempts to publish THEN the system SHALL return `404`.

**Independent Test**: Create a draft, publish it, verify `OPEN` status and category count remains zero.

---

### P1: Manage Tournament Status ⭐ MVP

**User Story**: As a tournament owner, I want to close, reopen, or cancel a tournament so that the tournament lifecycle matches operational reality.

**Acceptance Criteria**:

1. WHEN a tournament owner closes an `OPEN` tournament THEN the system SHALL change status to `CLOSED` and return `200`.
2. WHEN a tournament owner reopens a `CLOSED` tournament THEN the system SHALL change status to `OPEN` and return `200`.
3. WHEN a tournament owner cancels a `DRAFT`, `OPEN`, or `CLOSED` tournament THEN the system SHALL change status to `CANCELLED` and return `200`.
4. IF a tournament owner closes a `DRAFT` or `CANCELLED` tournament THEN the system SHALL return `409` without changing status.
5. IF a tournament owner reopens a `DRAFT`, `OPEN`, or `CANCELLED` tournament THEN the system SHALL return `409` without changing status.
6. IF a tournament owner cancels a `CANCELLED` tournament THEN the system SHALL return `409` without changing status.
7. IF an authenticated organizer who does not own the tournament attempts a status action THEN the system SHALL return `404`.

**Independent Test**: Publish, close, reopen, cancel, and verify invalid transitions return `409`.

---

### P1: Use Tournament Management Screens ⭐ MVP

**User Story**: As an `ORGANIZER`, I want tournament screens inside Squadra so that I can create, edit, publish, and manage tournaments without leaving the authenticated app.

**Acceptance Criteria**:

1. WHEN an authenticated `ORGANIZER` opens `/tournaments` THEN the frontend SHALL show owned tournaments or an empty-state create prompt.
2. WHEN a tournament is created successfully from `/tournaments` THEN the frontend SHALL add it to the visible list without a full page reload.
3. WHEN an authenticated `ORGANIZER` opens `/tournaments/{tournamentId}` THEN the frontend SHALL show editable tournament information and status actions allowed by the current status.
4. WHEN the tournament status is `DRAFT` THEN the frontend SHALL show a publish action.
5. WHEN the tournament status is `OPEN` THEN the frontend SHALL show close and cancel actions.
6. WHEN the tournament status is `CLOSED` THEN the frontend SHALL show reopen and cancel actions.
7. WHEN the tournament status is `CANCELLED` THEN the frontend SHALL show read-only tournament information and no mutating actions.
8. The frontend SHALL show that categories are a separate next step after tournament creation and SHALL NOT include category fields in tournament forms.
9. IF load or save fails after one refresh-token recovery attempt THEN the frontend SHALL show a non-sensitive error and preserve typed values.
10. WHILE the user is authenticated as `ORGANIZER` THEN the sidebar SHALL link Tournaments to `/tournaments`.

**Independent Test**: Render list/detail as empty owner, owner with draft/open/closed/cancelled tournaments, failed save, and expired-token recovery.

---

### P1: Authorization, Privacy, and Observability ⭐ MVP

**User Story**: As Squadra, I want tournament management to be private and traceable so that tournament data is not leaked or mutated by unauthorized actors.

**Acceptance Criteria**:

1. IF a request is unauthenticated THEN the system SHALL return `401`.
2. IF an authenticated user has global role `ATHLETE` THEN the system SHALL return `403` for all tournament management endpoints.
3. IF an authenticated `ORGANIZER` does not own a tournament THEN the system SHALL return `404` for detail, edit, publish, and status actions.
4. IF a syntactically valid request violates validation or category-boundary rules THEN the system SHALL return `422`.
5. IF a status transition conflicts with the current state THEN the system SHALL return `409`.
6. WHEN a tournament is created, edited, published, closed, reopened, or cancelled THEN the system SHALL emit an application log entry without tokens, passwords, or financial data.

**Independent Test**: Exercise all endpoints for unauthenticated, athlete, owner, non-owner, invalid, and conflicting requests; assert safe status codes and no sensitive fields in responses.

---

## Edge Cases

- IF two create requests from the same organizer run concurrently THEN the system SHALL create two separate `DRAFT` tournaments owned by that organizer.
- IF two status transitions race for the same tournament THEN the system SHALL persist one valid final status and SHALL NOT skip through an invalid transition.
- IF an organizer account is deleted THEN the database SHALL remove or reject orphaned owned tournaments according to referential integrity.
- IF date fields are submitted with malformed ISO dates THEN the system SHALL return `422`.
- IF a tournament has zero categories THEN publish SHALL still be allowed because categories are created after the tournament.
- IF category count is zero THEN the frontend SHALL present categories as the next setup step, not as missing tournament data.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| TOUR-01 | P1: Create Tournament | Verified | Done |
| TOUR-02 | P1: Validate Tournament Information | Verified | Done |
| TOUR-03 | P1: List My Tournaments | Verified | Done |
| TOUR-04 | P1: View Tournament Detail | Verified | Done |
| TOUR-05 | P1: Edit Tournament | Verified | Done |
| TOUR-06 | P1: Publish Tournament | Verified | Done |
| TOUR-07 | P1: Manage Tournament Status | Verified | Done |
| TOUR-08 | P1: Use Tournament Management Screens | Verified | Done |
| TOUR-09 | P1: Authorization, Privacy, and Observability | Verified | Done |
| TOUR-10 | Edge Cases | Verified | Done |

**Coverage:** 10 total, 10 mapped to implementation/tests, 0 unmapped.

---

## Success Criteria

- [ ] Organizers can create draft tournaments without creating categories.
- [ ] Organizers can list and manage only tournaments they own.
- [ ] Tournament owners can edit `DRAFT`, `OPEN`, and `CLOSED` tournaments.
- [ ] Tournament owners can publish `DRAFT` tournaments to `OPEN`.
- [ ] Tournament owners can close, reopen, and cancel according to allowed status transitions.
- [ ] Category fields are rejected from tournament payloads and omitted from tournament forms.
- [ ] Backend tests prove validation, ownership, category boundary, privacy, status transitions, and logging trigger points.
- [ ] Frontend tests prove list/detail routes, create/edit, status-specific controls, failure states, auth recovery, and mobile-ready UI.
- [ ] Backend coverage remains at or above 80%.
- [ ] Frontend checks pass.
