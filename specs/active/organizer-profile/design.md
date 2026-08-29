# Organizer Profile Design

**Spec**: `specs/active/organizer-profile/spec.md`
**Status**: Draft

---

## Architecture Overview

Organizer Profile is implemented as an authenticated organizer-owned "me" resource. The frontend never sends a user ID for ownership; the API derives ownership from the verified access token and permits only `ORGANIZER`.

```mermaid
flowchart TD
    UI[/organizer/profile screen/] --> Client[frontend organizer-profile API client]
    Client --> Api[api/v1/organizers/me/profile]
    Api --> App[application/services/interfaces + impl]
    App --> Domain[domain entities/repositories]
    App --> Repo[infra/db/repositories]
    Repo --> Db[(PostgreSQL organizer_profiles)]
    Api --> Auth[api/filter RequiresRoles ORGANIZER]
    Home[/home shell/] --> ProfileLink[/organizer/profile]
```

---

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
| --- | --- | --- |
| Access-token role filter | `backend/src/main/java/com/squadra/platform/api/filter/AccessTokenAuthorizationFilter.java` | Protect organizer profile endpoints with `@RequiresRoles(ORGANIZER)` and read the verified token from request context. |
| Authentication JWT claims | `backend/src/main/java/com/squadra/platform/domain/services/AccessTokenVerifier.java` | Use `VerifiedAccessToken.userId()` as the only owner ID source. |
| Athlete profile architecture | `backend/src/main/java/com/squadra/platform/api/v1/athletes/profile` and `frontend/src/features/athlete-profile` | Mirror the already proven "me profile" vertical slice for organizer profile. |
| Layer-first backend structure | `backend/src/main/java/com/squadra/platform` | Add files only under responsibility folders, not layer roots. |
| JDBC adapter pattern | `backend/src/main/java/com/squadra/platform/infra/db/adapters/ReactiveJdbcSupport.java` | Reuse the current worker-isolated repository pattern used by athlete profiles. |
| Auth context | `frontend/src/features/auth/auth-context.tsx` | Use access token and refresh retry for authenticated profile requests. |
| React Query provider | `frontend/src/providers.tsx` | Load and mutate organizer profile server state with TanStack Query. |
| App shell styling | `frontend/src/app/home/page.tsx` and `frontend/src/app/globals.css` | Reuse the authenticated sidebar and Squadra visual language. |

### Integration Points

| System | Integration Method |
| --- | --- |
| Authentication | `Authorization: Bearer <accessToken>` plus `@RequiresRoles(UserRole.ORGANIZER)` on backend. |
| Database | New Flyway migration `V4__create_organizer_profiles.sql` creates `squadra.organizer_profiles`. |
| Frontend routing | New App Router page at `frontend/src/app/organizer/profile/page.tsx`. |
| Home navigation | Organizer users see `Profile` link route to `/organizer/profile`; athlete users continue to `/athlete/profile`. |

---

## Components

### OrganizerProfileResource

- **Purpose**: Owns HTTP transport for reading and saving the authenticated organizer's profile.
- **Location**: `backend/src/main/java/com/squadra/platform/api/v1/organizers/profile/OrganizerProfileResource.java`
- **Interfaces**:
  - `get(ContainerRequestContext): Uni<Response>` returns current organizer profile or `404`.
  - `put(ProfileUpsertRequest, ContainerRequestContext): Uni<ProfileResponse>` upserts current organizer profile.
- **Dependencies**: `OrganizerProfileService`, `VerifiedAccessToken`.
- **Reuses**: `@RequiresRoles`, request-context access-token property, athlete profile DTO/resource style.

### OrganizerProfileService

- **Purpose**: Orchestrates profile retrieval, validation and upsert for the current organizer.
- **Location**:
  - Interface: `backend/src/main/java/com/squadra/platform/application/services/interfaces/OrganizerProfileService.java`
  - Implementation: `backend/src/main/java/com/squadra/platform/application/services/impl/DefaultOrganizerProfileService.java`
- **Interfaces**:
  - `Uni<Optional<OrganizerProfile>> findMine(UUID userId)`
  - `Uni<OrganizerProfile> saveMine(UUID userId, SaveOrganizerProfileCommand command)`
- **Dependencies**: `OrganizerProfileRepository`.
- **Reuses**: Existing application service split and Mutiny use-case style.

### OrganizerProfile Domain Model

- **Purpose**: Represents organizer-owned organization information used by tournaments.
- **Location**:
  - Entity: `backend/src/main/java/com/squadra/platform/domain/entities/OrganizerProfile.java`
  - Repository contract: `backend/src/main/java/com/squadra/platform/domain/repositories/OrganizerProfileRepository.java`
  - Exception: `backend/src/main/java/com/squadra/platform/domain/exceptions/OrganizerProfileNotFoundException.java`
