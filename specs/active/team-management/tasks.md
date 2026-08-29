# Team Management Tasks

## Execution Protocol

Implemented with the `tlc-spec-driven` skill and the Squadra Specialist Agent after explicit approval.

**Design**: `specs/active/team-management/design.md`
**Status**: Implemented

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec. Guidelines found: `AGENTS.md`, `.agents/agents/squadra-specialist-agent.md`, `docs/roles-and-permissions.md`, `docs/business-rules.md`, `docs/security-business-constraints.md`, backend `pom.xml`, frontend `package.json`, `frontend/vitest.config.ts`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Domain | unit | Validate team field bounds, admin-role constraints, athlete-membership states, explicit self-join, invitation transitions, independence rules and owner invariant from TEAM-01, TEAM-05, TEAM-07, TEAM-10, TEAM-13, TEAM-14 | `backend/src/test/java/com/squadra/platform/domain/entities/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Application service | unit | Cover create owner assignment, list/detail scoping, owner/admin edit rules, self-join, owner-only admin management, owner/admin athlete invitation management, athlete accept/reject, roster removal, transfer and deterministic error paths | `backend/src/test/java/com/squadra/platform/application/services/impl/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| API/resource | integration | Cover every route: `201`, `200`, `401`, `403`, `404`, `409`, `422`, no private data exposure, and no client-provided authorization bypass | `backend/src/test/java/com/squadra/platform/api/v1/teams/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Repository/migration | integration through API/service | Cover create-with-owner, visible list, admin assignment/removal, invitation lifecycle, ownership transfer, one owner, and one athlete membership row per team/athlete | `backend/src/test/java/com/squadra/platform/api/v1/teams/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Frontend API/form/page | unit/component | Cover list/create, detail load/edit, admin controls, athlete invitation controls, invited-athlete actions, read-only member state, refresh retry, error preservation and Teams sidebar link | `frontend/src/features/teams/*.test.tsx`, `frontend/src/app/teams/**/*.test.tsx`, `frontend/src/app/home/page.test.tsx` | `npm run check` |
| Architecture/security | architecture/integration | Preserve layer placement, contextual authorization, private-resource non-disclosure, owner invariant, and no stale package violations | `backend/src/test/java/com/squadra/platform/architecture/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify` |

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

### Phase 3: Frontend Team Experience

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

### T1: Add team domain model

**What**: Create team identity, team administration roles, athlete membership states, domain exceptions and domain validation tests.
**Where**: `backend/src/main/java/com/squadra/platform/domain/entities/Team.java`
**Depends on**: None
**Reuses**: Existing domain validation style.
**Requirement**: TEAM-01, TEAM-05, TEAM-07, TEAM-08, TEAM-09, TEAM-10, TEAM-13, TEAM-14

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-testing`

**Done when**:

- [ ] Team fields validate all TEAM-01 bounds.
- [ ] Team administration allows only `TEAM_OWNER` and `TEAM_ADMIN`.
- [ ] Athlete membership allows only `INVITED` and `ACTIVE`.
- [ ] Tournament roles and athlete membership states are rejected as administration roles.
- [ ] Domain tests cover explicit self-join, invitation accept/reject/cancel/remove state rules, administration/membership independence and owner invariant rules.

**Tests**: Domain unit tests.
**Gate**: Quick backend.

---

### T2: Add team persistence migration

**What**: Create `squadra.teams`, `squadra.team_administrators`, and `squadra.team_athlete_memberships` with constraints and indexes.
**Where**: `backend/src/main/resources/db/migration/V5__create_teams.sql`
**Depends on**: T1
**Reuses**: Existing Flyway migration pattern.
**Requirement**: TEAM-01, TEAM-05, TEAM-06, TEAM-07, TEAM-08, TEAM-09, TEAM-10, TEAM-13, TEAM-14

**Tools**:

- MCP: NONE
- Skill: `squadra-database`, `squadra-security`

**Done when**:

- [ ] `teams` stores bounded team identity fields and timestamps.
- [ ] `team_administrators` stores one row per `(team_id, user_id)` and only `TEAM_OWNER`/`TEAM_ADMIN`.
- [ ] `team_athlete_memberships` stores one row per `(team_id, athlete_user_id)` and only `INVITED`/`ACTIVE`.
- [ ] Database design documents the exactly-one-owner enforcement strategy, indexes and concurrent transfer implications.
- [ ] Foreign keys prevent orphaned teams/admins/athlete memberships.

**Tests**: Covered through API/repository integration in T5/T6.
**Gate**: Full backend after T6.

---

### T3: Add team repository implementation

**What**: Implement repository adapter for teams, administrators, and athlete memberships.
**Where**: `backend/src/main/java/com/squadra/platform/infra/db/repositories/JdbcTeamRepository.java`
**Depends on**: T1, T2
**Reuses**: `ReactiveJdbcSupport`.
**Requirement**: TEAM-01, TEAM-02, TEAM-03, TEAM-04, TEAM-05, TEAM-06, TEAM-07, TEAM-08, TEAM-09, TEAM-10, TEAM-13, TEAM-14

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-database`, `squadra-testing`

