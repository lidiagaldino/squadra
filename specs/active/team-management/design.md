# Team Management Design

**Spec**: `specs/active/team-management/spec.md`
**Status**: Draft

---

## Architecture Overview

Team Management is implemented as a new Teams slice with separate concepts for team administration and athlete membership. `TEAM_OWNER` and `TEAM_ADMIN` authorize management actions. Athlete membership uses `INVITED` and `ACTIVE` states and represents sporting participation. Administration and athlete membership are independent relationships, so a user can be `TEAM_OWNER + ACTIVE` or `TEAM_ADMIN + ACTIVE`. Tournament-specific roles remain outside this model.

```mermaid
graph TD
    ListUI[/teams] --> TeamsClient[teamsApi]
    DetailUI[/teams/{teamId}] --> TeamsClient
    TeamsClient --> TeamsResource[TeamsResource]
    TeamsResource --> TeamService[TeamService]
    TeamService --> Team[Team]
    TeamService --> Admin[TeamAdministrator]
    TeamService --> AthleteMember[TeamAthleteMembership]
    TeamService --> TeamRepo[TeamRepository]
    TeamService --> UserRepo[UserRepository]
    TeamRepo --> DB[(teams + team_administrators + team_athlete_memberships)]
```

Significant screen implementation must run the UI chain in order: Business Rules, `DESIGN.md`, `squadra-design`, Stitch MCP exploration when available, `squadra-ui`, then implementation. Stitch output can explore layout and interaction, but cannot define permissions or domain behavior.

---

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
| --- | --- | --- |
| Authenticated sidebar shell | `frontend/src/app/home/page.tsx`, `frontend/src/app/athlete/profile/page.tsx`, `frontend/src/app/organizer/profile/page.tsx` | Reuse visual layout and sign-out flow for `/teams` and `/teams/{teamId}`. |
| Auth context and refresh retry pattern | `frontend/src/features/auth/auth-context.tsx`, profile API clients | Use bearer token, cookies, and one refresh retry. |
| REST role filter | `backend/src/main/java/com/squadra/platform/api/filter/RequiresRoles.java` | Require global authenticated roles at resource entry, then contextual team permission in service. |
| User repository | `backend/src/main/java/com/squadra/platform/domain/repositories/UserRepository.java` | Resolve admin and athlete targets by email and validate global user role. |
| Reactive repository style | `backend/src/main/java/com/squadra/platform/infra/db/repositories/*Repository.java` | Follow existing Mutiny/JDBC repository adapters. |
| Profile API request/response style | `backend/src/main/java/com/squadra/platform/api/v1/*/profile` | Mirror DTO validation, exception mappers, and resource tests. |
| Architecture tests | `backend/src/test/java/com/squadra/platform/architecture/LayerPlacementTest.java` | Ensure new files are not loose in layer roots. |

### Integration Points

| System | Integration Method |
| --- | --- |
| Identity | Use authenticated `VerifiedAccessToken.userId()` for the acting user and `UserRepository.findByEmail` for assignment/invitation targets. |
| Database | Add `teams`, `team_administrators`, and `team_athlete_memberships`. |
| Frontend navigation | Update Home/profile sidebars so Teams links to `/teams`. |
| DESIGN.md and Squadra design skills | Apply the product visual direction before creating `/teams` and `/teams/{teamId}`. |

---

## Components

### Team Domain

- **Purpose**: Validate permanent team identity and ownership/admin/membership invariants.
- **Location**: `backend/src/main/java/com/squadra/platform/domain/entities/`
- **Interfaces**:
  - `Team.create(fields, now): Team`
  - `Team.update(fields, now): void`
  - `TeamAdministrator.create(teamId, userId, role, now): TeamAdministrator`
  - `TeamAthleteMembership.invited(teamId, athleteUserId, now): TeamAthleteMembership`
  - `TeamAthleteMembership.activate(now): void`
- **Dependencies**: `TeamAdminRole`, `TeamAthleteMembershipStatus`, `UUID`, `Instant`.
- **Reuses**: Existing domain validation style from profiles.

### Team Repository

