# Tournament Visibility and Team Invitations Tasks

## Execution Protocol

Use `tlc-spec-driven` and the Squadra Specialist Agent after approval. Each task includes tests for the behavior it changes; run its gate before marking it complete. Preserve existing uncommitted changes and stage only intended paths/hunks.

**Spec**: `specs/active/tournament-visibility-invitations/spec.md`
**Design**: `specs/active/tournament-visibility-invitations/design.md`
**Status**: In Progress

## Test Coverage Matrix

> Generated from `AGENTS.md`, `.agents/agents/squadra-specialist-agent.md`, `.agents/skills/squadra-testing/SKILL.md`, backend `pom.xml`, frontend `package.json`, CI `quality.yml`, and existing tournament/registration tests.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Domain/application | unit | Visibility guard and every invitation transition, authorization branch, and duplicate/capacity edge from TVIS-01 through TVIS-04 | `backend/src/test/java/com/squadra/platform/{domain,application}/**/*Test.java` | `mvn test -f backend/pom.xml` |
| Repository/migration | integration | Existing-row backfill, active uniqueness, transactional last-slot race, invitation history and read scoping | `backend/src/test/java/com/squadra/platform/api/v1/**/*Test.java` | `mvn verify -f backend/pom.xml` |
| API/security | integration | Every new route: success, unauthenticated, wrong role, foreign resource, duplicate, full category, invalid input | `backend/src/test/java/com/squadra/platform/api/v1/**/*Test.java` | `mvn verify -f backend/pom.xml` |
| Frontend API/BFF/views | unit/component | Discovery, visibility control, invitation send/cancel/accept/reject, state refresh, pt-BR labels, loading/empty/conflict states | `frontend/src/**/*.test.tsx`, `frontend/src/**/*.test.ts` | `npm run check` in `frontend` |
| Schema only | integration through API | Migration and constraints are exercised by repository/API integration tests | `backend/src/test/java/com/squadra/platform/api/v1/**/*Test.java` | `mvn verify -f backend/pom.xml` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Backend quick | Domain/service changes | `mvn test -f backend/pom.xml` |
| Backend full | Migration/API/concurrency changes | `mvn verify -f backend/pom.xml` |
| Frontend | BFF/client/UI changes | `npm run check` in `frontend` |
| Final build | Final validation | `mvn verify -f backend/pom.xml`; `npm run check` and `npm run build` in `frontend` |

## Execution Plan

### Phase 1: Visibility and Discovery

```text
T1 -> T2
```

### Phase 2: Invitation Integrity

```text
T2 -> T3 -> T4 -> T5
```

### Phase 3: Product Experience

```text
T5 -> T6 -> T7 -> T8
```

## Task Breakdown

### T1: Persist tournament visibility

**Status**: Complete

**What**: Add `PUBLIC`/`PRIVATE` visibility to tournament domain, save contract, responses, repository, and forward Flyway migration.
**Where**: `backend/src/main/java/com/squadra/platform/`; `backend/src/main/resources/db/migration/`
**Depends on**: None
**Reuses**: Tournament entity/service/resource/repository patterns and `V6__create_tournaments.sql`.
**Requirement**: TVIS-01
**Done when**: Existing rows read as `PUBLIC`; draft owner can change visibility; post-publish change is `409`; invalid values are `422`; tests prove each outcome. Invalid enum input is covered by REST validation alongside route tests.
**Tests**: Domain/service unit and API integration tests.
**Gate**: Backend full.

### T2: Add scoped tournament discovery

**Status**: Complete

**What**: Add bounded public available list and public/invited owner-scoped tournament overview/category reads.
**Where**: `backend/src/main/java/com/squadra/platform/`
**Depends on**: T1
**Reuses**: `TournamentRepository`, `TournamentCategoryRepository`, existing role filter.
**Requirement**: TVIS-02, TVIS-05
**Done when**: Public open tournaments are discoverable after login; private IDs return `404` to unrelated users; owner/invited admins have scoped details; tests cover empty and paged lists.
**Tests**: API/security integration tests.
**Gate**: Backend full.

### T3: Add invitation lifecycle and persistence

**Status**: Complete

