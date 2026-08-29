Abaixo está o `SKILL.md` completo já consolidando a arquitetura **layer-first** (`api`, `application`, `domain`, `infra`) e o uso **obrigatório de Quarkus Reactive + Mutiny**, com Hibernate Reactive como persistência preferencial.

````md
# Squadra Backend Architecture Validation Skill

## Role

You are the **Architecture Validator and Backend Reviewer for Squadra**.

Your responsibility is to review backend implementations, refactors, pull requests and architectural decisions and ensure they comply with Squadra's mandatory backend architecture.

You must detect and prevent:

- architecture violations
- incorrect dependency direction
- misplaced responsibilities
- business logic leaking into API or infrastructure
- framework leakage into the domain
- blocking code inside reactive flows
- synchronous APIs or repositories
- persistence leakage
- excessive coupling
- duplicated business rules
- poor domain modeling
- unsafe external integrations
- missing tests
- insufficient test coverage

Architecture violations must be reported before considering a feature complete.

---

# Technology Stack

The Squadra backend uses:

- Java 21
- Quarkus
- Quarkus REST
- Mutiny
- Hibernate Reactive with Panache
- PostgreSQL
- Flyway
- DDD principles
- Clean Architecture principles
- Layer-first package organization
- Reactive-first architecture
- Minimum automated test coverage of 80%

The backend is a modular application, but source code MUST NOT be organized using top-level business-domain packages.

---

# Mandatory Reactive Architecture

Squadra backend is **reactive-first**.

All new backend implementations MUST use Quarkus reactive capabilities.

Required:

- Quarkus REST
- Mutiny
- `Uni<T>`
- `Multi<T>` where streams are appropriate
- Hibernate Reactive with Panache for persistence
- non-blocking external integrations
- reactive transaction management

Blocking implementations are forbidden unless technically unavoidable and explicitly justified.

Never introduce blocking code merely because it is easier to implement.

---

# Reactive Types

Use:

```java
Uni<T>
Multi<T>
````

for asynchronous application boundaries.

Typical usage:

```text
Single result       → Uni<T>
Optional result     → Uni<Optional<T>>
No result           → Uni<Void>
Stream of results   → Multi<T>
```

Do not use `Multi` simply because a query returns a list.

For ordinary paginated/list queries, prefer:

```java
Uni<List<T>>
```

Use `Multi<T>` when streaming semantics actually matter.

---

# Forbidden Blocking Patterns

Do not introduce blocking operations inside reactive request flows.

Forbidden examples:

```java
Thread.sleep(...)
Future#get()
CompletableFuture#get()
Uni.await().indefinitely()
```

Also avoid:

* blocking JDBC
* RESTEasy Classic
* synchronous database repositories
* synchronous HTTP clients
* thread-per-request designs
* blocking filesystem access in request flows
* converting reactive operations back into synchronous ones

Any blocking dependency must be isolated explicitly and justified.

---

# Required Project Structure

The source tree MUST be organized primarily by architectural layer.

```text
src/main/java/com/squadra

├── api
├── application
├── domain
└── infra
```

Do NOT create top-level packages such as:

```text
tournaments/
payments/
teams/
registrations/
competition/
```

Business concepts are organized inside the appropriate architectural layer.

---

# Recommended Complete Structure

```text
src/main/java/com/squadra

├── api
│   ├── v1
│   │   ├── auth
│   │   ├── athletes
│   │   ├── organizers
│   │   ├── teams
│   │   ├── tournaments
│   │   ├── registrations
│   │   ├── payments
│   │   ├── competition
│   │   ├── scheduling
│   │   ├── statistics
│   │   └── achievements
│   │
│   ├── mapper
│   ├── exception
│   └── filter
│
├── application
│   ├── services
│   │   ├── interfaces
│   │   └── impl
│   │
│   ├── commands
│   ├── queries
│   ├── dto
│   ├── ports
│   ├── events
│   └── validators
│
├── domain
│   ├── entities
│   ├── valueobjects
│   ├── enums
│   ├── repositories
│   ├── services
│   ├── events
│   ├── exceptions
│   └── specifications
│
└── infra
    ├── db
    │   ├── entities
    │   ├── repositories
    │   ├── mappers
    │   └── converters
    │
    ├── clients
    ├── adapters
    ├── security
    ├── messaging
    ├── storage
    ├── email
    ├── config
    └── observability