- **Purpose**: Persist teams, administrators, athlete memberships, invitations, and visible-team queries.
- **Location**: `backend/src/main/java/com/squadra/platform/domain/repositories/TeamRepository.java`, `backend/src/main/java/com/squadra/platform/infra/db/repositories/JdbcTeamRepository.java`
- **Interfaces**:
  - `createWithOwner(Team, TeamAdministrator): Uni<TeamDetails>`
  - `listForUser(userId): Uni<List<TeamSummary>>`
  - `findVisibleToUser(teamId, userId): Uni<Optional<TeamDetails>>`
  - `saveTeam(Team, viewerUserId): Uni<TeamDetails>`
  - `upsertAdmin(teamId, userId, role, viewerUserId): Uni<TeamDetails>`
  - `removeAdmin(teamId, userId, viewerUserId): Uni<TeamDetails>`
  - `transferOwnership(teamId, fromUserId, toUserId): Uni<TeamDetails>`
  - `inviteAthlete(teamId, athleteUserId, viewerUserId): Uni<TeamDetails>`
  - `selfJoinAthlete(teamId, athleteUserId): Uni<TeamDetails>`
  - `activateAthlete(teamId, athleteUserId): Uni<TeamDetails>`
  - `removeAthleteMembership(teamId, athleteUserId, viewerUserId): Uni<TeamDetails>`
- **Dependencies**: `ReactiveJdbcSupport`.
- **Reuses**: Existing DB adapter pattern.

### Team Service

- **Purpose**: Orchestrate team use cases and enforce contextual authorization.
- **Location**: `backend/src/main/java/com/squadra/platform/application/services/interfaces/TeamService.java`, `backend/src/main/java/com/squadra/platform/application/services/impl/DefaultTeamService.java`
- **Interfaces**:
  - `create(userId, command): Uni<TeamDetails>`
  - `listMine(userId): Uni<List<TeamSummary>>`
  - `getMine(userId, teamId): Uni<TeamDetails>`
  - `update(userId, teamId, command): Uni<TeamDetails>`
  - `assignAdmin(userId, teamId, command): Uni<TeamDetails>`
  - `removeAdmin(userId, teamId, targetUserId): Uni<TeamDetails>`
  - `transferOwnership(userId, teamId, targetUserId): Uni<TeamDetails>`
  - `joinAsAthlete(userId, teamId): Uni<TeamDetails>`
  - `inviteAthlete(userId, teamId, command): Uni<TeamDetails>`
  - `acceptInvitation(userId, teamId): Uni<TeamDetails>`
  - `rejectInvitation(userId, teamId): Uni<Void>`
  - `cancelInvitation(userId, teamId, athleteUserId): Uni<TeamDetails>`
  - `removeActiveAthlete(userId, teamId, athleteUserId): Uni<TeamDetails>`
- **Dependencies**: `TeamRepository`, `UserRepository`, `Clock`.
- **Reuses**: Existing service interface/implementation split.

### Teams API

- **Purpose**: Expose the authenticated team management contract.
- **Location**: `backend/src/main/java/com/squadra/platform/api/v1/teams/`
- **Interfaces**:
  - `GET /api/v1/teams`
  - `POST /api/v1/teams`
  - `GET /api/v1/teams/{teamId}`
  - `PUT /api/v1/teams/{teamId}`
  - `PUT /api/v1/teams/{teamId}/admins`
  - `DELETE /api/v1/teams/{teamId}/admins/{userId}`
  - `PUT /api/v1/teams/{teamId}/owner`
  - `POST /api/v1/teams/{teamId}/athletes/me`
  - `POST /api/v1/teams/{teamId}/athlete-invitations`
  - `POST /api/v1/teams/{teamId}/athlete-invitations/accept`
  - `POST /api/v1/teams/{teamId}/athlete-invitations/reject`
  - `DELETE /api/v1/teams/{teamId}/athlete-invitations/{userId}`
  - `DELETE /api/v1/teams/{teamId}/athletes/{userId}`
- **Dependencies**: `TeamService`, access-token request property.
- **Reuses**: Existing `@RequiresRoles` and exception mapper style.

### Teams Frontend