**Done when**:

- [ ] Repository creates a team with one owner atomically.
- [ ] Repository lists teams visible through administration or athlete membership.
- [ ] Repository supports admin assignment/removal and atomic ownership transfer.
- [ ] Repository supports explicit self-join, invite, accept, reject, cancel and remove athlete membership transitions.
- [ ] Repository preserves administration when athlete membership changes and preserves athlete membership when administration changes.
- [ ] No request flow calls `.await().indefinitely()`.

**Tests**: Covered through service/API tests.
**Gate**: Quick backend compile/test after T4.

---

### T4: Add team application service

**What**: Implement team use cases and contextual authorization.
**Where**: `backend/src/main/java/com/squadra/platform/application/services/interfaces/TeamService.java`
**Depends on**: T3
**Reuses**: Existing service split and `UserRepository`.
**Requirement**: TEAM-01, TEAM-02, TEAM-03, TEAM-04, TEAM-05, TEAM-06, TEAM-07, TEAM-08, TEAM-09, TEAM-10, TEAM-12, TEAM-13, TEAM-14

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-security`, `squadra-testing`

**Done when**:

- [ ] Create assigns exactly one `TEAM_OWNER`.
- [ ] List/detail return only teams related to the authenticated user and include independent administration/membership fields.
- [ ] Edit allows owner/admin and rejects athlete-only/no-relationship users with deterministic `403`/`404` behavior.
- [ ] Self-join allows global `ATHLETE` admins and rejects organizer-only admins.
- [ ] Admin management allows owner only and rejects invalid roles/statuses.
- [ ] Athlete invitation management allows owner/admin, handles duplicate/already-active/cancel/accept/reject conflicts and requires athlete accept before `ACTIVE`.
- [ ] Ownership transfer is owner-only, supports transfer to `TEAM_ADMIN` and `ACTIVE` athlete, preserves athlete memberships and leaves exactly one owner.
- [ ] Unit tests cover combination states, independence, forbidden, not-found, conflict, invalid transitions and concurrency-sensitive invariants.

**Tests**: Application service unit tests.
**Gate**: Quick backend.

---

### T5: Add teams API contract

**What**: Add DTOs, resources and exception mappers for team list/detail/admin/athlete invitation routes.
**Where**: `backend/src/main/java/com/squadra/platform/api/v1/teams/TeamsResource.java`
**Depends on**: T4
**Reuses**: Existing REST resource, validation and mapper style.
**Requirement**: TEAM-01, TEAM-02, TEAM-03, TEAM-04, TEAM-05, TEAM-06, TEAM-07, TEAM-08, TEAM-09, TEAM-10, TEAM-12, TEAM-13, TEAM-14

**Tools**:

- MCP: NONE
- Skill: `squadra-api-contract`, `squadra-security`, `squadra-testing`

**Done when**:

- [ ] `POST /api/v1/teams` creates a team and returns `201`.
- [ ] `GET /api/v1/teams` lists only visible teams.
- [ ] `GET`/`PUT /api/v1/teams/{teamId}` enforce deterministic contextual access.
- [ ] Self-join route explicitly creates `ACTIVE` athlete membership for eligible athlete admins only.
- [ ] Admin routes enforce owner-only access and reject non-admin role strings.
- [ ] Athlete invitation routes enforce owner/admin management and athlete-only accept/reject.
- [ ] `401`, `403`, `404`, `409`, `422`, unknown fields and no private data exposure are tested.

**Tests**: Backend API integration tests.
**Gate**: Full backend.

---

### T6: Update backend validation, architecture and coverage gates

**What**: Extend architecture/security validation where needed and keep backend coverage >= 80%.
**Where**: `backend/src/test/java/com/squadra/platform/architecture/LayerPlacementTest.java`
**Depends on**: T5
**Reuses**: Existing architecture placement test and auth API tests.
**Requirement**: TEAM-01 through TEAM-14

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-security`, `squadra-testing`

**Done when**:

- [ ] No production classes are loose under layer roots.
- [ ] No stale package references exist.
- [ ] Full backend gate passes with coverage >= 80%.

**Tests**: Architecture and full backend tests.
**Gate**: Full backend.

---

### T7: Add teams frontend API client

**What**: Implement typed frontend client for list/create/detail/edit/admin/invitation operations with one refresh retry.
**Where**: `frontend/src/features/teams/api.ts`
**Depends on**: T5
**Reuses**: Existing profile API client pattern.
**Requirement**: TEAM-01 through TEAM-13

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] API client exposes list, create, detail, edit, self-join, admin, ownership, athlete invitation and roster functions.
- [ ] `401` triggers one token refresh and one retry.
- [ ] API tests cover success, forbidden, not-found, conflict, validation failures and failed recovery.

**Tests**: Frontend API unit tests.
**Gate**: Frontend.

---

### T8: Add Teams list route

**What**: Build `/teams` with authenticated sidebar, team list, empty state and create entry point.
**Where**: `frontend/src/app/teams/page.tsx`
**Depends on**: T7
**Reuses**: Home/profile sidebar and form styling.
**Requirement**: TEAM-02, TEAM-11, TEAM-12

**Tools**:

- MCP: Stitch for UI exploration if major layout needs refinement
- Skill: `squadra-design`, `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] `/teams` shows the user's teams or an empty-state create prompt.
- [ ] Successful create adds the team to the visible list without a reload.
- [ ] Invited teams are visually distinct from active participation.
- [ ] List items link to `/teams/{teamId}`.
- [ ] Tests cover empty, create, load failure and auth recovery states.

**Tests**: Frontend component/page tests.
**Gate**: Frontend.

---

### T9: Add Team detail route

**What**: Build `/teams/{teamId}` with team identity, roster, invitations, administration and transfer sections.
**Where**: `frontend/src/app/teams/[teamId]/page.tsx`
**Depends on**: T8
**Reuses**: Existing app shell and form patterns.
**Requirement**: TEAM-03, TEAM-04, TEAM-05, TEAM-06, TEAM-07, TEAM-08, TEAM-09, TEAM-10, TEAM-11, TEAM-12, TEAM-13

**Tools**:

- MCP: Stitch for UI exploration if major layout needs refinement
- Skill: `squadra-design`, `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] Owner/admin see editable team identity.
- [ ] Athlete-only users see read-only team identity.
- [ ] Athlete admins see explicit "join team as athlete" action when eligible.
- [ ] Owner sees admin assignment/removal and ownership transfer controls.
- [ ] Owner/admin see athlete invitation, cancellation and active athlete removal controls.
- [ ] Invited athlete sees invitation-specific language plus accept and reject actions.
- [ ] Controls derive independently from administration role and athlete membership status.
- [ ] Tests cover `TEAM_OWNER` only, `TEAM_OWNER + ACTIVE`, `TEAM_ADMIN` only, `TEAM_ADMIN + ACTIVE`, `ACTIVE` only, `INVITED` only, no relationship, save failure and typed value preservation.

**Tests**: Frontend component/page tests.
**Gate**: Frontend.

---

### T10: Wire team navigation

**What**: Update authenticated sidebars so Teams navigates to `/teams`.
**Where**: `frontend/src/app/home/page.tsx`
**Depends on**: T9
**Reuses**: Existing role-aware `HomeShell`.
**Requirement**: TEAM-11

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-testing`

**Done when**:

- [ ] Home sidebar Teams link points to `/teams`.
- [ ] Athlete profile sidebar Teams link points to `/teams`.
- [ ] Organizer profile sidebar Teams link points to `/teams`.
- [ ] Page tests cover the updated navigation.

**Tests**: Frontend page tests.
**Gate**: Frontend.

---

### T11: Final feature validation and report

**What**: Run full relevant validation, write validation evidence, and update requirement traceability.
**Where**: `specs/active/team-management/validation.md`
**Depends on**: T10
**Reuses**: Existing validation report style.
**Requirement**: TEAM-01 through TEAM-14

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`, `squadra-testing`, `squadra-architecture`, `squadra-security`, `squadra-api-contract`, `squadra-database`

**Done when**:

- [ ] Backend full validation passes or any environment limitation is documented.
- [ ] Frontend `npm run check` and `npm run build` pass.
- [ ] `validation.md` cites file/line evidence for every acceptance criterion.
- [ ] Spec traceability status is updated to Verified.
- [ ] Final validation confirms every TEAM-01 through TEAM-14 requirement maps to implementation and tests.

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