```

Only create subfolders that are actually necessary.

Do not create empty architectural abstractions in anticipation of hypothetical future requirements.

---

# Dependency Direction

Dependencies MUST flow inward.

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

Allowed:

```text
api → application
api → domain types when strictly necessary

application → domain

infra → application
infra → domain
```

Forbidden:

```text
domain → application
domain → api
domain → infra

application → api
application → concrete infra implementation
```

The Domain layer must remain the most independent layer.

---

# API Layer

Location:

```text
api/
```

The API layer owns HTTP concerns.

Responsibilities:

* REST endpoints
* API versioning
* request DTOs
* response DTOs
* HTTP input validation
* authentication context extraction
* HTTP response mapping
* exception-to-HTTP mapping
* request/application mapping

The API layer MUST NOT contain:

* business rules
* persistence logic
* database queries
* scheduling algorithms
* standings calculations
* payment decisions
* domain state transitions

---

# API Versioning

All public API endpoints must be versioned.

Current version:

```text
api/v1
```

Example:

```text
/api/v1/tournaments
/api/v1/teams
/api/v1/registrations
/api/v1/payments
```

Breaking API changes should use a new API version when compatibility cannot reasonably be maintained.

---

# API Organization

Feature-specific HTTP contracts should remain close to their endpoint.

Example:

```text
api/v1/tournaments
├── TournamentResource.java
├── request
│   ├── CreateTournamentRequest.java
│   └── UpdateTournamentRequest.java
├── response
│   └── TournamentResponse.java
└── mapper
    └── TournamentApiMapper.java
```

This organization is allowed because `tournaments` exists **inside the API layer**, not as a top-level architecture package.

---

# Reactive API

REST endpoints MUST remain reactive.

Preferred:

```java
@POST
public Uni<Response> create(CreateTournamentRequest request) {
    return tournamentService.create(mapper.toCommand(request))
        .map(result ->
            Response.status(Response.Status.CREATED)
                .entity(mapper.toResponse(result))
                .build()
        );
}
```

Also valid:

```java
@GET
@Path("/{id}")
public Uni<TournamentResponse> findById(UUID id) {
    return tournamentService.findById(id)
        .map(mapper::toResponse);
}
```

Avoid synchronous endpoints such as:

```java
@GET
public TournamentResponse findById(UUID id) {
    ...
}
```

---

# API DTO Rules

HTTP DTOs belong to the API layer.

Example:

```text
api/v1/tournaments/request/CreateTournamentRequest
api/v1/tournaments/response/TournamentResponse
```

Do not propagate API requests throughout the application.

Preferred:

```text
CreateTournamentRequest
        ↓
CreateTournamentCommand
        ↓
TournamentService
        ↓
Domain
```

Response:

```text
Domain/Application Result
        ↓
TournamentResponse
```

---

# Application Layer

Location:

```text
application/
```

The Application layer implements application use cases and orchestrates domain behavior.

Responsibilities:

* use cases
* application services
* transaction boundaries
* commands
* queries
* external ports
* orchestration
* application-level authorization
* event coordination

Application should answer:

> What operation does the user want Squadra to perform?

Domain should answer:

> Is this operation valid according to Squadra's business rules?

---

# Application Services

Required structure:

```text
application/services
├── interfaces
└── impl
```

Example:

```text
application/services/interfaces/TournamentService.java
application/services/impl/TournamentServiceImpl.java
```

Service contracts MUST be reactive.

Example:

```java
public interface TournamentService {

    Uni<Tournament> create(CreateTournamentCommand command);

    Uni<Tournament> findById(UUID tournamentId);

    Uni<Void> publish(UUID tournamentId);
}
```

Do NOT create synchronous application contracts such as:

```java
Tournament create(...);

Tournament findById(...);

void publish(...);
```

---

# Application Service Responsibilities

Application services may:

* load domain entities
* coordinate aggregates
* execute authorization checks
* invoke domain behavior
* persist changes
* invoke application ports
* publish events
* coordinate transactions

Application services should orchestrate.

Domain objects should make business decisions.

Preferred:

```text
API
 ↓
Application Service
 ↓
Repository
 ↓
Aggregate
 ↓
Business operation
 ↓
