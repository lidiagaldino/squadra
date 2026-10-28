# Team Registration Tasks

## Execution Protocol

Implement these tasks with the `tlc-spec-driven` skill and the Squadra Specialist Agent after explicit approval. This file is specification-phase output. Do not implement production code while status is `Draft`.

**Spec**: `specs/active/team-registration/spec.md`
**Design**: `specs/active/team-registration/design.md`
**Status**: Implemented

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec. Guidelines found: `AGENTS.md`, `.agents/agents/squadra-specialist-agent.md`, `.agents/skills/squadra-testing/SKILL.md`, `docs/business-rules.md`, `docs/tournaments-and-registrations.md`, `docs/security-business-constraints.md`, backend `pom.xml`, frontend `package.json`, `frontend/vitest.config.ts`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Domain | unit | Cover registration creation, required IDs, initial `DRAFT`, cancellation transition, invalid cancellation, documented statuses, and timestamp behavior for TREG-01, TREG-02, TREG-04 | `backend/src/test/java/com/squadra/platform/domain/entities/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Application service | unit | Cover team-admin authorization, tournament `OPEN` requirement, category scoping, duplicate checks, max team capacity, organizer list privacy, team list/cancel privacy, and safe logging triggers for TREG-01 through TREG-06 | `backend/src/test/java/com/squadra/platform/application/services/impl/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| API/resource | integration | Cover every registration route, `201`, `200`, `401`, `403`, `404`, `409`, `422`, unknown fields, duplicate create, category full, response shape, and private resource behavior | `backend/src/test/java/com/squadra/platform/api/v1/**/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Repository/migration | integration through API/service | Cover migration applies, FK restrictions, active team/category uniqueness, list order, team/category/tournament joins, and category delete conflict after registration | `backend/src/test/java/com/squadra/platform/api/v1/**/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Frontend API/form/page | unit/component | Cover registration API clients, BFF route behavior, team selector, registration CTA, status labels, organizer registration list, conflict errors, cancellation, loading/empty/error states, and refresh retry | `frontend/src/features/**/*.test.tsx`, `frontend/src/features/**/*.test.ts`, `frontend/src/lib/**/*.test.ts` | `npm run check` |
| Architecture/security | architecture/integration | Preserve layer placement, no API to infra dependency, contextual authorization, no sensitive response fields/log inputs, and no production classes loose under layer roots | `backend/src/test/java/com/squadra/platform/architecture/*Test.java`, API tests | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick backend | Domain/application-only backend tasks | `JAVA_HOME=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home PATH=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test -q` |
| Full backend | API, DB, security, or final backend tasks | `JAVA_HOME=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home PATH=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify -q` |
| Frontend | Frontend API/form/page tasks | `npm run check` |
| Full feature | Final validation | backend `mvn verify`, frontend `npm run check`, frontend `npm run build`, TLC validators |

---

## Execution Plan

Phases are ordered and run sequentially.

### Phase 1: Backend Domain and Persistence

```text
T1 -> T2
T2 -> T3
T3 -> T4
```

### Phase 2: Backend Use Case and API

```text
T4 -> T5
T5 -> T6
T6 -> T7
```

### Phase 3: Frontend Registration Experience

```text
T7 -> T8
T8 -> T9
T9 -> T10
```

### Phase 4: Final Validation

```text
T10 -> T11
```

---

## Task Breakdown

### Phase 1: Backend Domain and Persistence

#### T1: Add registration domain model

**Status**: Complete

**What**: Create `TeamRegistration`, `RegistrationStatus`, registration exceptions, and domain tests.
**Where**: Backend domain layer
**Depends on**: None
**Reuses**: `TournamentCategory` and `Team` domain validation style.
**Requirement**: TREG-01, TREG-02, TREG-04

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-testing`

**Done when**:

- [ ] Domain creates registrations with `DRAFT` status and required IDs.
- [ ] Domain cancellation updates `DRAFT` to `CANCELLED` and refreshes `updatedAt`.
- [ ] Invalid cancellation returns a domain conflict.
- [ ] Domain status enum includes all documented registration statuses.
- [ ] Domain tests cover valid, invalid, and cancellation cases.

**Tests**: Domain unit tests.
**Gate**: Quick backend.

#### T2: Add registration persistence migration

**Status**: Complete

**What**: Add `squadra.tournament_registrations` migration with FKs, status checks, uniqueness strategy, timestamps, and indexes.
**Where**: Backend Flyway migration layer
**Depends on**: T1
**Reuses**: `V7__create_tournament_categories.sql` constraint/index style.
**Requirement**: TREG-01, TREG-02, TREG-04

**Tools**:

- MCP: NONE
- Skill: `squadra-database`, `squadra-security`, `squadra-testing`

**Done when**:

- [ ] Migration stores team, tournament, category, status, and timestamps.
- [ ] Foreign keys restrict deletion of referenced teams, tournaments, and categories.
- [ ] Database or transaction-safe equivalent enforces one active registration per team/category.
- [ ] Indexes support category list and team list access patterns.

**Tests**: Covered through repository/API integration tests.
**Gate**: Full backend after T6.

#### T3: Add registration repository

**Status**: Complete

**What**: Implement registration repository contract/adapter and details read models for category and team lists.
**Where**: Backend repository layer
**Depends on**: T2
**Reuses**: `ReactiveJdbcSupport`, `JdbcTeamRepository`, and `JdbcTournamentCategoryRepository` patterns.
**Requirement**: TREG-01, TREG-02, TREG-03, TREG-04

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-database`, `squadra-testing`

