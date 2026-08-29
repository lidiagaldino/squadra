# Squadra Project Foundation Specification

## Problem Statement

Squadra currently contains only business documentation. Engineers need one repeatable local baseline for the web application, API, and PostgreSQL database before product domains are implemented.

## Goals

- [ ] Provide a runnable frontend, backend, database, and local orchestration baseline.
- [ ] Establish secure configuration, layering, testing, and development conventions.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Authentication, JWT, and refresh tokens | Identity behavior is not specified. |
| Domain modules and product workflows | The requested foundation must not implement product features. |
| Business tables and seed data | Data ownership belongs to future domain specifications. |
| Production deployment | Local foundation does not authorize infrastructure provisioning. |

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Repository topology | Independent `frontend`, `backend`, and `infra` directories | Keeps a single frontend package without an unnecessary root npm workspace. | y |
| Local orchestration | Docker Compose with PostgreSQL, API, and web services | Reproducible onboarding without host Maven. | y |
| API availability | Versioned health endpoint is anonymous; future product endpoints default to authenticated | Health checks need no product data; MVP tournament pages require authentication. | y |
| Database baseline | Flyway owns an empty `squadra` schema only | Avoids premature persistence choices. | y |

**Open questions:** none - all resolved or logged above.

## User Stories

### P1: Reproducible local workspace

**User Story**: As a Squadra engineer, I want a single project structure and local startup path so that I can begin a domain feature without inventing infrastructure conventions.

**Why P1**: Every future feature depends on this baseline.

**Acceptance Criteria**:

1. WHEN an engineer follows the documented local setup THEN the system SHALL start PostgreSQL, the Quarkus API, and the Next.js web application with Docker Compose.
2. WHEN the web workspace is checked THEN the system SHALL type-check, lint, and run its test suite through declared npm scripts.
3. WHEN the API workspace is checked THEN the system SHALL compile and run its test suite through declared Maven commands.
4. The system SHALL keep frontend, backend, and infrastructure files in separate top-level workspace directories.

**Independent Test**: Build and test each application, then request the API health endpoint.

### P1: Safe application baseline

**User Story**: As a platform engineer, I want configuration and module conventions that protect future product work so that critical business rules do not drift into clients or HTTP resources.

**Why P1**: Squadra holds personal, competition, and payment data.

**Acceptance Criteria**:

1. WHEN the API starts THEN the system SHALL read database and CORS configuration from environment variables with non-secret local defaults only.
2. WHEN Flyway runs against a new local database THEN the system SHALL create the `squadra` schema without product-domain tables.
3. The system SHALL place future backend modules under `api`, `services`, `domain`, and `infra` layers.
4. IF a secret is needed THEN the system SHALL require it through an ignored local environment file and SHALL provide only a placeholder in tracked templates.
5. WHILE product endpoints are introduced THEN the system SHALL treat backend authorization as the enforcement point and SHALL not treat frontend visibility as authorization.

**Independent Test**: Inspect environment templates, run the migration against a clean database, and compile the API.

### P1: Development conventions

**User Story**: As a contributor, I want documented quality and delivery conventions so that changes are consistent from the first feature onward.

**Why P1**: The platform begins without implementation conventions.

**Acceptance Criteria**:

1. WHEN a contributor opens the repository THEN the system SHALL document prerequisites, commands, topology, environment files, and no-feature boundary.
2. The system SHALL enforce 80% coverage thresholds for frontend and backend executable code as a project convention.
3. WHEN continuous integration runs THEN the system SHALL execute frontend and backend quality gates without requiring production credentials.

**Independent Test**: Review the README, test configuration, and CI workflow.

## Edge Cases

- IF Docker is unavailable THEN the system SHALL document host-native frontend and backend verification commands.
- IF a future contributor adds a product endpoint THEN the system SHALL require an explicit authorization design in that feature specification.
- IF an environment value is absent THEN the system SHALL fail only for required runtime secrets and SHALL never substitute a tracked secret.

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FOUND-01 | Local workspace | Foundation | Pending |
| FOUND-02 | Local workspace | Frontend | Pending |
| FOUND-03 | Local workspace | Backend | Pending |
| FOUND-04 | Safe baseline | Database | Pending |
| FOUND-05 | Safe baseline | Backend | Pending |
| FOUND-06 | Development conventions | Quality | Pending |
| FOUND-07 | Development conventions | CI | Pending |

**Coverage:** 7 total, 0 completed, 7 pending.

## Success Criteria

- [ ] A new engineer can identify setup and verification commands from the root.
- [ ] The foundation contains no product-domain entities, workflows, or authentication implementation.
- [ ] Application and database build configuration is ready once dependencies are installed.
