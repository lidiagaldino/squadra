# Tournament Management Tasks

## Execution Protocol

Implement these tasks with the `tlc-spec-driven` skill and the Squadra Specialist Agent after explicit approval. This file is specification-phase output. Do not implement production code while status is `Draft`.

**Design**: `specs/active/tournament-management/design.md`
**Status**: Implemented

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec. Guidelines found: `AGENTS.md`, `.agents/agents/squadra-specialist-agent.md`, `docs/tournaments-and-registrations.md`, `docs/security-business-constraints.md`, backend `pom.xml`, frontend `package.json`, `frontend/vitest.config.ts`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Domain | unit | Validate tournament field bounds, date ordering, category-boundary helpers, lifecycle transitions, cancelled read-only rule, timestamps, and edge statuses from TOUR-01, TOUR-02, TOUR-05, TOUR-06, TOUR-07, TOUR-10 | `backend/src/test/java/com/squadra/platform/domain/entities/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Application service | unit | Cover create owner assignment, owner-only list/detail/edit/status actions, non-owner privacy, lifecycle conflicts, category payload rejection, and safe logging triggers | `backend/src/test/java/com/squadra/platform/application/services/impl/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| API/resource | integration | Cover every tournament route: `201`, `200`, `401`, `403`, `404`, `409`, `422`, unknown fields, category-field rejection, and no private data exposure | `backend/src/test/java/com/squadra/platform/api/v1/tournaments/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Repository/migration | integration through API/service | Cover create/list/find/save, owner scoping, lifecycle persistence, constraints, indexes, timestamp columns, and category count placeholder | `backend/src/test/java/com/squadra/platform/api/v1/tournaments/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Frontend API/form/page | unit/component | Cover list/create/detail/edit/status actions, no category fields, status-specific controls, read-only cancelled state, refresh retry, error preservation, and sidebar link | `frontend/src/features/tournaments/*.test.tsx`, `frontend/src/app/tournaments/**/*.test.tsx`, `frontend/src/app/home/page.test.tsx` | `npm run check` |
| Architecture/security | architecture/integration | Preserve layer placement, organizer-only routes, private non-owner `404`, no stale package violations, and no sensitive logs/responses | `backend/src/test/java/com/squadra/platform/architecture/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick backend | Domain/application-only backend tasks | `JAVA_HOME=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home PATH=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test -q` |
| Full backend | API, DB, security or final backend tasks | `JAVA_HOME=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home PATH=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify -q` |
| Frontend | Frontend API/form/page tasks | `npm run check` |
| Full feature | Final validation | backend `mvn verify`, frontend `npm run check`, `npm run build`, `docker compose --env-file .env.example -f infra/docker-compose.yml config` |

---

## Execution Plan

Phases are ordered and run sequentially.

### Phase 1: Backend Domain and Persistence

```text
T1 → T2
T1 → T3
T2 → T3
```

### Phase 2: Backend Use Case and API

```text
T3 → T4
T4 → T5
T5 → T6
```

### Phase 3: Frontend Tournament Experience

```text
T5 → T7
T7 → T8
T8 → T9
T9 → T10
```

### Phase 4: Final Validation

```text
T10 → T11
```

---

## Task Breakdown

### T1: Add tournament domain model

**Status**: Done

**What**: Create `Tournament`, `TournamentStatus`, tournament exceptions, validation logic, lifecycle transitions, and domain tests.
**Where**: `backend/src/main/java/com/squadra/platform/domain/entities/Tournament.java`
**Depends on**: None
**Reuses**: `Team` entity validation style.
**Requirement**: TOUR-01, TOUR-02, TOUR-05, TOUR-06, TOUR-07, TOUR-10

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-testing`

**Done when**:

- [ ] `Tournament.create` assigns owner and `DRAFT`.
- [ ] Validation covers all field bounds and date ordering.
- [ ] Lifecycle transitions allow publish, close, reopen, and cancel only from specified statuses.
- [ ] Cancelled tournaments reject edits.
- [ ] Domain tests cover valid, invalid, and conflicting transitions.

**Tests**: Domain unit tests.
**Gate**: Quick backend.

---

### T2: Add tournament persistence migration

**Status**: Done

**What**: Create `squadra.tournaments` with owner FK, status checks, field constraints, date checks, timestamp fields, and indexes.
**Where**: `backend/src/main/resources/db/migration/V6__create_tournaments.sql`
**Depends on**: T1
**Reuses**: Existing Flyway migration pattern.
**Requirement**: TOUR-01, TOUR-02, TOUR-03, TOUR-04, TOUR-05, TOUR-06, TOUR-07, TOUR-10

**Tools**:

- MCP: NONE
- Skill: `squadra-database`, `squadra-security`

**Done when**:

- [ ] Table stores all tournament fields and lifecycle timestamps.
- [ ] Owner FK references `squadra.users(id)`.
- [ ] Check constraints enforce supported statuses and date ordering.
- [ ] Indexes support owner list and status filtering.

**Tests**: Covered through API/repository integration in T5/T6.
**Gate**: Full backend after T6.

---

### T3: Add tournament repository implementation

**Status**: Done

**What**: Implement repository port and JDBC adapter for organizer-owned tournament persistence.
**Where**: `backend/src/main/java/com/squadra/platform/domain/repositories/TournamentRepository.java`
**Depends on**: T1, T2
**Reuses**: `ReactiveJdbcSupport` and `JdbcTeamRepository` mapping patterns.
**Requirement**: TOUR-01, TOUR-03, TOUR-04, TOUR-05, TOUR-06, TOUR-07, TOUR-10

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-database`, `squadra-testing`

**Done when**:

- [ ] Repository creates tournaments.
- [ ] Repository lists only tournaments for the owner organizer.
- [ ] Repository finds details only when owned by the caller.
- [ ] Repository saves edits/status changes and returns `categoryCount`.
- [ ] No request flow calls `.await().indefinitely()`.

**Tests**: Covered through service/API tests.
**Gate**: Quick backend compile/test after T4.

---

### T4: Add tournament application service

**Status**: Done

**What**: Implement tournament use cases, ownership privacy, lifecycle orchestration, category-boundary validation, and safe lifecycle logging.
**Where**: `backend/src/main/java/com/squadra/platform/application/services/interfaces/TournamentService.java`
**Depends on**: T3
**Reuses**: `DefaultTeamService` ownership/error patterns.
**Requirement**: TOUR-01 through TOUR-10

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-security`, `squadra-testing`, `squadra-observability`

**Done when**:

- [ ] Create/list/detail/edit/status methods exist and return `Uni`.
- [ ] Owner ID is always derived from authenticated user ID.
- [ ] Non-owner organizers receive not-found behavior through repository visibility.
- [ ] Category-like payloads and unknown fields are rejected with `422`.
- [ ] Invalid lifecycle transitions return conflict.
- [ ] Service tests cover ownership, validation, category boundary, status conflicts, cancelled edit, and logging trigger paths.

**Tests**: Application service unit tests.
**Gate**: Quick backend.

---

### T5: Add tournament API contract

**Status**: Done

**What**: Add request/response DTOs, REST resource, and exception mappers for tournament management.
**Where**: `backend/src/main/java/com/squadra/platform/api/v1/tournaments/TournamentsResource.java`
**Depends on**: T4
**Reuses**: Team API route and mapper conventions.
**Requirement**: TOUR-01 through TOUR-10

**Tools**:

- MCP: NONE
- Skill: `squadra-api-contract`, `squadra-security`, `squadra-testing`

**Done when**:

- [ ] `POST /api/v1/tournaments` returns `201`.
- [ ] `GET /api/v1/tournaments` returns owned tournaments only.
- [ ] `GET`/`PUT /api/v1/tournaments/{tournamentId}` enforce owner visibility.
- [ ] Publish/close/reopen/cancel routes enforce lifecycle transitions.
- [ ] `401`, `403`, `404`, `409`, `422`, unknown fields, category-field rejection, and no private data exposure are tested.

**Tests**: Backend API integration tests.
**Gate**: Full backend.

---

### T6: Update backend validation, architecture and coverage gates

**Status**: Done

**What**: Extend architecture/security validation where needed and keep backend coverage >= 80%.
**Where**: `backend/src/test/java/com/squadra/platform/architecture/LayerPlacementTest.java`
**Depends on**: T5
**Reuses**: Existing architecture placement test and coverage gate.
**Requirement**: TOUR-01 through TOUR-10

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-security`, `squadra-testing`

**Done when**:

- [ ] No production classes are loose under layer roots.
- [ ] Full backend gate passes with coverage >= 80%.
- [ ] API responses do not include sensitive owner-adjacent data beyond IDs needed for management.

**Tests**: Architecture and full backend tests.
**Gate**: Full backend.

---

### T7: Add tournaments frontend API client

**Status**: Done

**What**: Implement typed frontend client for list/create/detail/edit/publish/close/reopen/cancel with one refresh retry.
**Where**: `frontend/src/features/tournaments/api.ts`
**Depends on**: T5
**Reuses**: Team frontend API client pattern.
**Requirement**: TOUR-01 through TOUR-09

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] API client exposes every tournament management operation.
- [ ] `401` triggers one token refresh and one retry.
- [ ] Payloads never include category fields.
- [ ] API tests cover success, failed recovery, and safe errors.

**Tests**: Frontend API unit tests.
**Gate**: Frontend.

---

### T8: Add tournament list route

**Status**: Done

**What**: Build `/tournaments` with organizer sidebar, tournament list, empty state, and create form.
**Where**: `frontend/src/app/tournaments/page.tsx`
**Depends on**: T7
**Reuses**: Team list screen and authenticated shell patterns.
**Requirement**: TOUR-03, TOUR-08, TOUR-09

**Tools**:

- MCP: Stitch for UI exploration
- Skill: `squadra-design`, `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] `/tournaments` shows owned tournaments or empty-state create prompt.
- [ ] Successful create adds the tournament to the visible list without reload.
- [ ] Tournament cards show status, dates, location, and category count.
- [ ] No category fields appear in the create form.
- [ ] Tests cover empty, create, load failure, and auth recovery states.

