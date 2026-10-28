# Tournament Categories Tasks

## Execution Protocol

Implement these tasks with the `tlc-spec-driven` skill and the Squadra Specialist Agent after explicit approval. This file is specification-phase output. Do not implement production code while status is `Draft`.

**Spec**: `specs/active/tournament-categories/spec.md`
**Design**: `specs/active/tournament-categories/design.md`
**Status**: Implemented

---

## Test Coverage Matrix

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Domain | unit | Validate category field bounds, enum parsing, money cents, roster/team limits, scheduling bounds, Swiss round rules, and timestamp preservation for TCAT-01 through TCAT-04 | `backend/src/test/java/com/squadra/platform/domain/entities/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Application service | unit | Cover owner-only access, tournament status mutation locks, duplicate normalized names, non-owner privacy, CRUD orchestration, and safe logging for TCAT-01 through TCAT-08 | `backend/src/test/java/com/squadra/platform/application/services/impl/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| API/resource | integration | Cover every nested category route, `201`, `200`, `204`, `401`, `403`, `404`, `409`, `422`, unknown fields, duplicate names, and response shape | `backend/src/test/java/com/squadra/platform/api/v1/tournaments/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Repository/migration | integration through API/service | Cover migration constraints, FK scoping, unique normalized names, delete behavior, list order, and real tournament category counts | `backend/src/test/java/com/squadra/platform/api/v1/tournaments/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Frontend API/form/page | unit/component | Cover category API client, list/empty states, create/edit/delete flows, read-only statuses, enum labels, BRL price formatting, and refresh retry | `frontend/src/features/tournaments/*.test.tsx` | `npm run check` |
| Architecture/security | architecture/integration | Preserve layer placement, organizer-only routes, owner-scoped category access, no sensitive response fields, and no production classes loose under layer roots | `backend/src/test/java/com/squadra/platform/architecture/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick backend | Domain/application-only backend tasks | `JAVA_HOME=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home PATH=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test -q` |
| Full backend | API, DB, security, or final backend tasks | `JAVA_HOME=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home PATH=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify -q` |
| Frontend | Frontend API/form/page tasks | `npm run check` |
| Full feature | Final validation | backend `mvn verify`, frontend `npm run check`, frontend `npm run build` |

---

## Execution Plan

Phases are ordered and run sequentially.

### Phase 1: Backend Domain and Persistence

```text
T1 -> T2
T1 -> T3
T2 -> T3
```

### Phase 2: Backend Use Case and API

```text
T4 -> T5
T5 -> T6
```

### Phase 3: Frontend Category Experience

```text
T7 -> T8
T8 -> T9
```

### Phase 4: Final Validation

```text
T10 -> T11
```

---

## Task Breakdown

### Phase 1: Backend Domain and Persistence

#### T1: Add tournament category domain model

**Status**: Done

**What**: Create the category aggregate, enums, domain exceptions, validation behavior, and domain tests.
**Where**: Backend domain layer
**Depends on**: None
**Reuses**: `Tournament` validation style and existing domain enum placement.
**Requirement**: TCAT-01, TCAT-02, TCAT-04

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-testing`

**Done when**:

- [ ] Category create/update validates modality, roster limits, team limits, payment model, price, competition format, Swiss rounds, and scheduling settings.
- [ ] Domain behavior preserves identity fields on update and refreshes `updatedAt`.
- [ ] Domain exceptions remain HTTP-free.
- [ ] Domain tests cover valid, invalid, and Swiss/non-Swiss cases.

**Tests**: Domain unit tests.
**Gate**: Quick backend.

#### T2: Add category persistence migration

**Status**: Done

**What**: Add `squadra.tournament_categories` with FK, enum checks, money cents, scheduling checks, uniqueness, timestamps, and indexes.
**Where**: Backend Flyway migration layer
**Depends on**: T1
**Reuses**: `V6__create_tournaments.sql` constraint and index style.
**Requirement**: TCAT-01, TCAT-02, TCAT-03, TCAT-05, TCAT-06

