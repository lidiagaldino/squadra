# Team Registration Specification

## Problem Statement

Teams need a first registration step that places a permanent team into a specific tournament category. The platform must enforce the existing `Team + Tournament + Category` registration invariant, category capacity, team administration authorization, and private tournament scoping before later roster, payment, and organizer review flows build on top of the registration.

## Goals

- [ ] `TEAM_OWNER` and `TEAM_ADMIN` users can register their team in a specific tournament category.
- [ ] Every registration is linked to exactly one team, one tournament, and one category.
- [ ] A team cannot register more than once in the same category.
- [ ] Category team limits are enforced server-side.
- [ ] Organizers can view registrations for owned tournament categories.
- [ ] Team administrators can view and cancel their team's own registrations.
- [ ] Frontend surfaces category registration in Brazilian Portuguese without exposing raw enum statuses.
- [ ] Backend remains the source of truth for authorization, category capacity, lifecycle status, and privacy.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Registration roster invitations and athlete acceptance | Existing docs define this as the next registration step; this slice establishes the registration container first. |
| Athlete-per-category conflict validation | Requires `RegistrationRoster` and confirmed athlete participation, not just team submission. |
| Payment creation, payment status, webhooks, refunds | Payments depend on roster/payment model flows and gateway integration. |
| Organizer approval/rejection | Review depends on roster and payment readiness; this slice only creates/cancels/list registrations. |
| Competition draw, standings, scheduling eligibility | Only `APPROVED` registrations enter competition, and approval is later scope. |
| Public tournament registration pages | MVP tournament/category/registration access remains authenticated and private. |
| Email/notification delivery | No notification infrastructure is present for this feature yet. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Feature location | Use `specs/active/team-registration` | Registration is separate from team management and category setup. | n |
| Initial registration status | Create registrations as `DRAFT` | Docs list `DRAFT` as the first registration state before athletes, payment, and review. | n |
| Registration initiation | Allow `TEAM_OWNER` and `TEAM_ADMIN` to initiate registration | Roles docs authorize these permanent team administrators to initiate inscriptions. | n |
| Global role requirement | Allow authenticated `ATHLETE` or `ORGANIZER` users if they are team admins | Team management allows both roles to own/admin teams; contextual team role is authoritative. | n |
| Tournament status for registration | Allow registration only while tournament is `OPEN` | Publishing moves tournament to registration-ready state; `DRAFT`, `CLOSED`, and `CANCELLED` are not joinable. | n |
| Category ownership | Registration target category must belong to the route tournament | Prevents cross-tournament category injection through IDs. | n |
| Duplicate registration | Enforce one registration per `(categoryId, teamId)` | Existing domain invariant says registration represents `Team + Tournament + Category`. | n |
| Capacity count | Count non-`REJECTED` and non-`CANCELLED` registrations against `category.maxTeams` | Draft and in-progress registrations reserve a team slot until cancelled/rejected. | n |
| Cancellation | Team admins may cancel their own `DRAFT` registration | Later states will need stricter transition rules; initial slice only has `DRAFT`. | n |
| Organizer visibility | Tournament owner organizer may list category registrations | Organizers need operational visibility before later review actions exist. | n |
| Response privacy | Registration responses include team identity and status, not athlete roster or PII beyond existing team display fields | Roster/users are out of scope and privacy docs require minimizing data. | n |
| API shape | Use nested category route for create/list and team route for own registration list/cancel | Creation is category-specific; team admins also need their own registration view. | n |
| Frontend placement | Add registration controls to the tournament detail category area for eligible team admins and a registrations summary for organizers | Reuses existing tournament/category workspace and avoids a premature separate registration dashboard. | n |

**Open questions:** none.

---

## User Stories

### P1: Register Team in Category MVP

**User Story**: As a `TEAM_OWNER` or `TEAM_ADMIN`, I want to register my team in a tournament category so that my team can start the tournament participation process.

**Acceptance Criteria**:

1. WHEN an authenticated `TEAM_OWNER` or `TEAM_ADMIN` submits a valid team registration for an `OPEN` tournament category THEN the system SHALL persist a `DRAFT` registration and return `201`.
2. WHEN a registration is created THEN the system SHALL associate it with the requested `teamId`, `tournamentId`, and `categoryId`.
3. WHEN a registration is created THEN the system SHALL assign `createdAt` and `updatedAt` timestamps from the backend clock.
4. IF the tournament is not `OPEN` THEN the system SHALL return `409` without writing a registration.
5. IF the category does not belong to the route tournament THEN the system SHALL return `404`.
6. IF the authenticated user is not a `TEAM_OWNER` or `TEAM_ADMIN` for the requested team THEN the system SHALL return `403`.
7. IF the request is unauthenticated THEN the system SHALL return `401`.

**Independent Test**: Create a team, create/publish a tournament with a category, register the team in that category as the team owner, and verify `201`, `DRAFT`, IDs, and timestamps.

### P1: Enforce Registration Eligibility MVP

**User Story**: As Squadra, I want registration eligibility enforced centrally so that category capacity and duplicate registrations remain reliable under retries and concurrent attempts.

**Acceptance Criteria**:

1. IF the same team already has a non-cancelled registration in the same category THEN the system SHALL return `409`.
2. IF the category already has `maxTeams` non-cancelled registrations THEN the system SHALL return `409`.
3. IF a duplicate create request races with another duplicate create request THEN the system SHALL persist at most one registration for the same `(categoryId, teamId)`.
4. IF capacity is reached by concurrent requests THEN the system SHALL persist no more than `category.maxTeams` non-cancelled registrations.
5. The system SHALL enforce one non-cancelled registration per team/category with a database constraint or transaction-safe equivalent.
6. The system SHALL store registration status as one of `DRAFT`, `WAITING_ATHLETES`, `WAITING_PAYMENT`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, or `CANCELLED`.

**Independent Test**: Fill a category to its `maxTeams`, attempt one more registration, retry duplicate registration, and verify conflicts without extra rows.

### P1: List Category Registrations for Organizer MVP

**User Story**: As a tournament owner organizer, I want to list registrations in a category so that I can see which teams started the registration process.

**Acceptance Criteria**:

1. WHEN a tournament owner requests category registrations THEN the system SHALL return registrations scoped to that tournament category ordered by `createdAt` ascending.
2. WHEN a category has no registrations THEN the system SHALL return `200` with an empty list.
3. WHEN registration rows are returned THEN the system SHALL include registration ID, status, team ID, team name, category ID, tournament ID, `createdAt`, and `updatedAt`.
4. IF an organizer does not own the tournament THEN the system SHALL return `404`.
5. IF an authenticated non-organizer requests organizer category registration list THEN the system SHALL return `403`.

**Independent Test**: Register teams in two categories across two tournaments and verify each organizer sees only registrations for the owned route category.

### P1: List and Cancel Own Team Registrations MVP

**User Story**: As a team administrator, I want to see and cancel my team's own category registrations so that I can correct accidental submissions before later roster/payment steps.

**Acceptance Criteria**:

1. WHEN a `TEAM_OWNER` or `TEAM_ADMIN` requests team registrations THEN the system SHALL return only registrations for that team.
2. WHEN team registrations are returned THEN the system SHALL include tournament name, category name, status, team ID, tournament ID, category ID, registration ID, `createdAt`, and `updatedAt`.
3. WHEN a `TEAM_OWNER` or `TEAM_ADMIN` cancels a `DRAFT` registration for their team THEN the system SHALL update status to `CANCELLED`, update `updatedAt`, and return `200`.
4. IF a user who is not a team administrator requests or cancels team registrations THEN the system SHALL return `403`.
5. IF the target registration does not belong to the route team THEN the system SHALL return `404`.
6. IF the target registration is not cancellable THEN the system SHALL return `409` without changing it.

**Independent Test**: Register a team, list its registrations as owner/admin, cancel the draft registration, and verify the cancelled row remains visible but no longer blocks a fresh registration for that category.

### P1: Registration Frontend MVP

**User Story**: As a team administrator, I want to register from the tournament category interface so that I can choose my team and understand the next step in Portuguese.

**Acceptance Criteria**:

1. WHEN the tournament detail screen loads for an authenticated user THEN the frontend SHALL fetch the user's teams and category registrations needed to render eligible registration actions.
2. WHEN an `OPEN` tournament category is shown to a team administrator THEN the frontend SHALL provide a Portuguese team-selection control and a "Inscrever equipe" action.
3. WHEN a category is not open for registration THEN the frontend SHALL render the registration action as unavailable with Portuguese explanatory text.
4. WHEN a registration succeeds THEN the frontend SHALL show the human-readable `Rascunho` status and refresh category/team registration state.
5. WHEN a registration fails with `409` THEN the frontend SHALL preserve the selected team and show a non-sensitive Portuguese conflict message.
6. WHEN an organizer views owned category registrations THEN the frontend SHALL show a compact registration list with team names and human-readable statuses.
7. The frontend SHALL NOT display raw status enum values such as `WAITING_PAYMENT`.

**Independent Test**: Render open, closed, duplicate, successful, and organizer-list registration states and verify Portuguese labels and preserved form state.

### P1: Authorization, Privacy, and Observability MVP

**User Story**: As Squadra, I want team registration to be private, auditable, and safe so that category participation cannot be manipulated through IDs.

**Acceptance Criteria**:

1. IF a registration endpoint receives an unauthenticated request THEN the system SHALL return `401`.
2. IF a registration endpoint receives a global role that lacks the required route capability THEN the system SHALL return `403`.
3. IF a registration endpoint receives valid IDs for resources outside the user's context THEN the system SHALL return `403` for visible teams without administration and `404` for private tournament/category resources.
4. WHEN a registration is created or cancelled THEN the system SHALL emit a safe application log with action, actor user ID, team ID, tournament ID, category ID, registration ID, and resulting status.
5. The system SHALL NOT log access tokens, authorization headers, payment provider payloads, or athlete roster details during registration operations.
6. IF request validation fails THEN the system SHALL return `422` without exposing stack traces, SQL details, or persistence internals.

**Independent Test**: Exercise registration endpoints as unauthenticated user, athlete member only, team admin, unrelated organizer, and tournament owner; verify status codes, privacy, and safe response bodies.

---

## Edge Cases

| Edge Case | Expected Handling |
| --- | --- |
| Category ID belongs to a different tournament | Return `404` and do not reveal the foreign category. |
| Team admin tries to register a team into a draft tournament | Return `409`; registration requires `OPEN`. |
| Same team retries registration after a network timeout | Unique constraint/equivalent prevents duplicate non-cancelled registrations and returns `409` if already created. |
| Category has exactly `maxTeams - 1` non-cancelled registrations | One more registration succeeds; the next returns `409`. |
| Cancelled registration exists for same team/category | Allow a new `DRAFT` registration because cancelled rows no longer reserve capacity. |
| Athlete member without team administration attempts registration | Return `403`. |
| Organizer owns tournament but is not team admin | Organizer can list registrations but cannot register a team unless also a team admin. |
| Unknown request fields | Return `422`; do not silently persist unsupported client data. |

## Requirement Traceability

| ID | Requirement | User Story | Priority | Status |
| --- | --- | --- | --- | --- |
| TREG-01 | Create a team/category/tournament registration in `DRAFT` | Register Team in Category | P1 | pending |
| TREG-02 | Enforce duplicate, capacity, and status eligibility | Enforce Registration Eligibility | P1 | pending |
| TREG-03 | Provide organizer category registration list | List Category Registrations for Organizer | P1 | pending |
| TREG-04 | Provide team registration list and draft cancellation | List and Cancel Own Team Registrations | P1 | pending |
| TREG-05 | Provide frontend registration and organizer visibility in pt-BR | Registration Frontend | P1 | pending |
| TREG-06 | Enforce authorization, privacy, validation, and safe logs | Authorization, Privacy, and Observability | P1 | pending |

**Coverage:** 6 total, 6 mapped to tasks, 0 unmapped.

## Success Criteria

- [ ] A team administrator can register a team into a specific open tournament category.
- [ ] Duplicate and over-capacity registrations are rejected server-side.
- [ ] Tournament owners can see which teams started registration in each category.
- [ ] Team admins can see and cancel their own draft registrations.
- [ ] Frontend shows registration state and actions in Brazilian Portuguese.
- [ ] Backend and frontend quality gates pass with coverage thresholds intact.
