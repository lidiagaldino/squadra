# Organizer Profile Tasks

## Execution Protocol

Implement these tasks with the `tlc-spec-driven` skill and the Squadra Specialist Agent. Use `squadra-architecture`, `squadra-security`, `squadra-database`, `squadra-testing`, `squadra-api-contract`, `squadra-ui`, and `squadra-design` where the task touches that concern.

**Design**: `specs/active/organizer-profile/design.md`
**Status**: Verified

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec. Guidelines found: `AGENTS.md`, `docs/development.md`, `.agents/skills/squadra-testing/SKILL.md`, backend `pom.xml`, frontend `package.json`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Domain | unit | Validate all organizer profile invariants and bounds from OPROF-03/OPROF-05 | `backend/src/test/java/com/squadra/platform/domain/entities/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Application service | unit | Cover create, edit, missing profile, upsert and ownership behavior | `backend/src/test/java/com/squadra/platform/application/services/impl/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| API/resource | integration | Cover `GET`/`PUT` happy path, `401`, `403`, `404`, `422`, and no client owner ID path | `backend/src/test/java/com/squadra/platform/api/v1/organizers/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Repository/migration | integration through API/service | Cover key upsert/query paths and unique organizer row behavior | `backend/src/test/java/com/squadra/platform/api/v1/organizers/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 test` |
| Frontend API/form/page | unit/component | Cover empty state, loaded profile, save success, validation errors, refresh retry, failed recovery and sidebar route | `frontend/src/features/organizer-profile/*.test.tsx`, `frontend/src/app/organizer/profile/*.test.tsx`, `frontend/src/app/home/page.test.tsx` | `npm run check` |
| Architecture/security | architecture/integration | Preserve layer placement, role enforcement and no stale package violations | `backend/src/test/java/com/squadra/platform/architecture/*Test.java` | `mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify` |

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
T1 → T4
T3 → T4
T4 → T5
T5 → T6
```

### Phase 3: Frontend Organizer Experience

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

### T1: Add organizer profile domain model

**What**: Create the domain entity, repository contract, not-found exception and validation tests.
**Where**: `backend/src/main/java/com/squadra/platform/domain/entities/OrganizerProfile.java`
**Depends on**: None
**Reuses**: Existing domain entity style and `EmailAddress` value object.
**Requirement**: OPROF-01, OPROF-02, OPROF-03, OPROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-testing`

**Done when**:

- [x] Domain entity validates required and optional field bounds.
- [x] Website accepts only `http://` or `https://` when supplied.
- [x] Domain tests cover OPROF-03 edge cases.

**Tests**: Domain unit tests.
**Gate**: Quick backend.

---

### T2: Add organizer profile Flyway migration

**What**: Create `squadra.organizer_profiles` with one profile per user, constraints and indexes.
**Where**: `backend/src/main/resources/db/migration/V4__create_organizer_profiles.sql`
**Depends on**: T1
**Reuses**: Existing Flyway migration pattern.
**Requirement**: OPROF-01, OPROF-02, OPROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-database`, `squadra-security`

**Done when**:

- [x] Migration defines `user_id` unique foreign key to `squadra.users`.
- [x] Check constraints enforce bounded text and website scheme.
- [x] Date/time column semantics match database skill guidance.

**Tests**: Covered through API/repository integration in T5/T6.
**Gate**: Full backend after T6.

---

### T3: Add organizer profile repository implementation

**What**: Implement reactive repository adapter for find/upsert by authenticated user ID.
**Where**: `backend/src/main/java/com/squadra/platform/infra/db/repositories/JdbcOrganizerProfileRepository.java`
**Depends on**: T1, T2
**Reuses**: `ReactiveJdbcSupport`.
**Requirement**: OPROF-01, OPROF-02, OPROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-database`, `squadra-testing`

**Done when**:

- [x] Repository returns `Uni<Optional<OrganizerProfile>>` for `findByUserId`.
- [x] Upsert preserves one row per user.
- [x] No request flow calls `.await().indefinitely()`.

**Tests**: Covered through application/API tests.
**Gate**: Quick backend compile/test after T4.

---

### T4: Add organizer profile application service

**What**: Implement service interface and implementation for getting and saving the current organizer profile.
**Where**: `backend/src/main/java/com/squadra/platform/application/services/interfaces/OrganizerProfileService.java`
**Depends on**: T1, T3
**Reuses**: Existing application service split.
**Requirement**: OPROF-01, OPROF-02, OPROF-03, OPROF-05

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

### T5: Add organizer profile API contract

**What**: Add request/response DTOs and REST resource for `GET` and `PUT /api/v1/organizers/me/profile`.
**Where**: `backend/src/main/java/com/squadra/platform/api/v1/organizers/profile/OrganizerProfileResource.java`
**Depends on**: T4
**Reuses**: Athlete profile API contract style and `@RequiresRoles`.
**Requirement**: OPROF-01, OPROF-02, OPROF-03, OPROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-api-contract`, `squadra-security`, `squadra-testing`

**Done when**:

- [x] `GET` returns profile or `404 ORGANIZER_PROFILE_NOT_FOUND`.
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
**Requirement**: OPROF-01 through OPROF-05

**Tools**:

- MCP: NONE
- Skill: `squadra-architecture`, `squadra-security`, `squadra-testing`

**Done when**:

- [x] No production classes are loose under layer roots.
- [x] No stale package references exist.
- [x] Focused backend Organizer Profile gate passes.

**Tests**: Architecture and full backend tests.
**Gate**: Full backend.

---

### T7: Add organizer profile frontend API client

**What**: Implement typed frontend client for loading and saving organizer profiles with one refresh retry.
**Where**: `frontend/src/features/organizer-profile/api.ts`
**Depends on**: T5
**Reuses**: `frontend/src/features/athlete-profile/api.ts`.
**Requirement**: OPROF-01, OPROF-02, OPROF-04

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-testing`

**Done when**:

- [x] `404 ORGANIZER_PROFILE_NOT_FOUND` maps to `null`.
- [x] `401` triggers one token refresh and one retry.
- [x] API tests cover success, empty profile and failed recovery.

**Tests**: Frontend API unit tests.
**Gate**: Frontend.

---

### T8: Add organizer profile form and route

**What**: Build `/organizer/profile` with authenticated app shell, organizer-focused form, validation and save behavior.
**Where**: `frontend/src/features/organizer-profile/form.tsx`
**Depends on**: T7
**Reuses**: Athlete profile form patterns and Squadra sidebar styling.
**Requirement**: OPROF-03, OPROF-04

**Tools**:

- MCP: Stitch for UI exploration if major layout needs refinement
- Skill: `squadra-design`, `squadra-ui`, `squadra-testing`

**Done when**:

- [x] Form validates all OPROF-03 frontend bounds before save.
- [x] Empty, loading, error and success states are role-appropriate.
- [x] Route renders inside the authenticated sidebar shell.
- [x] Component/page tests cover empty, loaded, save success, validation and save failure.

**Tests**: Frontend component/page tests.
**Gate**: Frontend.

---

### T9: Wire organizer navigation from Home

**What**: Update the authenticated Home sidebar so organizer users navigate to `/organizer/profile`.
**Where**: `frontend/src/app/home/page.tsx`
**Depends on**: T8
**Reuses**: Existing role-aware `HomeShell`.
**Requirement**: OPROF-04

**Tools**:

- MCP: NONE
- Skill: `squadra-ui`, `squadra-testing`

**Done when**:

- [x] Organizer sidebar Profile link points to `/organizer/profile`.
- [x] Athlete sidebar Profile link still points to `/athlete/profile`.
- [x] Home page tests cover both role-specific links.

**Tests**: Frontend page tests.
**Gate**: Frontend.

---

### T10: Final feature validation and report

**What**: Run full relevant validation, write validation evidence, and update requirement traceability.
**Where**: `specs/active/organizer-profile/validation.md`
**Depends on**: T9
**Reuses**: Existing validation report style.
**Requirement**: OPROF-01 through OPROF-05

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`, `squadra-testing`, `squadra-architecture`, `squadra-security`, `squadra-api-contract`, `squadra-database`

**Done when**:

- [x] Backend focused/full validations pass or any environment limitation is documented.
- [x] Frontend `npm run check` and `npm run build` pass.
- [x] `validation.md` cites file/line evidence for every acceptance criterion.
- [x] Spec traceability status is updated to Verified.

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
| T1 → T4 | T4 depends on T1 | Pass |
| T3 → T4 | T4 depends on T3 | Pass |
| T4 → T5 | T5 depends on T4 | Pass |
| T5 → T6 | T6 depends on T5 | Pass |
| T5 → T7 | T7 depends on T5 | Pass |
| T7 → T8 | T8 depends on T7 | Pass |
| T8 → T9 | T9 depends on T8 | Pass |
| T9 → T10 | T10 depends on T9 | Pass |

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
| T8 | Frontend form/page | Component/page tests in same task | Pass |
| T9 | Frontend navigation | Page tests in same task | Pass |
| T10 | Validation report | Full feature gate | Pass |