**Tools**:

- MCP: NONE
- Skill: `squadra-database`, `squadra-security`

**Done when**:

- [ ] Migration stores all category fields.
- [ ] Database enforces normalized category-name uniqueness per tournament.
- [ ] Database check constraints mirror stable category invariants.
- [ ] Indexes support category list and count access patterns.

**Tests**: Covered through repository/API integration tests.
**Gate**: Full backend after T6.

#### T3: Add category repository and real tournament counts

**Status**: Done

**What**: Implement category repository contract/adapter and update tournament repository category counts from the category table.
**Where**: Backend repository layer
**Depends on**: T1, T2
**Reuses**: `ReactiveJdbcSupport` and `JdbcTournamentRepository` mapping patterns.
**Requirement**: TCAT-01, TCAT-03, TCAT-05, TCAT-06

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-database`, `squadra-testing`

**Done when**:

- [ ] Repository creates, lists, finds, saves, and deletes categories by tournament scope.
- [ ] Duplicate name checks can exclude the current category during edit.
- [ ] Category lists are ordered by `createdAt` ascending.
- [ ] Tournament list/detail responses return real `categoryCount`.
- [ ] No production code calls `.await().indefinitely()` in request flows.

**Tests**: Repository behavior covered through service/API tests.
**Gate**: Quick backend after T4, full backend after T6.

### Phase 2: Backend Use Case and API

#### T4: Add category application service

**Status**: Done

**What**: Implement category use cases with tournament ownership, mutation status locks, duplicate-name checks, and safe logging.
**Where**: Backend application service layer
**Depends on**: T3
**Reuses**: `DefaultTournamentService` ownership and logging patterns.
**Requirement**: TCAT-01, TCAT-03, TCAT-04, TCAT-05, TCAT-08

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-security`, `squadra-testing`, `squadra-observability`

**Done when**:

- [ ] Service methods return `Uni` for create/list/detail/update/delete.
- [ ] Service verifies parent tournament ownership before category access.
- [ ] Service blocks mutations for `CLOSED` and `CANCELLED` tournaments.
- [ ] Service maps duplicate names and delete conflicts to domain/application conflicts.
- [ ] Service logs create/edit/delete without sensitive data.

**Tests**: Application service unit tests.
**Gate**: Quick backend.

#### T5: Add category API contract

**Status**: Done

**What**: Add nested category REST resource, request/response DTOs, strict validation, and exception mapping.
**Where**: Backend API layer
**Depends on**: T4
**Reuses**: `TournamentsResource` and tournament mapper conventions.
**Requirement**: TCAT-01 through TCAT-08

**Tools**:

- MCP: NONE
- Skill: `squadra-api-contract`, `squadra-security`, `squadra-testing`

**Done when**:

- [ ] Nested category routes expose create/list/detail/update/delete.
- [ ] Create returns `201` with a `Location` header.
- [ ] Delete returns `204`.
- [ ] API tests cover owner, non-owner, athlete, unauthenticated, invalid, duplicate, and conflict cases.
- [ ] Response DTO includes every independently configured category field.

**Tests**: Backend API integration tests.
**Gate**: Full backend.

#### T6: Update backend architecture and coverage gates

**Status**: Done

