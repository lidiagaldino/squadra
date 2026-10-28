# Tournament Visibility and Team Invitations Specification

## Problem Statement

Tournament owners need to choose whether teams can request a category registration or must first be invited. Today tournament access is owner-only, yet any team admin who knows a category ID can create a registration. Visibility, discovery, invitation decisions, and capacity must be enforced together by the backend.

## Goals

- [ ] An organizer can set a tournament to `PUBLIC` or `PRIVATE` when creating or editing a draft.
- [ ] Signed in team admins can discover public open tournaments and request category registration.
- [ ] Only a private tournament's owner can invite a team into a specific category.
- [ ] A team's owner or admin can accept or reject its pending invitation.
- [ ] Accepting an invitation creates a category registration; rejecting creates none.
- [ ] Private tournament and invitation data remain scoped to the owner and invited team's admins.
- [ ] UI copy and state labels are in Brazilian Portuguese.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Anonymous public pages | Existing privacy rule requires login for championships. |
| Email, push, or external invitation delivery | No delivery channel is needed for an in-app invitation inbox. |
| Roster, payment, and organizer approval | Existing registration flow ends at `DRAFT`. |
| Invitation expiry | Organizer cancellation and team rejection are explicit lifecycle actions. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Meaning of public | Discoverable to authenticated users, never anonymous | Matches `docs/security-business-constraints.md`. | y |
| Visibility default and migration | `PUBLIC` for new and existing tournaments | Preserves existing team-initiated registration behavior. | y |
| Visibility edits | Owner can change only a `DRAFT` tournament | Avoids invalidating live invitations and registrations. | y |
| Invite target | Tournament owner selects an existing team and category | Registration is category-specific. | y |
| Team lookup | Organizer sees only team ID, display name, and city/state in a paginated picker | Allows invitations without disclosing member/admin PII. | y |
| Invitation timing | Owner can invite only while a private tournament is `OPEN` | Category and registration rules already use `OPEN`. | y |
| Pending capacity | Pending invitations do not reserve a category slot | Capacity is committed on acceptance; a full category returns `409` and leaves invite pending. | y |
| Invitation decision | Team owner or admin may accept/reject; only the owner may cancel a pending invitation | Mirrors contextual team and tournament roles. | y |
| Repeat invitations | At most one pending invitation per team/category; owner may re-invite after rejection/cancellation | Supports corrected invitations without duplicate pending actions. | y |
| Existing registrations | Existing `DRAFT` and later registrations remain valid if tournament visibility is migrated | Avoids data loss. | y |

**Open questions:** none. User authorized completion of the existing specification on 2026-10-28.

---

## User Stories

### P1: Configure Tournament Visibility

**User Story**: As an organizer, I want to choose public or private access when configuring my tournament.

**Acceptance Criteria**:

1. WHEN an organizer creates a tournament without visibility THEN the system SHALL store `PUBLIC` and return it in tournament responses.
2. WHEN a tournament owner saves `PUBLIC` or `PRIVATE` on a draft tournament THEN the system SHALL persist that value and return it on subsequent reads.
3. IF a tournament owner changes visibility after the tournament leaves `DRAFT` THEN the system SHALL return `409` without changing visibility.
4. IF a user who does not own the tournament changes visibility THEN the system SHALL return `404`.
5. IF visibility has a value other than `PUBLIC` or `PRIVATE` THEN the system SHALL return `422`.

**Independent Test**: Create public and private drafts, edit each, publish one, then verify persistence, owner scope, and the post-publish conflict.

### P1: Request Registration in a Public Tournament

**User Story**: As a team admin, I want to find a public tournament and request registration for my team in a category.

**Acceptance Criteria**:

1. WHEN a signed in user lists available tournaments THEN the system SHALL return only `OPEN` `PUBLIC` tournaments with minimal event details in bounded pages.
2. WHEN a signed in user opens a public tournament THEN the system SHALL return its event details and categories.
3. WHEN a `TEAM_OWNER` or `TEAM_ADMIN` requests a public category registration while the tournament is `OPEN` THEN the system SHALL create a `DRAFT` team/category registration and return `201`.
4. IF a team admin directly posts a registration to a private category THEN the system SHALL return `404` without creating a registration.
5. IF a user without administration of the team posts a public registration THEN the system SHALL return `403`.
6. IF the category is full or the team has an active registration THEN the system SHALL return `409` without an extra registration.

**Independent Test**: Find a public open tournament as a team admin, register, and verify a private tournament never appears or accepts a direct request.

### P1: Invite a Team to a Private Category

**User Story**: As a private tournament owner, I want to invite a team into a category.

**Acceptance Criteria**:

1. WHEN the owner invites an existing team to an `OPEN` private tournament category THEN the system SHALL create a `PENDING` invitation for that team/category and return `201`.
2. WHEN the owner searches for teams to invite THEN the system SHALL return bounded results containing only team ID, name, city, and state.
3. IF a non-owner sends or lists category invitations THEN the system SHALL return `404`.
4. IF the tournament is public, not `OPEN`, or the category is outside the tournament THEN the system SHALL return `409` for invalid state or `404` for invalid scope without creating an invitation.
5. IF the team already has an active registration or pending invitation for the category THEN the system SHALL return `409`.
6. WHEN the owner cancels a `PENDING` invitation THEN the system SHALL mark it `CANCELLED` without creating a registration.

**Independent Test**: Search teams, invite one into a private open category, list it as owner, and verify cross-owner and duplicate attempts fail.

### P1: Respond to an Invitation

**User Story**: As a team owner or admin, I want to accept or reject a private tournament invitation for my team.

**Acceptance Criteria**:

1. WHEN a team admin lists their team's invitations THEN the system SHALL return only invitations for that team with tournament/category names and statuses.
2. WHEN a team admin accepts a `PENDING` invitation while its tournament is `OPEN` and category has capacity THEN the system SHALL atomically mark it `ACCEPTED` and create one `DRAFT` registration.
3. WHEN a team admin rejects a `PENDING` invitation THEN the system SHALL mark it `REJECTED` and create no registration.
4. IF a non-admin attempts to list or decide a team invitation THEN the system SHALL return `403`.
5. IF a decision is retried after the invitation is no longer `PENDING` THEN the system SHALL return `409` without a second registration.
6. IF the category is full or the tournament is no longer `OPEN` during acceptance THEN the system SHALL return `409` and leave the invitation `PENDING`.
7. IF concurrent accepts or public registration attempts race for the last category slot THEN the system SHALL persist no more than `maxTeams` active registrations.

**Independent Test**: Accept one invitation, reject another, retry both, and race two accepts for the last slot.

### P1: Private Access and Portuguese UI

**User Story**: As a tournament owner or invited team admin, I want the correct private view and clear controls in Portuguese.

**Acceptance Criteria**:

1. WHILE a tournament is `PRIVATE`, the system SHALL allow only its owner or an invited/registered team's admin to read its event and category details.
2. IF an unrelated signed in user requests private event or category details THEN the system SHALL return `404`.
3. WHEN an owner opens a private tournament category THEN the frontend SHALL show a team picker, invite action, and invitation statuses in Portuguese.
4. WHEN a team admin opens their invitation inbox THEN the frontend SHALL show accept and reject actions for `PENDING` invitations in Portuguese.
5. WHEN a team admin opens a public tournament category THEN the frontend SHALL show the existing team selector and registration request action in Portuguese.
6. WHEN invitation or registration mutation succeeds THEN the frontend SHALL refresh the relevant invitation, category, and team registration views.
7. IF an action fails with `403`, `404`, or `409` THEN the frontend SHALL keep the selected team and show a non-sensitive Portuguese message.
8. IF a request is unauthenticated THEN the system SHALL return `401` without private event or invitation data.

**Independent Test**: Use owner, invited admin, unrelated admin, and unauthenticated sessions across public/private detail screens and invitation inbox.

---

## Edge Cases

| Edge Case | Expected Handling |
| --- | --- |
| Invitation and direct public registration for the same team/category | Visibility rule blocks private direct registration; active-registration uniqueness still protects races. |
| Owner cancels pending invite while team accepts | One serializable outcome; no accepted invite without its registration. |
| Team admin loses admin role before decision | `403` on subsequent decision. |
| Full category at accept time | `409`; invitation stays pending. |
| Rejected invitation followed by owner re-invitation | New pending invitation is allowed; history remains. |
| Existing tournament rows at migration | Backfilled to `PUBLIC`; existing registration rows remain untouched. |

## Requirement Traceability

| ID | Requirement | Story | Priority | Status |
| --- | --- | --- | --- | --- |
| TVIS-01 | Persist and expose visibility | Configure Tournament Visibility | P1 | Verified |
| TVIS-02 | Discover and register in public categories | Request Registration in a Public Tournament | P1 | Pending |
| TVIS-03 | Owner team search and private invitations | Invite a Team to a Private Category | P1 | Implemented |
| TVIS-04 | Team invitation decisions and capacity integrity | Respond to an Invitation | P1 | Implemented |
| TVIS-05 | Private access and Portuguese UI | Private Access and Portuguese UI | P1 | Pending |

**Coverage:** 5 total, 5 mapped to tasks, 0 unmapped.

## Success Criteria

- [ ] Owner can create a public or private tournament and cannot change visibility after publish.
- [ ] Public open tournaments are discoverable to signed in team admins; private ones are not.
- [ ] Only owner can invite; only team admins can accept or reject.
- [ ] Accepted invitation creates exactly one registration within category capacity.
- [ ] Backend and frontend quality gates pass with existing coverage thresholds.