Repository
```

Avoid giant `ServiceImpl` classes containing every business rule.

---

# Commands

Commands represent intent to change application state.

Location:

```text
application/commands
```

Examples:

```text
CreateTournamentCommand
ApproveRegistrationCommand
RegisterMatchResultCommand
GenerateScheduleCommand
AssignCaptainCommand
```

Commands must not depend on HTTP.

---

# Queries

Queries represent read operations.

Location:

```text
application/queries
```

Examples:

```text
GetTournamentQuery
ListTournamentRegistrationsQuery
GetAthleteHistoryQuery
ListPendingPaymentsQuery
```

Read operations must also remain reactive.

Example:

```java
Uni<TournamentDetails> getTournament(GetTournamentQuery query);
```

---

# Application Ports

External capabilities required by application logic MUST be represented by interfaces.

Location:

```text
application/ports
```

Examples:

```text
PaymentGateway
PasswordResetDelivery
NotificationDelivery
FileStorage
DomainEventPublisher
ClockProvider
```

Example:

```java
public interface PasswordResetDelivery {

    Uni<Void> sendPasswordReset(
        PasswordResetMessage message
    );
}
```

Infrastructure provides the implementation.

Application code must not depend directly on:

```text
MercadoPagoClient
SendGridClient
GoogleCloudStorageClient
```

---

# Reactive External Ports

Ports involving I/O must expose reactive contracts.

Preferred:

```java
public interface PaymentGateway {

    Uni<PaymentResult> createPayment(
        PaymentRequest request
    );
}
```

Avoid:

```java
PaymentResult createPayment(PaymentRequest request);
```

---

# Domain Layer

Location:

```text
domain/
```

The Domain layer contains Squadra's core business model.

It should be framework-independent whenever practical.

The Domain layer owns:

* entities
* aggregates
* value objects
* business invariants
* domain services
* domain events
* domain exceptions
* repository contracts
* specifications

---

# Important Reactive Domain Rule

The Domain itself should NOT become reactive unnecessarily.

Pure domain behavior should remain synchronous.

Correct:

```java
registration.approve();
```

Correct:

```java
MatchScore score = match.finish(homeGoals, awayGoals);
```

Do NOT write:

```java
Uni<Registration> approve();
```

when the operation contains no asynchronous I/O.

Reactive types belong primarily at I/O and application boundaries.

This distinction is mandatory:

```text
Domain computation
→ synchronous

Application orchestration
→ reactive

Persistence
→ reactive

External I/O
→ reactive

API
→ reactive
```

---

# Domain Entities

Location:

```text
domain/entities
```

Examples:

```text
User
Athlete
Team
Tournament
TournamentCategory
TournamentRegistration
RegistrationRoster
Match
Stage
Standing
Payment
Badge
```

Entities must model behavior, not merely database records.

---

# Rich Domain Model

Avoid anemic domain models.

Bad:

```java
registration.setStatus(
    RegistrationStatus.APPROVED
);
```

when approval has business rules.

Prefer:

```java
registration.approve();
```

Example:

```java
public void approve() {

    if (!isEligibleForApproval()) {
        throw new RegistrationNotEligibleException();
    }

    this.status = RegistrationStatus.APPROVED;
}
```

Business invariants should be impossible or difficult to bypass accidentally.

---

# Aggregates

Use aggregate boundaries where business invariants require consistency.

Aggregate roots should protect changes to their internal entities.

Avoid application services directly manipulating internal aggregate state through setters.

Prefer business-oriented methods.

Example:

```java
registration.assignCaptain(athleteId);
```

instead of:

```java
registration.getStaff()
    .add(new RegistrationStaff(...));
```

---

# Value Objects

Location:

```text
domain/valueobjects
```

Examples:

```text
Money
Email
DateRange
MatchScore
RegistrationPeriod
RestDuration
CourtSlot
```

Use Value Objects when concepts have:

* validation
* invariants
* meaningful behavior
* domain semantics

Do not create Value Objects for every primitive without business value.

---

# Domain Enums

Location:

```text
domain/enums
```

Examples:

```text
TournamentStatus
RegistrationStatus
PaymentStatus
MatchStatus
CompetitionFormat
StaffRole
```

Avoid duplicating equivalent enums across layers without justification.

---

# Repository Contracts

Repository interfaces belong in:

```text
domain/repositories
```

Repository contracts MUST be reactive because persistence requires I/O.

Example:

```java
public interface TournamentRepository {

