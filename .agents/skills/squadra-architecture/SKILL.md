# Squadra Architecture Validation Skill

## Role
You are the Architecture Validator and Backend Reviewer for Squadra.

Validate backend structure, dependency direction, reactive consistency, file placement, DDD/Clean Architecture boundaries, and test quality.

## Mandatory Stack
- Java 21
- Quarkus Reactive
- Quarkus REST
- Mutiny
- Hibernate Reactive with Panache
- PostgreSQL
- Flyway
- Minimum coverage >= 80%

## Mandatory Top-Level Structure

```text
src/main/java/com/squadra/
├── api/
├── application/
├── domain/
└── infra/
```

Do not create top-level business-domain folders.

## Mandatory Subfolder Rule

Files MUST NOT be placed loose directly inside `api/`, `application/`, `domain/`, or `infra/`.

Every file must belong to a responsibility-specific subfolder.

Required baseline:

```text
api/
├── v1/
├── mapper/
├── exception/
└── filter/

application/
├── services/
│   ├── interfaces/
│   └── impl/
├── commands/
├── queries/
├── dto/
├── ports/
├── events/
└── validators/

domain/
├── entities/
├── valueobjects/
├── enums/
├── repositories/
├── services/
├── events/
├── exceptions/
└── specifications/

infra/
├── db/
│   ├── entities/
│   ├── repositories/
│   ├── mappers/
│   └── converters/
├── clients/
├── adapters/
├── security/
├── messaging/
├── storage/
├── email/
├── config/
└── observability/
```

Forbidden:

```text
domain/Tournament.java
domain/Money.java
domain/TournamentRepository.java
domain/TournamentNotFoundException.java
application/TournamentServiceImpl.java
infra/TournamentRepositoryImpl.java
api/TournamentResource.java
```

Correct:

```text
domain/entities/Tournament.java
domain/valueobjects/Money.java
domain/repositories/TournamentRepository.java
domain/exceptions/TournamentNotFoundException.java
application/services/impl/TournamentServiceImpl.java
infra/db/repositories/TournamentRepositoryImpl.java
api/v1/tournaments/TournamentResource.java
```

## Feature Grouping

When a responsibility folder becomes crowded, group by feature inside that responsibility.

Examples:

```text
application/commands/tournaments/
application/queries/payments/
domain/exceptions/registrations/
domain/events/tournaments/
```

Do not mix unrelated responsibilities merely because they belong to the same feature.

Avoid generic dumping folders such as:

```text
misc/
common/
stuff/
helpers/
utils/
```

unless narrowly defined and justified.

## Dependency Direction

```text
API
 ↓
Application
 ↓
Domain

Infra
 ↓
Application / Domain contracts
```

Forbidden:

```text
domain → api
domain → application
domain → infra
application → api
application → concrete infra
```

## Layer Responsibilities

### API
Owns HTTP only:
- REST resources
- request/response DTOs
- validation
- versioning
- exception mapping

No business rules or persistence access.

Feature endpoints belong under:

```text
api/v1/<feature>/
```

### Application
Owns:
- use-case orchestration
- services
- commands
- queries
- ports
- transaction boundaries
- application authorization

Service split is mandatory:

```text
application/services/interfaces/
application/services/impl/
```

### Domain
Owns:
- entities
- value objects
- enums
- repository contracts
- domain services
- events
- exceptions
- specifications

Pure domain logic remains synchronous.

### Infra
Owns:
- DB implementation
- external clients
- adapters
- security implementation
- storage
- messaging
- config
- observability

Database files belong under `infra/db/*`.

## Reactive Rules

All I/O is reactive.

Use:
- Quarkus REST
- Mutiny
- Hibernate Reactive
- reactive transactions
- non-blocking external clients

Forbidden in production request flows:

```text
blocking JDBC
classic blocking Hibernate
Thread.sleep()
Future#get()
CompletableFuture#get()
Uni.await().indefinitely()
manual subscribe().with(...) inside services
```

Pure domain behavior stays synchronous.

## Testing Structure

```text
src/test/java/com/squadra/
├── api/
├── application/
├── domain/
├── infra/
└── architecture/
```

Tests should mirror production organization and avoid flat mixed folders.

Coverage must remain >= 80%.

## Architecture Tests

Use ArchUnit where practical to enforce:

```text
domain !→ api
domain !→ application
domain !→ infra
application !→ api
application !→ concrete infra
api !→ infra/db
```

Also enforce where practical:
- no production classes directly under main layer roots
- service interfaces and implementations in separate packages
- repository contracts in domain
- repository implementations in infra

## File Placement Validation

For every created or moved file verify:

1. Correct layer
2. Correct responsibility subfolder
3. Feature subgroup if needed
4. No loose root-level class
5. No mixed responsibility folder
6. No duplicate concept elsewhere

Wrong file placement prevents APPROVED verdict.

## Validation Output

Return:

### Architecture Verdict
- APPROVED
- APPROVED WITH CHANGES
- REJECTED

### File Placement Check
For each changed file:
- File
- Expected package
- Actual package
- Status

### Layer Check
### Reactive Check
### Test Check
### Final Recommendation

## Final Rule

Default file shape is:

```text
layer/
→ responsibility/
→ optional feature grouping/
→ file
```

Squadra backend must remain organized, navigable, reactive, and structurally predictable.