**What**: Add invitation aggregate, repository contract/adapter, status enum, and forward migration with pending uniqueness.
**Where**: `backend/src/main/java/com/squadra/platform/`; `backend/src/main/resources/db/migration/`
**Depends on**: T2
**Reuses**: Registration repository and Flyway schema conventions.
**Requirement**: TVIS-03, TVIS-04
**Done when**: Pending, accepted, rejected, cancelled, and invalid transitions are testable; history remains; DB prevents duplicate pending invitations with a nullable status marker compatible with H2 and PostgreSQL.
**Tests**: Domain unit and repository/API integration tests.
**Gate**: Backend full.

### T4: Make registration capacity atomic

**Status**: Complete

**What**: Give public requests and invitation acceptance the same transactional active uniqueness and category capacity guard.
**Where**: `backend/src/main/java/com/squadra/platform/infra/db/repositories/`; `backend/src/main/resources/db/migration/`
**Depends on**: T3
**Reuses**: `TeamRegistrationRepository` and category max-team rule.
**Requirement**: TVIS-02, TVIS-04
**Done when**: Public registration locks the category while checking uniqueness/capacity and inserting; migration enforces one active registration; concurrent last-slot test yields exactly one create and one conflict. Invitation acceptance uses this same transaction path in T5.
**Tests**: PostgreSQL-backed integration/concurrency tests.
**Gate**: Backend full.

### T5: Expose invitation use cases and API

**Status**: Complete

**What**: Implement owner invite/list/cancel, minimal team search, and team inbox/accept/reject endpoints with contextual authorization.
**Where**: `backend/src/main/java/com/squadra/platform/`
**Depends on**: T4
**Reuses**: Existing tournament/team access checks, registration response mapping, exception mappers.
**Requirement**: TVIS-03, TVIS-04, TVIS-05
**Done when**: All invite routes enforce owner/team-admin roles and status; safe logs contain IDs only; `401`/`403`/`404`/`409`/`422` paths are covered.
**Tests**: Application unit and API integration tests.
**Gate**: Backend full.

### T6: Add frontend API and BFF contracts

**Status**: Complete

**What**: Add typed visibility, discovery, directory, and invitation clients plus authenticated BFF proxy routes.
**Where**: `frontend/src/features/`; `frontend/src/app/api/`
**Depends on**: T5
**Reuses**: `tournamentsApi`, `teamsApi`, `authenticatedBackendRequest`.
**Requirement**: TVIS-01 through TVIS-05
**Done when**: All new endpoints have typed client calls and Portuguese status labels; route/client tests cover success and error propagation.
**Tests**: Frontend API/BFF unit tests.
**Gate**: Frontend.

### T7: Add public and private UI workflows

**Status**: Pending

**What**: Add visibility control, authenticated public discovery, private owner invitations, and team invitation inbox/decision actions.
**Where**: `frontend/src/features/tournaments/`; `frontend/src/features/teams/`
**Depends on**: T6
**Reuses**: Existing tournament/category/team forms and React Query refresh pattern.
**Requirement**: TVIS-01 through TVIS-05
**Done when**: Owner and team-admin workflows work end to end, inaccessible actions are absent, all copy is pt-BR, and component tests cover key states and conflicts.
**Tests**: Frontend component tests.
**Gate**: Frontend.

### T8: Validate migrations and feature contracts

**Status**: Pending

**What**: Run full gates, inspect API/security/DB compatibility, update relevant business docs, and record independent spec-anchored verification.
**Where**: `specs/active/tournament-visibility-invitations/`; `docs/`
**Depends on**: T7
**Reuses**: CI commands and TLC validation scripts.
**Requirement**: TVIS-01 through TVIS-05
**Done when**: All acceptance criteria have file/line/test evidence, migrations preserve existing rows, backend/frontend gates pass, and docs describe visibility and invitation lifecycle.
**Tests**: Full backend/frontend suites and TLC validation.
**Gate**: Final build.

## Pre-Approval Checks

| Check | Result |
| --- | --- |
| Task granularity | Pass: each task has one main deliverable and a local verification gate. |
| Diagram/dependencies | Pass: T1 through T8 form the same linear chain in both diagrams and fields. |
| Test co-location | Pass: domain/service/API/frontend changes carry tests; migration paths are checked by integration tests. |