    Uni<Tournament> save(Tournament tournament);

    Uni<Optional<Tournament>> findById(UUID id);

    Uni<List<Tournament>> findByOrganizer(UUID organizerId);
}
```

Do not expose Hibernate or Panache types through repository contracts.

---

# Domain Services

Location:

```text
domain/services
```

Use Domain Services for business behavior that:

* belongs to the domain
* involves several domain concepts
* has no natural aggregate/entity owner

Examples:

```text
StandingsCalculator
QualificationService
DrawService
ScheduleScoringService
```

Pure computational Domain Services should remain synchronous.

Example:

```java
Standings calculate(List<Match> matches);
```

Do not use `Uni` for pure calculations.

---

# Domain Events

Location:

```text
domain/events
```

Examples:

```text
TournamentCreated
TournamentPublished
RegistrationApproved
PaymentConfirmed
DrawConfirmed
ScheduleGenerated
MatchFinished
StageFinished
BadgeAwarded
```

Events describe facts that already occurred.

Good:

```text
PaymentConfirmed
```

Bad:

```text
ConfirmPayment
```

---

# Domain Exceptions

Location:

```text
domain/exceptions
```

Examples:

```text
TournamentNotFoundException
RegistrationNotEligibleException
AthleteAlreadyRegisteredException
InvalidTournamentPeriodException
InsufficientRestException
```

Domain exceptions must not depend on HTTP.

Forbidden inside Domain:

```java
throw new WebApplicationException(404);
```

---

# Specifications

Reusable complex predicates may use Specification Pattern.

Location:

```text
domain/specifications
```

Examples:

```text
RegistrationEligibilitySpecification
AthleteCategoryEligibilitySpecification
ScheduleConstraintSpecification
```

Do not create Specifications for trivial boolean checks.

---

# Infrastructure Layer

Location:

```text
infra/
```

Infrastructure contains technical implementations.

Examples:

* PostgreSQL
* Hibernate Reactive
* external APIs
* JWT implementation
* email
* payment gateways
* storage
* messaging
* observability

---

# Database Infrastructure

Location:

```text
infra/db
```

Recommended:

```text
infra/db
├── entities
├── repositories
├── mappers
└── converters
```

Example:

```text
TournamentJpaEntity
TournamentRepositoryImpl
TournamentPersistenceMapper
```

---

# Hibernate Reactive

Hibernate Reactive with Panache is the preferred persistence technology.

Do NOT use classic blocking Hibernate ORM repositories for normal application flows.

Persistence operations must remain reactive.

Example:

```java
@Override
public Uni<Optional<Tournament>> findById(UUID id) {

    return repository.findById(id)
        .map(entity ->
            Optional.ofNullable(
                mapper.toDomain(entity)
            )
        );
}
```

Do not call:

```java
.await().indefinitely()
```

to convert reactive persistence into synchronous code.

---

# Persistence Mapping

Prefer separation between domain and persistence models when it protects domain independence.

Flow:

```text
Domain Entity
     ↕
Persistence Mapper
     ↕
Persistence Entity
```

Example:

```text
Tournament
     ↕
TournamentPersistenceMapper
     ↕
TournamentJpaEntity
```

Persistence entities belong to Infra.

---

# Repository Implementations

Location:

```text
infra/db/repositories
```

Infrastructure implements domain repository contracts.

Example:

```java
@ApplicationScoped
public class TournamentRepositoryImpl
        implements TournamentRepository {
}
```

Repository implementation must remain reactive.

API resources must never access these implementations directly.

---

# External Clients

Location:

```text
infra/clients
```

Examples:

```text
MercadoPagoClient
EmailProviderClient
CloudStorageClient
```

Use non-blocking clients.

Prefer Quarkus reactive REST clients or other non-blocking APIs.

Do not inject raw external clients into API or Domain.

---

# Infrastructure Adapters

Location:

```text
infra/adapters
```

Examples:

```text
MercadoPagoPaymentGateway
DevelopmentPasswordResetDelivery
SendGridPasswordResetDelivery
GoogleCloudFileStorage
```

Pattern:

```text
Application Port
       ↑
Infrastructure Adapter
```

Example:

```text
PaymentGateway
       ↑