- **Purpose**: Let authenticated users create teams, manage authorized team detail, and respond to athlete invitations.
- **Location**: `frontend/src/app/teams/page.tsx`, `frontend/src/app/teams/[teamId]/page.tsx`, `frontend/src/features/teams/`
- **Interfaces**:
  - `teamsApi.listTeams(auth)`
  - `teamsApi.createTeam(auth, input)`
  - `teamsApi.getTeam(auth, teamId)`
  - `teamsApi.updateTeam(auth, teamId, input)`
  - `teamsApi.assignAdmin(auth, teamId, input)`
  - `teamsApi.removeAdmin(auth, teamId, userId)`
  - `teamsApi.transferOwnership(auth, teamId, userId)`
  - `teamsApi.joinAsAthlete(auth, teamId)`
  - `teamsApi.inviteAthlete(auth, teamId, email)`
  - `teamsApi.acceptInvitation(auth, teamId)`
  - `teamsApi.rejectInvitation(auth, teamId)`
  - `teamsApi.cancelInvitation(auth, teamId, userId)`
  - `teamsApi.removeAthlete(auth, teamId, userId)`
- **Dependencies**: React Hook Form, Zod, TanStack Query, auth context.
- **Reuses**: Existing app shell/profile form patterns.

---

## Data Models

### Team

```typescript
interface Team {
  id: string
  name: string
  city: string | null
  state: string | null
  foundedYear: number | null
  description: string | null
  createdAt: string
  updatedAt: string
}
```

**Relationships**: A team has one `TEAM_OWNER`, zero or more `TEAM_ADMIN` rows, and zero or more athlete memberships.

### TeamAdministrator

```typescript
type TeamAdminRole = "TEAM_OWNER" | "TEAM_ADMIN"

interface TeamAdministrator {
  teamId: string
  userId: string
  email: string
  role: TeamAdminRole
  createdAt: string
  updatedAt: string
}
```

**Relationships**: A user can administer many teams. Each `(teamId, userId)` has one administration row. A team has exactly one owner.

### TeamAthleteMembership

```typescript
type TeamAthleteMembershipStatus = "INVITED" | "ACTIVE"

interface TeamAthleteMembership {
  teamId: string
  athleteUserId: string
  email: string
  status: TeamAthleteMembershipStatus
  invitedAt: string
  updatedAt: string
}
```

**Relationships**: A user can be an athlete member of many teams. Each `(teamId, athleteUserId)` has one athlete membership row.

### Team Detail Response

```typescript
interface TeamDetails extends Team {
  administrationRole: "TEAM_OWNER" | "TEAM_ADMIN" | null
  athleteMembershipStatus: "INVITED" | "ACTIVE" | null
  administrators: TeamAdministrator[]
  athletes: TeamAthleteMembership[]
}
```

**Relationships**: Capabilities derive independently from `administrationRole` and `athleteMembershipStatus`.

---

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
| --- | --- | --- |
| Missing/invalid access token | Resource/filter returns `401` | User sees auth-required state or refresh retry occurs. |
| Related user can view team but lacks operation permission | Service returns `403 TEAM_FORBIDDEN` | User sees a safe authorization message. |
| User has no relationship to private team | Service returns `404 TEAM_NOT_FOUND` | Team existence is not leaked. |
| Target user email not found or not an `ATHLETE` for athlete invitation | Service returns `404 TEAM_TARGET_USER_NOT_FOUND` | Admin sees a safe target-not-found message. |
| Invalid team data or invalid admin role string | Bean/domain validation returns `422 VALIDATION_FAILED` | Form keeps typed values and shows validation messages. |
| Duplicate invitation or already active athlete | Service returns `409 TEAM_MEMBERSHIP_CONFLICT` | Admin sees the current membership state is already occupied. |
| Accept invitation twice or reject accepted invitation | Service returns `409 TEAM_MEMBERSHIP_CONFLICT` | Athlete sees the invitation is no longer pending. |
| Accept/reject cancelled or previously rejected invitation | Service returns `404 TEAM_NOT_FOUND` | No private/pending relationship is exposed. |
| Removing/demoting only owner or transfer target with no relationship | Service returns `422 VALIDATION_FAILED` | Owner must perform a valid ownership transfer. |
| Concurrent ownership transfers | Repository transaction preserves one owner | System never exposes zero or multiple owners. |
| Unknown request fields | Resource returns `422 VALIDATION_FAILED` | Unknown data is not persisted silently. |

