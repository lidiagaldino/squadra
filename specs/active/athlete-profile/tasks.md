# Athlete Profile Tasks

## Execution Protocol

Implement these tasks with the `tlc-spec-driven` skill and the Squadra Specialist Agent. Use `squadra-architecture`, `squadra-security`, `squadra-database`, `squadra-testing`, `squadra-api-contract`, `squadra-ui`, and `squadra-design` where the task touches that concern.

**Design**: `specs/active/athlete-profile/design.md`
**Status**: Done

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec. Guidelines found: `AGENTS.md`, `docs/development.md`, `.agents/skills/squadra-testing/SKILL.md`, backend `pom.xml`, frontend `package.json`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Domain | unit | Validate all profile invariants and enum/bounds edge cases from APROF-03/APROF-05 | `backend/src/test/java/com/squadra/platform/domain/entities/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Application service | unit | Cover create, edit, missing profile, upsert and ownership behavior | `backend/src/test/java/com/squadra/platform/application/services/impl/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| API/resource | integration | Cover `GET`/`PUT` happy path, `401`, `403`, `404`, `422`, and no client owner ID path | `backend/src/test/java/com/squadra/platform/api/v1/athletes/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Repository/migration | integration through API/service | Cover key upsert/query paths and unique athlete row behavior | `backend/src/test/java/com/squadra/platform/api/v1/athletes/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Frontend API/form/page | unit/component | Cover empty state, loaded profile, save success, validation errors, refresh retry and failed recovery | `frontend/src/features/athlete-profile/*.test.tsx`, `frontend/src/app/athlete/profile/*.test.tsx` | `npm run check` |
| Architecture/security | architecture/integration | Preserve layer placement, role enforcement and no stale package violations | `backend/src/test/java/com/squadra/platform/architecture/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick backend | Domain/application-only backend tasks | `JAVA_HOME=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home PATH=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test -q` |
| Full backend | API, DB, security or final backend tasks | `JAVA_HOME=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home PATH=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify -q` |
| Frontend | Frontend API/form/page tasks | `npm run check` |
| Full feature | Final validation | backend `mvn verify`, frontend `npm run check`, `docker compose --env-file .env.example -f infra/docker-compose.yml config`, API Docker build |

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
T1 → T4
T3 → T4
T4 → T5
T5 → T6
```

### Phase 3: Frontend Athlete Experience

```text
T5 → T7
T7 → T8
T8 → T9
```

### Phase 4: Final Validation

```text
T9 → T10
```

---

## Task Breakdown

### T1: Add athlete profile domain model

**What**: Create the domain entity, football position enum, dominant foot enum, repository contract and validation tests.
**Where**: `backend/src/main/java/com/squadra/platform/domain/entities/AthleteProfile.java`
**Depends on**: None
**Reuses**: Existing domain entity and enum style.
**Requirement**: APROF-01, APROF-02, APROF-03, APROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-testing`

**Done when**:

- [x] Domain entity validates required and optional field bounds.
- [x] Duplicate/invalid secondary position rules are enforced.
- [x] Domain tests cover APROF-03 edge cases.

**Tests**: Domain unit tests.
**Gate**: Quick backend.

---

### T2: Add athlete profile Flyway migration

**What**: Create `squadra.athlete_profiles` with one profile per user, constraints and indexes.
**Where**: `backend/src/main/resources/db/migration/V3__create_athlete_profiles.sql`
**Depends on**: T1
**Reuses**: Existing Flyway migration pattern.
**Requirement**: APROF-01, APROF-02, APROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-database`, `squadra-security`

**Done when**:

- [x] Migration defines `user_id` unique foreign key to `squadra.users`.
- [x] Check constraints enforce enum and bounded numeric values.
- [x] Date/time column semantics match database skill guidance.

**Tests**: Covered through API/repository integration in T5/T6.
**Gate**: Full backend after T6.

---

### T3: Add athlete profile repository implementation

**What**: Implement reactive repository adapter for find/upsert by authenticated user ID.
**Where**: `backend/src/main/java/com/squadra/platform/infra/db/repositories/JdbcAthleteProfileRepository.java`
**Depends on**: T1, T2
**Reuses**: `ReactiveJdbcSupport`.
**Requirement**: APROF-01, APROF-02, APROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-database`, `squadra-testing`

**Done when**:

- [x] Repository returns `Uni<Optional<AthleteProfile>>` for `findByUserId`.
- [x] Upsert preserves one row per user.
- [x] No request flow calls `.await().indefinitely()`.

**Tests**: Covered through application/API tests.
**Gate**: Quick backend compile/test after T4.

---

### T4: Add athlete profile application service

**What**: Implement service interface and implementation for getting and saving the current athlete profile.
**Where**: `backend/src/main/java/com/squadra/platform/application/services/interfaces/AthleteProfileService.java`
**Depends on**: T1, T3
**Reuses**: Existing application service split.
**Requirement**: APROF-01, APROF-02, APROF-03, APROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-testing`

**Done when**:

- [x] Service derives ownership from provided authenticated user ID only.
- [x] Service handles missing profile and upsert behavior.
- [x] Unit tests cover create, edit, same-submission idempotency and invalid input.

**Tests**: Application service unit tests.
**Gate**: Quick backend.

---

### T5: Add athlete profile API contract

