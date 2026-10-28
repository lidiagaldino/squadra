# Tournament Categories Specification

## Problem Statement

Organizers need to configure tournament categories as independent competitive divisions after a tournament exists. Each category must define its own modality, roster limits, team limits, payment model, price, competition format, and scheduling settings so that registrations, payments, competition generation, and schedules can later operate from category-owned rules instead of tournament-wide defaults.

## Goals

- [ ] Tournament owners can create categories under tournaments they own.
- [ ] Each category independently stores modality, roster limits, team limits, payment model, price, competition format, and scheduling settings.
- [ ] Tournament owners can list, view, edit, and remove configurable categories before downstream registrations depend on them.
- [ ] Tournament summaries/details return a real category count.
- [ ] Organizer tournament detail UI replaces placeholder categories with API-backed category management in Brazilian Portuguese.
- [ ] Backend remains the source of truth for validation, price, ownership, and lifecycle constraints.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Team registration into categories | Registration requires team/category eligibility, roster invitations, and payment state. |
| Payment charge creation, webhook handling, refunds | Categories define payment model and price only; official payment execution is a separate feature. |
| Draw generation, brackets, standings, Swiss pairing | This feature captures the selected competition format but does not generate competition artifacts. |
| Schedule generation or publication | Categories define scheduling settings only; generating matches/slots is a scheduling-engine feature. |
| Category-specific regulations document/versioning | A later regulation/audit feature should version official rule changes. |
| Public category pages for athletes | MVP category management is an authenticated organizer workflow. |
| Editing categories with approved registrations | Registrations do not exist yet; future registration work must add stricter mutation locks. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Feature location | Use `specs/active/tournament-categories` | Tournament Management explicitly kept category CRUD out of scope. | n |
| API shape | Use nested routes under `/api/v1/tournaments/{tournamentId}/categories` | Categories are owned by tournaments and authorization is contextual. | n |
| Category mutation statuses | Allow create, update, and delete while tournament status is `DRAFT` or `OPEN`; block `CLOSED` and `CANCELLED` with `409` | Published tournaments may still be configured for registrations, but closed/cancelled tournaments should be stable. | n |
| Delete behavior | Delete is allowed only before registrations exist; MVP has no registrations, so repository delete may proceed for configurable categories | Future registration tables must restrict delete through foreign keys and service checks. | n |
| Payment model values | Use `TEAM` and `ATHLETE` only | Payments docs state each category uses exactly one of these models in MVP. | n |
| Free category representation | Use selected payment model with `priceInCents = 0` | Avoids inventing a third payment model not present in payment rules. | n |
| Price storage | Store price as integer cents in BRL | Database and payments guidance forbid floating-point money. | n |
| Modality values | Use `FIELD_SOCCER`, `FUTSAL`, and `SOCIETY` | These cover the current women's football tournament modalities used by Squadra. | n |
| Roster limits | Capture `minAthletes` and `maxAthletes` per team registration | Docs define roster limits at category level; staff limits are future scope. | n |
| Team limits | Capture `minTeams` and `maxTeams` per category | Competition and registration readiness depend on category-owned team ranges. | n |
| Competition format values | Use `ROUND_ROBIN`, `GROUP_STAGE`, `KNOCKOUT`, `GROUPS_PLUS_CUPS`, and `SWISS` | Matches Competition Engine docs. | n |
| Swiss rounds | Require `swissRounds` only when format is `SWISS`; otherwise omit/null | Swiss format needs a configured number of rounds before draw generation. | n |
| Scheduling settings | Capture `matchDurationMinutes`, `intervalBetweenMatchesMinutes`, `minimumRestMinutes`, and scheduling `strategy` | Matches Scheduling Engine inputs and strategies without generating a schedule. | n |
| Scheduling strategy values | Use `FASTEST_COMPLETION`, `BALANCED_REST`, `BALANCED_COURTS`, `HYBRID`, and `MANUAL` | Matches Scheduling Engine docs. | n |
| Category name uniqueness | Require unique normalized category names per tournament | Avoids ambiguous registration/payment category selection. | n |
| Unknown request fields | Unknown category payload fields return `422` | Matches existing tournament and team API strictness. | n |

**Open questions:** none.

---

## User Stories

### P1: Create Tournament Category ⭐ MVP

**User Story**: As an `ORGANIZER`, I want to create categories inside my tournament so that each division can have its own rules before teams register.

**Acceptance Criteria**:

1. WHEN an authenticated owner organizer submits a valid category for a `DRAFT` or `OPEN` tournament THEN the system SHALL persist the category and return `201`.
2. WHEN a category is created THEN the system SHALL associate it with the requested tournament and not with any other tournament.
3. WHEN a category is created THEN the system SHALL assign `createdAt` and `updatedAt` timestamps from the backend clock.
4. IF the tournament is `CLOSED` or `CANCELLED` THEN the system SHALL return `409` without writing a category.
5. IF the authenticated organizer does not own the tournament THEN the system SHALL return `404` without exposing that the tournament exists.
6. IF the authenticated user is not an `ORGANIZER` THEN the system SHALL return `403`.
7. IF the request is unauthenticated THEN the system SHALL return `401`.