---

## Risks & Concerns

| Concern | Location | Impact | Mitigation |
| --- | --- | --- | --- |
| Team administration and athlete membership are easy to conflate | `docs/roles-and-permissions.md`, `docs/business-rules.md` | A user might be incorrectly modeled as either admin or athlete, not both. | Use separate domain entities/tables/enums for administration and athlete membership. |
| Tournament roles could leak into permanent team management | `docs/tournaments-and-registrations.md` | CAPTAIN/TEAM_MANAGER could be granted outside registration context. | Reject tournament-only role strings and omit them from permanent enums. |
| Owner invariant is concurrency-sensitive | `docs/security-business-constraints.md` | Transfers could leave zero or multiple owners. | Use transaction-level owner transfer and a database owner uniqueness strategy compatible with the chosen DB. |
| Owner deletion/deactivation can orphan teams | `docs/business-rules.md` | Team may become unmanageable. | Add identity deletion/deactivation guard before any future account lifecycle feature can remove a unique owner. |
| The UI has separate list/detail responsibilities | `.agents/skills/squadra-ui/SKILL.md` | A single crowded `/teams` screen would not scale. | Use `/teams` for list/create and `/teams/{teamId}` for identity, roster, invitations, administration, and future sections. |
| Invitation removal could be mistaken for history deletion | Team Management spec | Reject/cancel delete pending rows, but there is no sporting history before `ACTIVE`. | Keep future invitation history states out of scope and preserve active sporting history rules for later tournament features. |

---

## Tech Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Administration roles | Store only `TEAM_OWNER` and `TEAM_ADMIN` in team administration | Matches authorization rules and avoids using athlete membership as permission. |
| Athlete membership states | Store only `INVITED` and `ACTIVE` in athlete membership | Captures consent-based MVP lifecycle without email delivery. |
| Ownership invariant | Exactly one owner per team, including during transfer | Prevents orphaned or ambiguous team authority. |
| Unauthorized contract | `401` unauthenticated, `403` related but unauthorized, `404` no relationship/private not visible, `422` invalid invariant, `409` state conflict | Removes ambiguous `404 or 403` behavior. |
| Routes | `/teams` list/create and `/teams/{teamId}` detail/manage | Keeps the team experience scalable for tournaments, history, statistics, badges, and achievements. |
| Team names | Not globally unique | Matches MVP decision and existing business ambiguity. |
| Self-join | Explicit `POST /api/v1/teams/{teamId}/athletes/me` for global `ATHLETE` admins | Administration never silently creates athlete membership, but self-consent needs no invitation. |
| UI hierarchy | Team Identity, Current Sporting Context, Athlete Roster, Pending Invitations, Management actions | Keeps the page sports-oriented instead of administration-dominated. |

---

## Requirement Mapping

| Requirement | Design Coverage |
| --- | --- |
| TEAM-01 | Team domain, create API, team persistence |
| TEAM-02 | `listForUser`, list response with independent administration/membership fields |
| TEAM-03 | team detail response, private visibility, `/teams/{teamId}` UI |
| TEAM-04 | `update`, owner/admin edit authorization |
| TEAM-05 | explicit self-join route/service/repository |
| TEAM-06 | `inviteAthlete`, conflict handling, target ATHLETE validation |
| TEAM-07 | accept/reject invitation service and deterministic state errors |
| TEAM-08 | cancel invitation, remove active athlete, independence preservation |
| TEAM-09 | owner-only admin assignment/removal |
| TEAM-10 | atomic ownership transfer to admin or active athlete |
| TEAM-11 | UI chain with DESIGN.md, squadra-design, Stitch, squadra-ui and responsive sports-oriented hierarchy |
| TEAM-12 | deterministic `401`, `403`, `404`, `409`, `422` error strategy |
| TEAM-13 | separate tables/entities/enums and independent capability derivation |
| TEAM-14 | unique keys, transfer transaction, concurrency tests and owner deletion/deactivation guard |
