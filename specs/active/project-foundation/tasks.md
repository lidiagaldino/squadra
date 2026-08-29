# Squadra Project Foundation Tasks

**Design**: `specs/active/project-foundation/design.md`
**Status**: In Progress

## Test Coverage Matrix

> Generated from the empty codebase, `AGENTS.md`, and the specification. Guidelines found: `AGENTS.md`, `.agents/agents/squadra-specialist-agent.md`; no existing tests - strong defaults applied.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Web UI | unit | All baseline behavior and listed edge cases in scope | `frontend/src/**/*.test.tsx` | `npm run test` from `frontend/` |
| API resource | integration | Health route happy path | `backend/src/test/java/**/*Test.java` | `mvn test -f backend/pom.xml` |
| Config and schema | none | Build/static validation | `infra/**` | `docker compose -f infra/docker-compose.yml config` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Web test task | `cd frontend && npm run test` |
| Full | API integration task | `mvn test -f backend/pom.xml` |
| Build | Configuration or final phase | `cd frontend && npm run check && cd ../backend && mvn verify && cd .. && docker compose -f infra/docker-compose.yml config` |

## Execution Plan

### Phase 1: Workspace

`T1 → T2`

### Phase 2: Runtime

`T3 → T4`

### Phase 3: Delivery conventions

`T5`

## Task Breakdown

### T1: Create root workspace conventions

**What**: Create root conventions, ignore rules, environment template, and development guide.
**Where**: repository root
**Depends on**: None
**Requirement**: FOUND-01, FOUND-06
**Tools**: Skill: `tlc-spec-driven`
**Done when**: topology, local setup, and non-secret configuration are documented without a root npm workspace.
**Tests**: none
**Gate**: build

### T2: Create Next.js web baseline

**What**: Create web shell, query provider, test, lint, and type-check configuration.
**Where**: `frontend/`
**Depends on**: T1
**Requirement**: FOUND-01, FOUND-02
**Tools**: Skill: `squadra-design`
**Done when**: readiness screen and all web quality scripts are declared without product routes or auth flows.
**Tests**: unit
**Gate**: quick

### T3: Create Quarkus API baseline

**What**: Create API runtime, health resource, package layers, and API test.
**Where**: `backend/`
**Depends on**: T1
**Requirement**: FOUND-01, FOUND-03, FOUND-05
**Tools**: Skill: `squadra-architecture`, `squadra-security`
**Done when**: a non-sensitive versioned health response and layer skeleton exist with Maven verification.
**Tests**: integration
**Gate**: full

### T4: Create database and local orchestration baseline

**What**: Add Flyway schema baseline, container builds, and Docker Compose runtime.
**Where**: `infra/`
**Depends on**: T2, T3
**Requirement**: FOUND-01, FOUND-04
**Tools**: Skill: `squadra-security`
**Done when**: Compose defines all three services and Flyway creates only `squadra`.
**Tests**: none
**Gate**: build

### T5: Create quality automation

**What**: Add CI quality gate and coverage conventions.
**Where**: `.github/`
**Depends on**: T2, T3, T4
**Requirement**: FOUND-06, FOUND-07
**Tools**: Skill: `squadra-security`
**Done when**: CI runs web and API quality checks without production credentials and coverage targets are 80%.
**Tests**: none
**Gate**: build

## Validation

| Check | Result |
| --- | --- |
| Task granularity | Pass: each task has one deliverable directory or root convention set. |
| Diagram and dependencies | Pass: `T1 → T2`, `T1 → T3`, `T2,T3 → T4`, `T2,T3,T4 → T5`. |
| Test co-location | Pass: executable UI and API tasks include their tests. |