**Independent Test**: Create a tournament as an organizer, publish it, create one valid category, and verify the response, timestamps, tournament association, and category count.

### P1: Validate Category Configuration ⭐ MVP

**User Story**: As an organizer, I want category configuration validated so that invalid roster, payment, competition, or scheduling rules cannot enter the tournament setup.

**Acceptance Criteria**:

1. IF `name` is blank, shorter than 3 characters, or longer than 80 characters THEN the system SHALL return `422`.
2. IF another category in the same tournament already has the same normalized `name` THEN the system SHALL return `409`.
3. IF `modality` is not `FIELD_SOCCER`, `FUTSAL`, or `SOCIETY` THEN the system SHALL return `422`.
4. IF `minAthletes` is less than 1 THEN the system SHALL return `422`.
5. IF `maxAthletes` is less than `minAthletes` THEN the system SHALL return `422`.
6. IF `maxAthletes` is greater than 99 THEN the system SHALL return `422`.
7. IF `minTeams` is less than 2 THEN the system SHALL return `422`.
8. IF `maxTeams` is less than `minTeams` THEN the system SHALL return `422`.
9. IF `maxTeams` is greater than 512 THEN the system SHALL return `422`.
10. IF `paymentModel` is not `TEAM` or `ATHLETE` THEN the system SHALL return `422`.
11. IF `priceInCents` is negative THEN the system SHALL return `422`.
12. IF `competitionFormat` is not one of the supported competition formats THEN the system SHALL return `422`.
13. IF `competitionFormat` is `SWISS` and `swissRounds` is missing, less than 1, or greater than 20 THEN the system SHALL return `422`.
14. IF `competitionFormat` is not `SWISS` and `swissRounds` is supplied THEN the system SHALL return `422`.
15. IF any scheduling duration is less than zero THEN the system SHALL return `422`.
16. IF `matchDurationMinutes` is less than 10 or greater than 240 THEN the system SHALL return `422`.
17. IF `schedulingStrategy` is not a supported scheduling strategy THEN the system SHALL return `422`.
18. IF the payload includes unknown fields THEN the system SHALL return `422`.

**Independent Test**: Submit invalid values for each category field independently and verify no category is persisted.

### P1: List and View Tournament Categories ⭐ MVP

**User Story**: As a tournament owner, I want to see configured categories so that I know what divisions are ready for registration.

**Acceptance Criteria**:

1. WHEN a tournament owner requests the category list THEN the system SHALL return categories for that tournament ordered by `createdAt` ascending.
2. WHEN a tournament owner requests an existing category detail THEN the system SHALL return every independently configured category field.
3. WHEN a tournament has no categories THEN the system SHALL return `200` with an empty list.
4. IF a category ID belongs to another tournament THEN the system SHALL return `404`.
5. IF a non-owner organizer requests category list or detail THEN the system SHALL return `404`.
6. IF an authenticated `ATHLETE` requests category list or detail THEN the system SHALL return `403`.

**Independent Test**: Create categories for two tournaments and verify each list/detail response is scoped to the owning tournament and organizer.

### P1: Edit Tournament Category ⭐ MVP

**User Story**: As a tournament owner, I want to edit a configurable category so that I can correct category rules before registration depends on them.

**Acceptance Criteria**:

1. WHEN a tournament owner submits valid category edits for a `DRAFT` or `OPEN` tournament THEN the system SHALL replace the configurable fields and return `200`.
2. WHEN a category is updated THEN the system SHALL preserve `id`, `tournamentId`, and `createdAt`.
3. WHEN a category is updated THEN the system SHALL update `updatedAt`.
4. IF the target category does not belong to the route tournament THEN the system SHALL return `404`.
5. IF the edit would duplicate another category name in the same tournament THEN the system SHALL return `409`.
6. IF the tournament is `CLOSED` or `CANCELLED` THEN the system SHALL return `409` without changing the category.
7. IF invalid category configuration is submitted THEN the system SHALL return `422` without changing the category.

**Independent Test**: Edit a category name, limits, payment, format, and scheduling settings, then verify the changed fields and unchanged identity fields.

### P1: Delete Tournament Category ⭐ MVP

**User Story**: As a tournament owner, I want to remove a configurable category so that abandoned divisions do not appear in tournament setup.

**Acceptance Criteria**:

1. WHEN a tournament owner deletes a category from a `DRAFT` or `OPEN` tournament THEN the system SHALL delete the category and return `204`.
2. WHEN a category is deleted THEN the system SHALL reduce the tournament category count.
3. IF the target category does not belong to the route tournament THEN the system SHALL return `404`.
4. IF the tournament is `CLOSED` or `CANCELLED` THEN the system SHALL return `409` without deleting the category.
5. IF future registrations reference the category THEN the system SHALL return `409` without deleting the category.