**Done when**:

- [ ] Repository creates registrations and returns joined details.
- [ ] Repository lists category registrations ordered by `createdAt` ascending.
- [ ] Repository lists team registrations with tournament/category names.
- [ ] Repository finds registrations scoped to route team.
- [ ] Repository supports active duplicate/capacity checks.
- [ ] No production code calls `.await().indefinitely()` in request flows.

**Tests**: Repository behavior covered through service/API integration tests.
**Gate**: Quick backend after T4, full backend after T6.

#### T4: Add team/category authorization support methods

**Status**: Complete

**What**: Add the minimal team/category/tournament repository support needed by the registration service without leaking infrastructure into application code.
**Where**: Backend repository contracts/adapters
**Depends on**: T3
**Reuses**: `TeamRepository.findVisibleToUser`, `TournamentRepository.findOwnedByOrganizer`, `TournamentCategoryRepository.findByTournament`.
**Requirement**: TREG-01, TREG-02, TREG-03, TREG-04, TREG-06

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-security`, `squadra-testing`

**Done when**:

- [ ] Registration service can determine whether acting user is `TEAM_OWNER` or `TEAM_ADMIN`.
- [ ] Registration service can load a route category with its tournament and limits.
- [ ] No unrelated team/tournament API response shape changes are introduced.
- [ ] Existing team/tournament/category tests still pass.

**Tests**: Application service unit tests and existing API tests.
**Gate**: Quick backend.

### Phase 2: Backend Use Case and API

#### T5: Add registration application service

**Status**: Complete

**What**: Implement create, organizer category list, team list, and draft cancellation use cases.
**Where**: Backend application service layer
**Depends on**: T4
**Reuses**: `DefaultTeamService` authorization style and `DefaultTournamentCategoryService` ownership/logging style.
**Requirement**: TREG-01 through TREG-06

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-security`, `squadra-testing`, `squadra-observability`

**Done when**:

- [ ] Service creates `DRAFT` registrations only for team admins and open tournament categories.
- [ ] Service rejects duplicate and over-capacity registration attempts.
- [ ] Service lists category registrations only for tournament owner organizers.
- [ ] Service lists and cancels team registrations only for team admins.
- [ ] Service logs create/cancel without sensitive data.
- [ ] Service tests cover all TREG requirements and failure modes.

**Tests**: Application service unit tests.
**Gate**: Quick backend.

#### T6: Add registration API contract

**Status**: Complete

**What**: Add registration resources, request/response DTOs, strict validation, and exception mapping.
**Where**: Backend API layer
**Depends on**: T5
**Reuses**: `TeamsResource`, `TournamentCategoriesResource`, and existing exception mapper conventions.
**Requirement**: TREG-01 through TREG-06

**Tools**:

- MCP: NONE
- Skill: `squadra-api-contract`, `squadra-security`, `squadra-testing`

**Done when**:

- [ ] Category registration list and create routes are exposed.
- [ ] Team registration list and cancel routes are exposed.
- [ ] Create returns `201` with a `Location` header.
- [ ] API tests cover owner/admin, athlete member only, unrelated user, non-owner organizer, invalid, duplicate, full, and cancelled cases.
- [ ] Response DTO includes only approved registration/team/category/tournament fields.

**Tests**: Backend API integration tests.
**Gate**: Full backend.

#### T7: Tighten category delete conflict behavior

**Status**: Complete

**What**: Ensure category deletion returns `409` when registrations reference the category.
**Where**: Backend category service/repository/error mapping
**Depends on**: T6
**Reuses**: Existing `TournamentCategoryConflictException` mapping.
**Requirement**: TREG-02, TREG-06

**Tools**:

- MCP: NONE
- Skill: `squadra-database`, `squadra-api-contract`, `squadra-testing`

**Done when**:

- [ ] Deleting a category with any registration returns `409`.
- [ ] Deleting an unreferenced category keeps the existing `204` behavior.
- [ ] API tests prove FK/dependency conflict does not leak SQL details.

**Tests**: Backend API integration tests.
**Gate**: Full backend.

### Phase 3: Frontend Registration Experience

#### T8: Add frontend registration API clients and BFF routes

**Status**: Complete