**What**: Extend architecture/security tests if needed and verify backend coverage remains at or above 80%.
**Where**: Backend test and quality gate layer
**Depends on**: T5
**Reuses**: Existing layer placement and Maven verification gates.
**Requirement**: TCAT-01 through TCAT-08

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-security`, `squadra-testing`

**Done when**:

- [ ] No production classes are loose under layer roots.
- [ ] API does not depend on `infra/db`.
- [ ] Application does not depend on concrete infrastructure.
- [ ] Full backend gate passes with coverage requirements intact.

**Tests**: Architecture and full backend tests.
**Gate**: Full backend.

### Phase 3: Frontend Category Experience

#### T7: Add frontend category API and formatting helpers

**Status**: Done

**What**: Add typed category API operations, enum labels, and BRL cents formatting.
**Where**: Frontend tournaments feature API layer
**Depends on**: T5
**Reuses**: Existing `tournamentsApi` refresh retry and error style.
**Requirement**: TCAT-02, TCAT-03, TCAT-06, TCAT-07

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] API client supports category list/create/detail/update/delete.
- [ ] Category input/output TypeScript types match the backend contract.
- [ ] Enum label helpers return Brazilian Portuguese labels.
- [ ] Price formatting renders BRL from integer cents.

**Tests**: Frontend API/helper tests.
**Gate**: Frontend.

#### T8: Replace placeholder category UI with real management flow

**Status**: Done

**What**: Update tournament detail to render API-backed categories, empty state, create/edit form, delete action, and read-only statuses.
**Where**: Frontend tournament detail layer
**Depends on**: T7
**Reuses**: Existing tournament detail shell, forms, React Query patterns, and Portuguese UI text.
**Requirement**: TCAT-03, TCAT-04, TCAT-05, TCAT-07

**Tools**:

- MCP: Stitch if available for layout exploration, otherwise NONE
- Skill: `squadra-design`, `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] Hardcoded category cards and category detail placeholders are removed.
- [ ] Empty, loading, error, editable, and read-only states are covered.
- [ ] Category form sends all required fields and preserves values on errors.
- [ ] Create/edit/delete refresh category list and tournament detail count.
- [ ] UI labels and errors are in Brazilian Portuguese.

**Tests**: Frontend component tests.
**Gate**: Frontend.

#### T9: Polish responsive category design and accessibility

**Status**: Done

**What**: Ensure category management layout works on desktop/mobile, has clear focus/error states, and uses Squadra visual identity without generic admin styling.
**Where**: Frontend styles layer
**Depends on**: T8
**Reuses**: Existing tournament CSS and Squadra palette.
**Requirement**: TCAT-07

**Tools**:

- MCP: NONE
- Skill: `squadra-design`, `squadra-ui`

**Done when**:

- [ ] Category cards have stable hover/focus behavior and readable contrast.
- [ ] Forms are usable on mobile without overlapping text or controls.
- [ ] Read-only category state is communicated without relying only on color.
- [ ] Controls have accessible labels and keyboard focus states.

**Tests**: Frontend component tests plus manual responsive check.
**Gate**: Frontend.

### Phase 4: Final Validation

#### T10: Run full backend and frontend validation

**Status**: Done

**What**: Run required quality gates and fix regressions until category implementation is verified.
**Where**: Project validation layer
**Depends on**: T6, T9
**Reuses**: Existing Maven, npm, and CI gates.
**Requirement**: TCAT-01 through TCAT-08

**Tools**:

- MCP: NONE
- Skill: `squadra-testing`

**Done when**:

- [ ] Backend full verification passes.
- [ ] Frontend `npm run check` passes.
- [ ] Frontend build passes if dependencies are available.
- [ ] Any unavailable gate is documented with the exact failure reason.

**Tests**: Full backend and frontend suites.
**Gate**: Full feature.

#### T11: Record implementation validation and traceability

**Status**: Done

**What**: Update feature validation notes, requirement statuses, and task statuses after implementation gates pass.
**Where**: Feature specification artifacts
**Depends on**: T10
**Reuses**: Existing active spec validation file pattern.
**Requirement**: TCAT-01 through TCAT-08

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`, `squadra-testing`

**Done when**:

- [ ] `validation.md` records gates, results, residual risks, and evidence.
- [ ] Requirement traceability statuses reflect implementation evidence.
- [ ] Task statuses reflect completed implementation work.
- [ ] TLC closure evidence is ready for final review.

**Tests**: Deterministic spec/task validation scripts.
**Gate**: Full feature.