**Tests**: Frontend component/page tests.
**Gate**: Frontend.

---

### T9: Add tournament detail route

**Status**: Done

**What**: Build `/tournaments/{tournamentId}` with editable identity/date/rules fields, category next-step section, and status-specific actions.
**Where**: `frontend/src/app/tournaments/[tournamentId]/page.tsx`
**Depends on**: T8
**Reuses**: Team detail screen and organizer profile form patterns.
**Requirement**: TOUR-04, TOUR-05, TOUR-06, TOUR-07, TOUR-08, TOUR-09

**Tools**:

- MCP: Stitch for UI exploration
- Skill: `squadra-design`, `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] Detail shows tournament identity, registration window, current status, category next step, and management actions.
- [ ] Draft shows publish/cancel controls.
- [ ] Open shows close/cancel controls.
- [ ] Closed shows reopen/cancel controls.
- [ ] Cancelled is read-only with no mutating controls.
- [ ] No category fields appear in the edit form.
- [ ] Tests cover draft/open/closed/cancelled controls, save failure, and typed value preservation.

**Tests**: Frontend component/page tests.
**Gate**: Frontend.

---

### T10: Wire tournament navigation

**Status**: Done

**What**: Update authenticated organizer sidebars so Tournaments navigates to `/tournaments`.
**Where**: `frontend/src/app/home/page.tsx`
**Depends on**: T9
**Reuses**: Existing role-aware sidebar links.
**Requirement**: TOUR-08

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] Home organizer sidebar includes a Tournaments link to `/tournaments`.
- [ ] Organizer profile sidebar includes a Tournaments link to `/tournaments`.
- [ ] Tournament screens keep Tournaments as the active sidebar item.
- [ ] Page tests cover the updated navigation.

**Tests**: Frontend page tests.
**Gate**: Frontend.

---

### T11: Final feature validation and report

**Status**: Done

**What**: Run full relevant validation, write validation evidence, and update requirement traceability.
**Where**: `specs/active/tournament-management/validation.md`
**Depends on**: T10
**Reuses**: Team Management validation report style.
**Requirement**: TOUR-01 through TOUR-10

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`, `squadra-testing`, `squadra-architecture`, `squadra-security`, `squadra-api-contract`, `squadra-database`, `squadra-observability`

**Done when**:

- [ ] Backend full validation passes or any environment limitation is documented.
- [ ] Frontend `npm run check` and `npm run build` pass.
- [ ] `validation.md` cites file/line evidence for every requirement.
- [ ] Spec traceability status is updated to Verified.
- [ ] Final validation confirms TOUR-01 through TOUR-10 map to implementation and tests.

**Tests**: Full feature validation.
**Gate**: Full feature.

---

## Pre-Approval Validation

### Diagram-Definition Cross-Check

| Edge | Matching `Depends on` | Result |
| --- | --- | --- |
| T1 → T2 | T2 depends on T1 | Pass |
| T1 → T3 | T3 depends on T1 | Pass |
| T2 → T3 | T3 depends on T2 | Pass |
| T3 → T4 | T4 depends on T3 | Pass |
| T4 → T5 | T5 depends on T4 | Pass |
| T5 → T6 | T6 depends on T5 | Pass |
| T5 → T7 | T7 depends on T5 | Pass |
| T7 → T8 | T8 depends on T7 | Pass |
| T8 → T9 | T9 depends on T8 | Pass |
| T9 → T10 | T10 depends on T9 | Pass |
| T10 → T11 | T11 depends on T10 | Pass |

### Test Co-location Validation

| Task | Code Layer | Required Tests | Result |
| --- | --- | --- | --- |
| T1 | Domain | Domain unit tests in same task | Pass |
| T2 | Migration | API/repository integration after API task | Pass |
| T3 | Repository | Covered through service/API tests | Pass |
| T4 | Application service | Service unit tests in same task | Pass |
| T5 | API/resource | API integration tests in same task | Pass |
| T6 | Architecture/security | Architecture/full backend tests in same task | Pass |
| T7 | Frontend API | API unit tests in same task | Pass |
| T8 | Frontend list route | Component/page tests in same task | Pass |
| T9 | Frontend detail route | Component/page tests in same task | Pass |
| T10 | Frontend navigation | Page tests in same task | Pass |
| T11 | Validation report | Full feature gate | Pass |