**What**: Add typed registration operations to tournament/team API clients and matching Next.js BFF routes.
**Where**: Frontend API/BFF layer
**Depends on**: T6
**Reuses**: Existing tournament/team BFF route and auth refresh patterns.
**Requirement**: TREG-01, TREG-03, TREG-04, TREG-05, TREG-06

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] Frontend can create category registration.
- [ ] Frontend can list category registrations.
- [ ] Frontend can list team registrations.
- [ ] Frontend can cancel team registration.
- [ ] Registration status labels are exported in Brazilian Portuguese.
- [ ] API tests cover request paths, payloads, `204`/`200` handling where relevant, refresh retry, and conflict errors.

**Tests**: Frontend API/helper tests.
**Gate**: Frontend.

#### T9: Add category registration controls to tournament detail

**Status**: Complete

**What**: Add team-selection registration UI, conflict handling, unavailable states, and successful registration refresh behavior to category cards.
**Where**: Frontend tournament detail layer
**Depends on**: T8
**Reuses**: Existing `TournamentDetailScreen`, category cards, TanStack Query, React Hook Form/Zod patterns.
**Requirement**: TREG-01, TREG-02, TREG-05

**Tools**:

- MCP: NONE
- Skill: `squadra-design`, `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] Open categories show team selector and `Inscrever equipe` action for available managed teams.
- [ ] Existing selected-team registrations show human-readable status instead of duplicate CTA.
- [ ] Closed/draft/cancelled tournaments explain why registration is unavailable.
- [ ] Conflict/error states preserve the selected team.
- [ ] Component tests cover loading, empty teams, success, duplicate conflict, category full conflict, and unavailable statuses.

**Tests**: Frontend component tests.
**Gate**: Frontend.

#### T10: Add organizer registration visibility and team cancellation UI

**Status**: Complete

**What**: Add organizer category registration list and team registration cancellation surfaces.
**Where**: Frontend tournament detail and team detail/list layer
**Depends on**: T9
**Reuses**: Existing tournament category area and team detail patterns.
**Requirement**: TREG-03, TREG-04, TREG-05, TREG-06

**Tools**:

- MCP: NONE
- Skill: `squadra-design`, `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] Organizer view shows compact category registration list/count with team names and labels.
- [ ] Team admin view shows own team registrations with tournament/category names.
- [ ] Team admin can cancel a `DRAFT` registration with a clear Portuguese action.
- [ ] Raw enum statuses never render.
- [ ] Component tests cover organizer list, empty state, cancellation success, cancellation conflict, and permission-safe errors.

**Tests**: Frontend component tests.
**Gate**: Frontend.

### Phase 4: Final Validation

#### T11: Run full validation and update TLC artifacts

**Status**: Complete

**What**: Run final gates, update requirement traceability/status, write validation report, and prepare atomic commits if requested by the user.
**Where**: Specs, validation artifacts, project quality gates
**Depends on**: T10
**Reuses**: TLC validators and existing feature validation style.
**Requirement**: TREG-01 through TREG-06

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`, `squadra-architecture`, `squadra-security`, `squadra-testing`, `squadra-api-contract`, `squadra-database`

**Done when**:

- [ ] Backend `mvn verify` passes with Java 21 and coverage threshold intact.
- [ ] Frontend `npm run check` passes.
- [ ] Frontend `npm run build` passes.
- [ ] `validate_spec.py` and `validate_tasks.py` pass for this feature.
- [ ] `validation.md` records requirement evidence and residual risks.

**Tests**: Full backend and frontend gates.
**Gate**: Full feature.

---

## Phase Execution Map

```text
Phase 1 -> Phase 2 -> Phase 3 -> Phase 4

Phase 1: T1 -> T2 -> T3 -> T4
Phase 2: T5 -> T6 -> T7
Phase 3: T8 -> T9 -> T10
Phase 4: T11
```

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | None | OK |
| T2 | T1 | T1 | OK |
| T3 | T2 | T2 | OK |
| T4 | T3 | T3 | OK |
| T5 | T4 | T4 | OK |
| T6 | T5 | T5 | OK |
| T7 | T6 | T6 | OK |
| T8 | T6 | T6 | OK |
| T9 | T8 | T8 | OK |
| T10 | T9 | T9 | OK |
| T11 | T10 | T10 | OK |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Domain | unit | Domain unit tests | OK |
| T2 | Migration | integration through API/service | Covered through repository/API integration tests | OK |
| T3 | Repository | integration through API/service | Covered through service/API integration tests | OK |
| T4 | Repository/application support | unit/integration | Application service unit tests and existing API tests | OK |
| T5 | Application service | unit | Application service unit tests | OK |
| T6 | API/resource | integration | Backend API integration tests | OK |
| T7 | API/DB conflict behavior | integration | Backend API integration tests | OK |
| T8 | Frontend API/BFF | unit | Frontend API/helper tests | OK |
| T9 | Frontend page/component | unit/component | Frontend component tests | OK |
| T10 | Frontend page/component | unit/component | Frontend component tests | OK |
| T11 | Validation artifacts | full gates | Full backend/frontend gates | OK |

## Approval Note

This plan has 11 tasks, which is more than one TLC task-budgeted batch. During Execute, offer batch sub-agents before implementation if the harness supports them; otherwise execute sequentially in the main window.