**What**: Add request/response DTOs and REST resource for `GET` and `PUT /api/v1/athletes/me/profile`.
**Where**: `backend/src/main/java/com/squadra/platform/api/v1/athletes/profile/AthleteProfileResource.java`
**Depends on**: T4
**Reuses**: Auth request/response package style and `@RequiresRoles`.
**Requirement**: APROF-01, APROF-02, APROF-03, APROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-api-contract`, `squadra-security`, `squadra-testing`

**Done when**:

- [x] `GET` returns profile or `404 PROFILE_NOT_FOUND`.
- [x] `PUT` creates/updates and returns `200`.
- [x] `401`, `403`, and `422` behavior is tested.
- [x] Resource never reads owner ID from request body.

**Tests**: Backend API integration tests.
**Gate**: Full backend.

---

### T6: Update backend validation, architecture and coverage gates

**What**: Extend architecture/security validation where needed and keep backend coverage >= 80%.
**Where**: `backend/src/test/java/com/squadra/platform/architecture/LayerPlacementTest.java`
**Depends on**: T5
**Reuses**: Existing architecture placement test and auth API tests.
**Requirement**: APROF-01 through APROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-security`, `squadra-testing`

**Done when**:

- [x] No production classes are loose under layer roots.
- [x] No stale package references exist.
- [x] Backend `mvn verify` passes and JaCoCo line coverage remains >= 80%.

**Tests**: Architecture and full backend tests.
**Gate**: Full backend.

---

### T7: Add athlete profile frontend API client

**What**: Add typed API functions for loading/saving profile with Authorization header and one refresh retry.
**Where**: `frontend/src/features/athlete-profile/api.ts`
**Depends on**: T5
**Reuses**: `frontend/src/features/auth/auth-context.tsx` and auth API conventions.
**Requirement**: APROF-04

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-testing`

**Done when**:

- [x] API client supports `GET` and `PUT`.
- [x] Client handles `404 PROFILE_NOT_FOUND` as empty state.
- [x] Client retries once after refresh on `401`.
- [x] Unit tests cover success, missing profile and failed recovery.

**Tests**: Frontend unit tests.
**Gate**: Frontend.

---

### T8: Add athlete profile form

**What**: Build the profile form with Zod validation, loading, error and success states.
**Where**: `frontend/src/features/athlete-profile/form.tsx`
**Depends on**: T7
**Reuses**: React Hook Form + Zod form patterns.
**Requirement**: APROF-03, APROF-04

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-design`, `squadra-testing`

**Done when**:

- [x] Form covers all profile fields in the spec.
- [x] Client-side validation mirrors backend bounds.
- [x] Typed values are preserved after save failure.
- [x] Component tests cover empty, loaded, invalid and save-success states.

**Tests**: Frontend component tests.
**Gate**: Frontend.

---

### T9: Add athlete profile page

**What**: Add `/athlete/profile` page with Squadra athlete-facing visual treatment.
**Where**: `frontend/src/app/athlete/profile/page.tsx`
**Depends on**: T8
**Reuses**: Existing app layout and global styles.
**Requirement**: APROF-04

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-design`, `squadra-testing`

**Done when**:

- [x] Page renders a football-first athlete profile experience.
- [x] Empty, loading, error and saved states are accessible.
- [x] Page tests pass.

**Tests**: Frontend page/component tests.
**Gate**: Frontend.

---

### T10: Run full feature validation

**What**: Run backend, frontend, compose and Docker validation; update task statuses and produce implementation summary.
**Where**: repository root
**Depends on**: T9
**Reuses**: Existing validation commands.
**Requirement**: APROF-01 through APROF-05

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`, `squadra-testing`, `squadra-architecture`, `squadra-security`

**Done when**:

- [x] Backend `mvn verify` passes.
- [x] Backend coverage remains >= 80%.
- [x] Frontend `npm run check` passes.
- [x] Docker compose config and API/web builds pass where relevant.
- [x] No architecture placement violations remain.

**Tests**: Full feature validation.
**Gate**: Full feature.

---

## Diagram-Definition Cross-Check

| Diagram Edge | Matching `Depends on` | Status |
| --- | --- | --- |
| T1 -> T2 | T2 depends on T1 | OK |
| T1,T2 -> T3 | T3 depends on T1,T2 | OK |
| T1,T3 -> T4 | T4 depends on T1,T3 | OK |
| T4 -> T5 | T5 depends on T4 | OK |
| T5 -> T6 | T6 depends on T5 | OK |
| T5 -> T7 | T7 depends on T5 | OK |
| T7 -> T8 | T8 depends on T7 | OK |
| T8 -> T9 | T9 depends on T8 | OK |
| T9 -> T10 | T10 depends on T9 | OK |

## Test Co-location Validation

| Task | Required Test Type | Tests Co-located? | Status |
| --- | --- | --- | --- |
| T1 | Domain unit | Yes | OK |
| T2 | Integration through T5/T6 | Yes, deferred to dependent API integration | OK |
| T3 | Integration through T5/T6 | Yes, deferred to dependent API integration | OK |
| T4 | Application unit | Yes | OK |
| T5 | API integration | Yes | OK |
| T6 | Architecture/full backend | Yes | OK |
| T7 | Frontend unit | Yes | OK |
| T8 | Frontend component | Yes | OK |
| T9 | Frontend page/component | Yes | OK |
| T10 | Full validation | Yes | OK |

## Tools Confirmation Before Execute

Use the tools and skills listed per task unless the user gives different execution instructions.