- **Interfaces**:
  - `OrganizerProfile.create(...)`
  - `OrganizerProfile.update(...)`
  - Validation embedded in constructor/update methods.
- **Dependencies**: Pure Java only.
- **Reuses**: Existing domain entity style from athlete profile.

### JdbcOrganizerProfileRepository

- **Purpose**: Persists organizer profiles with one row per organizer user.
- **Location**: `backend/src/main/java/com/squadra/platform/infra/db/repositories/JdbcOrganizerProfileRepository.java`
- **Interfaces**:
  - `Uni<Optional<OrganizerProfile>> findByUserId(UUID userId)`
  - `Uni<OrganizerProfile> upsert(OrganizerProfile profile)`
- **Dependencies**: `DataSource`, `ReactiveJdbcSupport`.
- **Reuses**: Existing repository adapter pattern and explicit timestamp binding.

### Organizer Profile Frontend Feature

- **Purpose**: Provides the organizer-owned profile form and authenticated API client.
- **Location**:
  - API client: `frontend/src/features/organizer-profile/api.ts`
  - Form: `frontend/src/features/organizer-profile/form.tsx`
  - Page: `frontend/src/app/organizer/profile/page.tsx`
- **Interfaces**:
  - `getProfile(auth): Promise<OrganizerProfile | null>`
  - `saveProfile(auth, input): Promise<OrganizerProfile>`
  - `OrganizerProfileForm`
- **Dependencies**: `useAuth`, TanStack Query, React Hook Form, Zod.
- **Reuses**: Athlete profile API retry pattern and authenticated app shell.

---

## Data Models

### Backend Domain

```java
class OrganizerProfile {
    UUID id;
    UUID userId;
    String organizationName;
    String contactName;
    EmailAddress contactEmail;
    String phone;
    String city;
    String state;
    String website;
    String description;
    Instant createdAt;
    Instant updatedAt;
}
```

### Database

```sql
CREATE TABLE squadra.organizer_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES squadra.users(id) ON DELETE CASCADE,
    organization_name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(320) NOT NULL,
    phone VARCHAR(30),
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    website VARCHAR(200),
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

### Frontend Types

```typescript
interface OrganizerProfile {
  id: string;
  organizationName: string;
  contactName: string;
  contactEmail: string;
  phone?: string;
  city: string;
  state: string;
  website?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}
```

---

## API Contract

| Method | Path | Auth | Behavior |
| --- | --- | --- | --- |
| `GET` | `/api/v1/organizers/me/profile` | ORGANIZER | Returns current profile or `404 ORGANIZER_PROFILE_NOT_FOUND`. |
| `PUT` | `/api/v1/organizers/me/profile` | ORGANIZER | Creates or updates current organizer profile and returns `200`. |

Compatibility classification: ADDITIVE.

---

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
| --- | --- | --- |
| Missing/invalid token | Existing filter returns `401`. | User sees sign-in recovery/error flow. |
| Wrong global role | Existing filter returns `403`. | Athlete cannot access organizer-only profile. |
| Profile missing on GET | API returns `404 ORGANIZER_PROFILE_NOT_FOUND`. | UI shows create-profile empty state. |
| Invalid profile data | API returns `422 VALIDATION_FAILED`. | UI keeps typed values and shows validation guidance. |
| DB uniqueness race | Repository upsert keeps one row by `user_id`. | Last valid save wins without duplicate rows. |

---

## Risks & Concerns

| Concern | Location | Impact | Mitigation |
| --- | --- | --- | --- |
| Current persistence uses worker-isolated JDBC rather than Hibernate Reactive | `backend/src/main/java/com/squadra/platform/infra/db/adapters/ReactiveJdbcSupport.java` | New DB work follows existing pattern but is not the preferred long-term reactive persistence stack. | Keep repository API reactive and isolated on worker pool, add no event-loop blocking, and keep the adapter swappable for Hibernate Reactive later. |
| Frontend access token only lives in memory | `frontend/src/features/auth/auth-context.tsx` | Direct page refresh may require refresh before organizer profile API calls can succeed. | Organizer profile API hook performs one refresh-token recovery before surfacing auth failure. |
| Organization profile contains contact information | `docs/security-business-constraints.md` | Accidental public exposure could reveal private contact details before tournament publication rules exist. | Keep endpoint authenticated and organizer-only; do not add public endpoint in this feature. |

---

## Tech Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Ownership API | `/organizers/me/profile` | Prevents trusting client-supplied user IDs and matches athlete profile precedent. |
| Save semantic | Idempotent `PUT` upsert | One endpoint supports create/edit and safe retries. |
| Profile visibility | Private in MVP | Tournament consumption/publication rules are not yet implemented. |
| Persistence style | Mirror current JDBC repository adapter | Keeps implementation consistent with existing athlete profile until persistence stack is upgraded. |