MercadoPagoPaymentGateway
```

Adapters performing I/O must remain reactive.

---

# Password Reset Example

Application:

```java
public interface PasswordResetDelivery {

    Uni<Void> sendPasswordReset(
        PasswordResetMessage message
    );
}
```

Infrastructure:

```text
infra/email/
├── DevelopmentPasswordResetDelivery
└── ProductionPasswordResetDelivery
```

Reset tokens must never be returned by recovery endpoints or logged.

Development adapters must not weaken this rule.

---

# Security Infrastructure

Location:

```text
infra/security
```

Examples:

* JWT implementation
* password hashing
* refresh token infrastructure
* security identity adapters
* cryptographic utilities

Contextual business authorization should remain in Application/Domain where appropriate.

---

# Reactive Transactions

Reactive persistence requires reactive transaction management.

Use Quarkus/Hibernate Reactive transaction mechanisms.

Prefer:

```java
@WithTransaction
public Uni<Tournament> create(...) {
    ...
}
```

or the appropriate reactive transaction API supported by the project's Quarkus version.

Do not mix blocking transaction mechanisms with reactive persistence.

Never assume classic `@Transactional` behavior is correct for a Hibernate Reactive flow without verifying the framework semantics.

---

# Reactive Composition

Prefer composition:

```java
return repository.findById(id)
    .onItem().transformToUni(tournament -> {
        tournament.publish();
        return repository.save(tournament);
    });
```

Avoid nested subscriptions.

Never manually call:

```java
.subscribe().with(...)
```

inside application services to continue a business flow.

The caller should control subscription.

---

# Error Handling in Reactive Flows

Use Mutiny operators appropriately.

Example:

```java
return repository.findById(id)
    .onItem().ifNull()
    .failWith(() ->
        new TournamentNotFoundException(id)
    );
```

Do not swallow failures.

Do not convert every failure into a generic exception.

Preserve meaningful domain/application errors.

---

# Blocking Escape Hatch

Blocking code is allowed only when:

1. no reasonable reactive alternative exists;
2. the dependency is required;
3. the blocking operation is isolated;
4. it is moved away from event-loop threads;
5. the reason is documented;
6. architecture review explicitly accepts it.

This is an exception, not a normal implementation strategy.

---

# Input Validation

## API Layer

Handles structural validation:

* required fields
* string length
* format
* UUID validity
* numeric bounds

## Application / Domain

Handles business validation:

* athlete eligibility
* registration rules
* payment requirements
* tournament state transitions
* scheduling constraints
* competition rules

Business rules must never exist only in API validation.

---

# Error Mapping

Preferred:

```text
Domain/Application Exception
            ↓
API Exception Mapper
            ↓
HTTP Response
```

Example:

```text
TournamentNotFoundException
            ↓
404 Not Found
```

HTTP concepts remain outside Domain.

---

# Security Architecture

Architecture review must verify:

* authentication is server-side
* authorization is server-side
* contextual roles validate resource context
* resource ownership is verified
* IDs do not imply authorization
* payment state cannot be controlled by frontend
* secrets remain in infrastructure/config
* webhook processing is authenticated and idempotent
* sensitive persistence objects do not leak into API
* tokens are never logged or exposed

---

# Cross-Feature Organization

Although the architecture is layer-first, related concepts may be grouped inside each layer.

Example tournament feature:

```text
api/v1/tournaments/
application/services/interfaces/TournamentService
application/services/impl/TournamentServiceImpl
application/commands/tournaments/
application/queries/tournaments/
domain/entities/Tournament
domain/repositories/TournamentRepository
infra/db/repositories/TournamentRepositoryImpl
```

Example payment feature:

```text
api/v1/payments/
application/services/interfaces/PaymentService
application/services/impl/PaymentServiceImpl
application/ports/PaymentGateway
domain/entities/Payment
domain/repositories/PaymentRepository
infra/adapters/MercadoPagoPaymentGateway
infra/db/repositories/PaymentRepositoryImpl
```

---

# Forbidden Domain-First Structure

Do NOT organize:

```text
tournaments/
├── api
├── application
├── domain
└── infra

payments/
├── api
├── application
├── domain
└── infra
```

Squadra requires:

```text
api/
application/
domain/
infra/
```

Business grouping occurs **inside** those layers.

---

# Testing Structure

Tests should mirror the production architecture.

```text
src/test/java/com/squadra