**Independent Test**: Delete one of two categories and verify it is gone, the remaining category is unchanged, and the tournament count is decremented.

### P1: Show Real Category Counts ⭐ MVP

**User Story**: As an organizer, I want tournament list and detail counts to reflect real categories so that setup progress is accurate.

**Acceptance Criteria**:

1. WHEN a tournament has categories THEN the tournament list response SHALL include the real category count.
2. WHEN a tournament has categories THEN the tournament detail response SHALL include the real category count.
3. WHEN a category is created or deleted THEN subsequent tournament list and detail responses SHALL reflect the new count.
4. IF a tournament has no categories THEN the category count SHALL be `0`.

**Independent Test**: Create and delete categories, then fetch list and detail responses and verify counts.

### P1: Manage Categories in the Frontend ⭐ MVP

**User Story**: As an organizer, I want category management on the tournament detail screen so that I can configure divisions without leaving the tournament workspace.

**Acceptance Criteria**:

1. WHEN the tournament detail screen loads THEN the frontend SHALL fetch and render API-backed categories instead of hardcoded placeholder category cards.
2. WHEN there are no categories THEN the frontend SHALL show a Portuguese empty state with a primary action to create a category.
3. WHEN the tournament is `DRAFT` or `OPEN` THEN the frontend SHALL allow category create, edit, and delete actions.
4. WHEN the tournament is `CLOSED` or `CANCELLED` THEN the frontend SHALL render categories read-only.
5. WHEN the organizer saves a category THEN the frontend SHALL send all required fields, preserve typed values on error, and refresh the tournament category count after success.
6. WHEN the frontend displays price THEN it SHALL format `priceInCents` as Brazilian Real.
7. WHEN the frontend displays enum values THEN it SHALL show Brazilian Portuguese labels.
8. IF category load or save fails after one refresh-token recovery attempt THEN the frontend SHALL show a non-sensitive Portuguese error message.

**Independent Test**: Render the tournament detail page with zero categories, editable categories, read-only statuses, failed saves, and successful mutations.

### P1: Authorization, Privacy, and Observability ⭐ MVP

**User Story**: As Squadra, I want category management to be private, safe, and observable so that configuration changes are traceable without exposing sensitive data.

**Acceptance Criteria**:

1. IF a category endpoint receives an unauthenticated request THEN the system SHALL return `401`.
2. IF a category endpoint receives an authenticated non-organizer request THEN the system SHALL return `403`.
3. IF an organizer does not own the parent tournament THEN the system SHALL return `404`.
4. IF category configuration violates business validation THEN the system SHALL return `422`.
5. IF category mutation violates tournament state or delete restrictions THEN the system SHALL return `409`.
6. WHEN a category is created, edited, or deleted THEN the system SHALL emit a safe application log with action, owner user ID, tournament ID, and category ID.
7. The system SHALL NOT log access tokens, authorization headers, payment provider data, or raw financial instrument data during category operations.

**Independent Test**: Exercise all category endpoints as unauthenticated, athlete, owner, and non-owner users and verify status codes, response privacy, and safe log behavior.

---

## Edge Cases

| Edge Case | Expected Handling |
| --- | --- |
| Same category name with different casing or extra spaces | Normalize and reject as duplicate within the tournament. |
| Price is zero | Accept as a free category while keeping the selected `TEAM` or `ATHLETE` model. |
| Swiss format without rounds | Reject with `422`. |
| Non-Swiss format with Swiss rounds | Reject with `422`. |
| Category ID valid UUID but under another tournament | Return `404`. |
| Tournament is cancelled after the category form loads | Save/delete returns `409`, and UI refreshes current tournament state. |
| Category list contains many categories | MVP returns an ordered list; pagination is future scope because expected category count per tournament is small. |

## Requirement Traceability

| ID | Requirement | User Story | Priority | Status |
| --- | --- | --- | --- | --- |
| TCAT-01 | Create categories under owned draft/open tournaments | Create Tournament Category | P1 | verified |
| TCAT-02 | Validate category configuration and supported enums | Validate Category Configuration | P1 | verified |
| TCAT-03 | List and view categories scoped to tournament ownership | List and View Tournament Categories | P1 | verified |
| TCAT-04 | Edit configurable category fields | Edit Tournament Category | P1 | verified |
| TCAT-05 | Delete configurable categories before downstream dependencies | Delete Tournament Category | P1 | verified |
| TCAT-06 | Return real tournament category counts | Show Real Category Counts | P1 | verified |
| TCAT-07 | Provide organizer UI for category management | Manage Categories in the Frontend | P1 | verified |
| TCAT-08 | Enforce authorization, privacy, error mapping, and safe observability | Authorization, Privacy, and Observability | P1 | verified |