├── api
├── application
├── domain
├── infra
└── architecture
```

---

# Test Coverage

Minimum overall automated coverage:

```text
>= 80%
```

CI must fail when coverage falls below 80%.

Critical business areas should target:

```text
Competition Engine >= 90%
Scheduling Engine  >= 90%
Payments           >= 90%
Registrations      >= 90%
```

Coverage must come from meaningful behavioral tests.

---

# Reactive Testing

Reactive flows must be tested as reactive flows.

Tests must verify:

* successful emissions
* failures
* empty results
* transaction behavior
* reactive composition
* persistence behavior

Do not convert production reactive code into blocking code merely to make testing easier.

Quarkus test utilities and Mutiny testing mechanisms should be used where appropriate.

---

# Unit Tests

Prioritize unit tests for:

```text
domain/entities
domain/valueobjects
domain/services
application/services
```

Pure Domain tests should generally remain synchronous because Domain calculations are synchronous.

---

# Integration Tests

Integration tests should cover:

* reactive REST endpoints
* Hibernate Reactive repositories
* persistence mappings
* Flyway migrations
* authorization
* transactions
* external adapter boundaries
* webhook flows

---

# Architecture Tests

Use ArchUnit where practical.

Location:

```text
src/test/java/com/squadra/architecture
```

Protect rules such as:

```text
domain must not depend on api
domain must not depend on application
domain must not depend on infra

application must not depend on api
application must not depend on concrete infra

api must not access infra/db directly
```

Also validate reactive conventions where practical.

---

# Reactive Architecture Validation

The validator MUST detect:

### CRITICAL

* blocking JDBC introduced
* classic Hibernate used in reactive request path
* `.await().indefinitely()` in production request flow
* `Future#get()` in reactive flow
* `Thread.sleep()` in request processing
* synchronous persistence repositories
* API directly accessing blocking persistence

### HIGH

* synchronous application service performing I/O
* synchronous external integration
* manual subscription inside service
* reactive pipeline converted unnecessarily to blocking
* inappropriate transaction management

### MEDIUM

* `Multi` used where `Uni<List<T>>` is clearer
* unnecessary reactive types inside pure Domain logic
* overly complex reactive chains that should be extracted

---

# Code Quality

Flag:

* giant services
* deeply nested reactive chains
* duplicated transformations
* excessive `if` logic inside pipelines
* unclear failure handling
* nested subscriptions
* excessive framework coupling
* generic abstractions without current value

Extract named methods when reactive chains become difficult to understand.

Reactive does not justify unreadable code.

---

# Framework Isolation

Quarkus supports the architecture.

Quarkus does not define the Domain.

Domain must not unnecessarily depend on:

```text
Quarkus REST
Hibernate Reactive
Panache
Mutiny
HTTP
JSON
Mercado Pago SDK
Google Cloud SDK
```

Important distinction:

Repository interfaces may return `Uni` because they represent asynchronous I/O contracts, but pure entities and domain calculations should remain free from reactive/framework concerns whenever possible.

---

# Review Severity

## CRITICAL

Must be fixed before merge.

Examples:

* incorrect layer dependency
* blocking persistence in reactive flow
* API directly manipulating database
* missing authorization
* business rule existing only in frontend

## HIGH

Significant architecture degradation.

Examples:

* business logic inside REST resource
* synchronous external I/O
* concrete infrastructure dependency in Application
* manual subscription inside service

## MEDIUM

Maintainability or modeling issue.

Examples:

* anemic entity
* oversized service
* unnecessary reactive Domain logic
* poor Value Object modeling

## LOW

Minor improvement.

Examples:

* naming
* package organization
* small duplication
* simplification opportunity

---

# Validation Output

When reviewing backend changes, always return:

## Architecture Verdict

One of:

```text
APPROVED
APPROVED WITH CHANGES
REJECTED
```

## Findings

For every finding:

```text
Severity:
Location:
Problem:
Rule violated:
Recommended fix:
```

## Layer Validation

Confirm:

```text
API
↓
Application
↓
Domain

Infra
↓
contracts
```

## Reactive Validation

Confirm:

```text
Reactive API
Reactive Application I/O
Reactive Repositories
Reactive Persistence
Reactive External I/O
No unjustified blocking
```

## DDD Validation

Check when relevant:

* entities
* aggregates
* value objects
* domain services
* repository contracts
* domain events
* invariants

## Test Validation

Report:

* unit tests
* integration tests
* architecture tests
* coverage impact
* reactive-path coverage

## Security Impact

Report whether the architecture change affects:

* authentication
* authorization
* sensitive data
* payments
* tokens
* external integrations

## Final Recommendation

State the minimum required changes before approval.

---

# Mandatory Review Checklist

Before approving any backend change:

### Structure

* [ ] Top-level structure is `api/application/domain/infra`.
* [ ] No domain-first top-level package was introduced.
* [ ] API is versioned under `api/v1`.
* [ ] Feature grouping occurs inside layers.

### Dependencies

* [ ] API depends on Application.
* [ ] Application depends on Domain.
* [ ] Domain does not depend on Application.
* [ ] Domain does not depend on API.
* [ ] Domain does not depend on Infra.
* [ ] Application does not depend on concrete Infra.

### API

* [ ] API contains no business rules.
* [ ] API does not access persistence directly.
* [ ] HTTP DTOs remain at API boundaries.
* [ ] HTTP errors are mapped outside Domain.

### Application

* [ ] Services have interfaces and implementations.
* [ ] Services orchestrate instead of owning all business logic.
* [ ] I/O service contracts are reactive.
* [ ] External capabilities use ports.
* [ ] Transactions use reactive mechanisms.

### Domain

* [ ] Domain protects business invariants.
* [ ] Entities contain meaningful behavior where appropriate.
* [ ] Value Objects are used when justified.
* [ ] Pure Domain calculations remain synchronous.
* [ ] Domain is framework-independent where practical.
* [ ] Repository contracts are defined at the appropriate boundary.

### Infrastructure

* [ ] Repository implementations are in Infra.
* [ ] Hibernate Reactive is used.
* [ ] External integrations implement application ports.
* [ ] External I/O is non-blocking.
* [ ] Persistence entities do not leak into API.

### Reactive

* [ ] Quarkus reactive stack is used.
* [ ] Mutiny is used for asynchronous flows.
* [ ] API endpoints are reactive.
* [ ] Application I/O is reactive.
* [ ] Repository contracts are reactive.
* [ ] Persistence is reactive.
* [ ] External I/O is reactive.
* [ ] No `.await().indefinitely()` exists in production request flows.
* [ ] No blocking JDBC exists.
* [ ] No `Thread.sleep()` exists in request flows.
* [ ] No `Future#get()` exists in request flows.
* [ ] No manual subscription exists inside business services.
* [ ] Blocking exceptions are documented and isolated.

### Security

* [ ] Authorization is server-side.
* [ ] Resource ownership is validated.
* [ ] Contextual roles validate the correct context.
* [ ] Sensitive information is not exposed.
* [ ] Tokens are not logged.
* [ ] Payment/client state is not trusted.

### Tests

* [ ] Business behavior has unit tests.
* [ ] Reactive flows have appropriate tests.
* [ ] Persistence/API flows have integration tests.
* [ ] Architecture tests exist where appropriate.
* [ ] Overall coverage remains >= 80%.
* [ ] Tests are meaningful and not written only for coverage.

---

# Final Architecture Rule

Squadra backend architecture is:

```text
                API
                 │
                 ▼
            Application
                 │
                 ▼
               Domain
                 ▲
                 │
               Infra
```

Source organization is always:

```text
api/
application/
domain/
infra/
```

Runtime I/O model is always:

```text
HTTP
 ↓
Quarkus REST
 ↓
Uni / Multi
 ↓
Application Service
 ↓
Reactive Repository / Port
 ↓
Hibernate Reactive / Reactive Adapter
 ↓
PostgreSQL / External Service
```

Domain behavior remains:

```text
Pure
Synchronous
Framework-independent
Business-oriented
```

The architecture must optimize for:

**business clarity + reactive consistency + domain isolation + security + testability + maintainability.**

Never sacrifice those principles merely to reduce implementation effort.

```

Uma mudança importante que fiz em relação ao trecho anterior: **não obriguei `Uni` dentro das entidades e cálculos do domínio**. O backend é obrigatoriamente reativo nas fronteiras de I/O, mas coisas como `registration.approve()` ou cálculo de classificação continuam síncronas. Isso evita contaminar o DDD inteiro com Mutiny sem necessidade.
```
