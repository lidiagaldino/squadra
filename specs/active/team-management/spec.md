# Team Management Specification

## Problem Statement

Squadra needs permanent teams that can exist before tournament registration. Users must be able to create and maintain team identity, delegate permanent team administration, and invite athletes into a consent-based sporting relationship that future tournament registrations can consume.

## Goals

- [ ] Authenticated users can create a team and become its single `TEAM_OWNER`.
- [ ] Team administration uses only `TEAM_OWNER` and `TEAM_ADMIN`.
- [ ] Athlete membership is modeled independently with `INVITED` and `ACTIVE` states.
- [ ] Athletes must explicitly join or accept an invitation before becoming active team athletes.
- [ ] Tournament-specific roles remain separate from permanent team administration and athlete membership.
- [ ] The frontend provides mobile-ready `/teams` and `/teams/{teamId}` screens inside the authenticated app shell.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Tournament registration | Team registration belongs to the tournament-registration feature. |
| Tournament roles | `TEAM_MANAGER`, `CAPTAIN`, `COACH`, and `ASSISTANT_COACH` are contextual to registrations only. |
| Public team pages | This feature manages authenticated private team data only. |
| Team logo/photo upload | Requires storage, scanning, image policy, and media lifecycle decisions. |
| Team deletion | Deletion/history rules need tournament-history requirements. |
| Email delivery | Athlete invitations can exist inside Squadra without email infrastructure. |
| Invitation history states | `REJECTED` and `CANCELLED` persistence states are future scope; MVP removes pending invitations. |
| Cross-team tournament eligibility | The athlete-per-category restriction belongs to registration/roster validation. |
| Tournament history, statistics, badges, achievements | Detail UI should scale for these sections, but this feature does not implement them. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Team creation actor | Any authenticated `ATHLETE` or `ORGANIZER` can create a team | Docs define team administration as contextual and do not restrict team creation by global role. | n |
| Team owner invariant | A team must have exactly one `TEAM_OWNER` at all observable committed states | Prevents orphaned teams and ambiguous authority. | n |
| Creator administration | The creator becomes the initial `TEAM_OWNER` only | Administration does not imply athlete membership. | n |
| Administration and athlete membership | A user may simultaneously be `TEAM_OWNER` plus `ACTIVE` athlete, or `TEAM_ADMIN` plus `ACTIVE` athlete | Administration authorizes management; athlete membership represents sporting participation. | n |
| Self-join | A global `ATHLETE` who administers a team may explicitly join that team as an `ACTIVE` athlete without invitation | The user is consenting on their own behalf. | n |
| Organizer-only self-join | A global `ORGANIZER` without `ATHLETE` capability cannot join an athlete roster | Athlete membership is a sporting relationship. | n |
| Admin assignment | Only `TEAM_OWNER` can assign or remove `TEAM_ADMIN` | `TEAM_ADMIN` assists management but cannot delegate administration. | n |
| Athlete invitation target | Team admins invite existing users who have global role `ATHLETE` | Avoids silent active membership and organizer-only roster membership. | n |
| Invitation rejection/cancellation | Rejecting or cancelling an invitation deletes the pending `INVITED` relationship | There is no sporting history until membership becomes `ACTIVE`. | n |
| Duplicate/invalid state transitions | Duplicate invitation, already active invite, accept twice, reject twice, and accept/reject cancelled invitation use deterministic `409` or `404` as specified below | Removes ambiguous state handling. | n |
| Owner transfer target | Ownership can transfer to an existing `TEAM_ADMIN` or `ACTIVE` athlete member | The target must already have a relationship with the team. | n |
| Owner deletion/deactivation | A unique `TEAM_OWNER` cannot be deleted or deactivated until ownership is transferred | Preserves the exactly-one-owner invariant. | n |
| Team name uniqueness | Team names are not globally unique in MVP | Existing docs do not define a global team namespace. | n |
| Unknown request fields | Unknown fields are rejected with `422` | Deterministic contract prevents silent persistence ambiguity. | n |
| Frontend routes | `/teams` lists teams and starts creation; `/teams/{teamId}` manages selected team detail | Keeps list focused and detail scalable. | n |

**Open questions:** none - all unresolved decisions are logged as assumptions above.

---

## User Stories

### P1: Create Team ⭐ MVP

**User Story**: As an authenticated user, I want to create a team so that I can manage a sporting entity before tournament registration.

**Acceptance Criteria**:

1. WHEN an authenticated user submits a valid team THEN the system SHALL persist the team and return `201` with the saved team.
2. WHEN a team is created THEN the system SHALL assign the creating user as the team's single `TEAM_OWNER`.
3. WHEN a team is created by a global `ATHLETE` THEN the system SHALL NOT automatically create athlete membership.
4. IF an unauthenticated request attempts to create a team THEN the system SHALL return `401` without writing team data.
5. IF `name` is blank, shorter than 2 characters, or longer than 100 characters THEN the system SHALL return `422`.
6. IF `city` is supplied and longer than 80 characters THEN the system SHALL return `422`.
7. IF `state` is supplied and longer than 80 characters THEN the system SHALL return `422`.
8. IF `foundedYear` is supplied and is earlier than 1850 or later than the current year THEN the system SHALL return `422`.
9. IF `description` is supplied and longer than 500 characters THEN the system SHALL return `422`.

**Independent Test**: Sign in, create a team, and verify it returns one `TEAM_OWNER` administration relationship and no athlete membership.

---

### P1: List My Teams ⭐ MVP

**User Story**: As an authenticated user, I want to see teams I administer, actively play for, or am invited to so that I can enter the right team context.

**Acceptance Criteria**:

1. WHEN an authenticated user requests `/teams` THEN the system SHALL return only teams where the user has `TEAM_OWNER`, `TEAM_ADMIN`, `ACTIVE`, or `INVITED`.
2. WHEN a user has no teams THEN the system SHALL return `200` with an empty list.
3. IF an unauthenticated request attempts to list teams THEN the system SHALL return `401`.
4. The system SHALL expose `administrationRole` independently from `athleteMembershipStatus`.
5. WHEN the user has `INVITED` status THEN the frontend SHALL visually distinguish the team from active participation.

**Independent Test**: Verify `/teams` examples for `TEAM_OWNER` only, `TEAM_ADMIN + ACTIVE`, `ACTIVE` only, and `INVITED` only.

---

### P1: View Team Detail ⭐ MVP

**User Story**: As a related team user, I want to view team detail so that I understand the team identity, roster, invitations, and my available actions.

**Acceptance Criteria**:

1. WHEN a related authenticated user opens `/teams/{teamId}` THEN the system SHALL return team identity, administration, athlete memberships, and the viewer's independent capabilities.
2. IF an authenticated user has no relationship to a private team and attempts to view it THEN the system SHALL return `404`.
3. IF an unauthenticated request attempts to view team detail THEN the system SHALL return `401`.
4. The frontend SHALL prioritize Team Identity, Current Sporting Context, Athlete Roster, Pending Invitations, and authorized Management actions in that order.
5. The frontend SHALL provide responsive mobile layouts for `/teams` and `/teams/{teamId}`.

**Independent Test**: Open detail as owner, admin, active athlete, invited athlete, and unrelated user and verify visible data/actions.

---

### P1: Edit Team ⭐ MVP

**User Story**: As a `TEAM_OWNER` or `TEAM_ADMIN`, I want to edit permanent team information so that the team identity stays current.

**Acceptance Criteria**:

1. WHEN a `TEAM_OWNER` submits valid team changes THEN the system SHALL update the team and return `200`.
2. WHEN a `TEAM_ADMIN` submits valid team changes THEN the system SHALL update the team and return `200`.
3. IF an authenticated user has only athlete membership and attempts to edit team information THEN the system SHALL return `403` without updating the team.
4. IF an authenticated user has no relationship to a private team and attempts to edit it THEN the system SHALL return `404` without exposing private team data.
5. IF invalid team information is submitted by an authorized admin THEN the system SHALL return `422` without updating the team.
6. WHEN team information is updated THEN the system SHALL update `updatedAt`.

**Independent Test**: Edit as owner/admin and verify active/invited/unrelated users cannot edit.

---

### P1: Join Own Team as Athlete ⭐ MVP

**User Story**: As a team administrator with global `ATHLETE` role, I want to explicitly join my team's athlete roster so that administration does not silently imply sporting membership.

**Acceptance Criteria**:

1. WHEN a global `ATHLETE` who is `TEAM_OWNER` joins their team roster THEN the system SHALL create an `ACTIVE` athlete membership without invitation.
2. WHEN a global `ATHLETE` who is `TEAM_ADMIN` joins their team roster THEN the system SHALL create an `ACTIVE` athlete membership without invitation.
3. IF the administrator is already `ACTIVE` THEN the system SHALL return `409` and keep one athlete membership.
4. IF a global `ORGANIZER` without `ATHLETE` capability attempts to join the athlete roster THEN the system SHALL return `403`.
5. WHEN self-join succeeds THEN the system SHALL NOT change the user's team administration role.

**Independent Test**: Verify `TEAM_OWNER` only becomes `TEAM_OWNER + ACTIVE` only after explicit self-join.

---

### P1: Invite Athlete ⭐ MVP

**User Story**: As a team administrator, I want to invite athletes so that athlete membership is consent-based.

**Acceptance Criteria**:

1. WHEN a `TEAM_OWNER` invites an existing `ATHLETE` by email THEN the system SHALL create one `INVITED` athlete membership.
2. WHEN a `TEAM_ADMIN` invites an existing `ATHLETE` by email THEN the system SHALL create one `INVITED` athlete membership.
3. IF the target email does not belong to an existing global `ATHLETE` user THEN the system SHALL return `404` with a non-sensitive error.
4. IF a team admin invites an already `INVITED` athlete THEN the system SHALL return `409` and keep one invitation.
5. IF a team admin invites an already `ACTIVE` athlete THEN the system SHALL return `409` without changing the active membership.
6. IF an athlete member without team administration attempts to invite athletes THEN the system SHALL return `403`.

**Independent Test**: Invite an existing athlete as owner/admin and verify duplicate and already-active cases return `409`.

---

### P1: Accept or Reject Invitation ⭐ MVP

**User Story**: As an invited athlete, I want to accept or reject a team invitation so that I control whether I become an active team athlete.

**Acceptance Criteria**:

1. WHEN an invited athlete accepts an invitation THEN the system SHALL change that athlete membership to `ACTIVE`.
2. WHEN an invited athlete rejects an invitation THEN the system SHALL delete the pending `INVITED` membership.
3. IF an athlete accepts an already accepted invitation THEN the system SHALL return `409`.
4. IF an athlete rejects an already accepted invitation THEN the system SHALL return `409`.
5. IF an athlete accepts a cancelled or previously rejected invitation THEN the system SHALL return `404`.
6. IF an athlete rejects a cancelled or previously rejected invitation THEN the system SHALL return `404`.
7. IF an athlete without a matching `INVITED` membership attempts to accept or reject THEN the system SHALL return `404`.
8. WHEN concurrent invitation acceptance requests occur THEN the system SHALL persist one `ACTIVE` membership and return one successful transition.

**Independent Test**: Invite, accept, verify `ACTIVE`; invite, reject, verify no relationship; repeat edge cases and verify deterministic statuses.

---

### P1: Manage Athlete Roster ⭐ MVP

**User Story**: As a team administrator, I want to cancel pending invitations and remove active athletes so that the roster remains accurate.

**Acceptance Criteria**:

1. WHEN a `TEAM_OWNER` or `TEAM_ADMIN` cancels an invitation THEN the system SHALL delete the pending `INVITED` membership.
2. WHEN a `TEAM_OWNER` or `TEAM_ADMIN` removes an active athlete THEN the system SHALL delete the `ACTIVE` athlete membership.
3. IF a team admin cancels an already accepted invitation THEN the system SHALL return `409` and keep the active membership.
4. IF concurrent invitation cancellation and acceptance occur THEN the system SHALL persist either no athlete membership or one `ACTIVE` membership, never duplicate memberships.
5. IF an athlete member without team administration attempts to cancel invitations or remove athletes THEN the system SHALL return `403`.
6. WHEN an athlete who is also `TEAM_ADMIN` is removed from the athlete roster THEN the system SHALL preserve `TEAM_ADMIN`.
7. WHEN a `TEAM_OWNER + ACTIVE` removes themselves from the athlete roster THEN the system SHALL preserve `TEAM_OWNER`.

**Independent Test**: Remove `ACTIVE` from `TEAM_ADMIN + ACTIVE` and verify admin remains; remove self athlete membership from `TEAM_OWNER + ACTIVE` and verify owner remains.

---

### P1: Manage Team Administrators ⭐ MVP

**User Story**: As a `TEAM_OWNER`, I want to manage team admins so that permanent administration is explicit.

**Acceptance Criteria**:

1. WHEN a `TEAM_OWNER` assigns an existing user as `TEAM_ADMIN` THEN the system SHALL create or update that user's team administration role as `TEAM_ADMIN`.
2. WHEN a `TEAM_OWNER` removes a `TEAM_ADMIN` THEN the system SHALL delete that user's team administration relationship.
3. IF a `TEAM_ADMIN` attempts to assign, promote, remove, or demote another administrator THEN the system SHALL return `403`.
4. IF an athlete member without team administration attempts to assign or remove administrators THEN the system SHALL return `403`.
5. IF a request attempts to assign `MEMBER`, `INVITED`, `ACTIVE`, `CAPTAIN`, `COACH`, `ASSISTANT_COACH`, or `TEAM_MANAGER` as a team administration role THEN the system SHALL return `422`.
6. IF a request attempts to remove or demote the only `TEAM_OWNER` directly THEN the system SHALL return `422`.
7. WHEN `TEAM_ADMIN` is removed from a user who is also `ACTIVE` THEN the system SHALL preserve the `ACTIVE` athlete membership.

**Independent Test**: Assign/remove admin while preserving independent athlete membership.

---

### P1: Transfer Ownership ⭐ MVP

**User Story**: As a `TEAM_OWNER`, I want to transfer ownership so that the team always has exactly one accountable owner.

**Acceptance Criteria**:

1. WHEN a `TEAM_OWNER` transfers ownership to an existing `TEAM_ADMIN` THEN the system SHALL atomically make the target the only `TEAM_OWNER` and demote the previous owner to `TEAM_ADMIN`.
2. WHEN a `TEAM_OWNER` transfers ownership to an `ACTIVE` athlete without administration THEN the system SHALL create or update target administration as `TEAM_OWNER` and preserve target `ACTIVE` membership.
3. WHEN ownership transfer succeeds for a previous owner who is also `ACTIVE` THEN the system SHALL preserve previous owner `ACTIVE` membership.
4. IF a `TEAM_ADMIN` attempts ownership transfer THEN the system SHALL return `403`.
5. IF an athlete member without team administration attempts ownership transfer THEN the system SHALL return `403`.
6. IF the target has no team relationship THEN the system SHALL return `422`.
7. IF concurrent ownership transfers occur THEN the system SHALL preserve exactly one `TEAM_OWNER` at all observable committed states.

**Independent Test**: Transfer to `TEAM_ADMIN` and to `ACTIVE` athlete without admin; verify exactly one owner and all athlete memberships unchanged.

---

### P1: Team UI and UX ⭐ MVP

**User Story**: As an authenticated user, I want sports-oriented team screens so that team identity and athlete roster feel primary while management actions stay available to authorized users.

**Acceptance Criteria**:

1. WHEN an authenticated user opens `/teams` THEN the frontend SHALL show the user's teams or an empty-state create prompt.
2. WHEN a user creates a team successfully from `/teams` THEN the frontend SHALL add the team to the visible list without a full page reload.
3. WHEN a user opens `/teams/{teamId}` with `TEAM_OWNER` or `TEAM_ADMIN` administration THEN the frontend SHALL show editable team identity.
4. WHEN a user opens `/teams/{teamId}` with only `ACTIVE` athlete membership THEN the frontend SHALL show team identity read-only.
5. WHEN a user opens `/teams/{teamId}` with `INVITED` athlete membership THEN the frontend SHALL show that this is an invitation, not active participation, plus accept and reject actions.
6. WHEN a `TEAM_OWNER` opens `/teams/{teamId}` THEN the frontend SHALL show admin assignment and ownership transfer controls.
7. WHEN a `TEAM_OWNER` or `TEAM_ADMIN` opens `/teams/{teamId}` THEN the frontend SHALL show athlete invitation, invitation cancellation, and active athlete removal controls.
8. The frontend SHALL derive capabilities from administration role and athlete membership status independently.
9. The frontend SHALL validate role combinations for `TEAM_OWNER` only, `TEAM_OWNER + ACTIVE`, `TEAM_ADMIN` only, `TEAM_ADMIN + ACTIVE`, `ACTIVE` only, `INVITED` only, and no relationship.
10. IF load or save fails after one refresh-token recovery attempt THEN the frontend SHALL show a non-sensitive error and preserve typed values.
11. WHILE the user is authenticated THEN the Home sidebar SHALL link Teams to `/teams`.
12. The implementation SHALL use Business Rules, `DESIGN.md`, `squadra-design`, Stitch MCP exploration when available, and `squadra-ui` before implementing significant team screens.
13. The frontend SHALL NOT let Stitch or visual design invent permissions, membership behavior, invitation rules, or tournament rules.

**Independent Test**: Render `/teams` and `/teams/{teamId}` across all required role combinations and verify visible controls and invitation language.

---

### P1: Authorization and Privacy ⭐ MVP

**User Story**: As a Squadra user, I want private teams and deterministic authorization so that team data is not leaked or mutated by the wrong actor.

**Acceptance Criteria**:

1. IF a request is unauthenticated THEN the system SHALL return `401`.
2. IF an authenticated related user lacks permission for an operation THEN the system SHALL return `403`.
3. IF an authenticated user has no relationship to a private team or invitation THEN the system SHALL return `404`.
4. IF a syntactically valid request violates validation or a business invariant THEN the system SHALL return `422`.
5. IF a requested state transition conflicts with current state THEN the system SHALL return `409`.
6. IF a request includes unknown fields THEN the system SHALL return `422` without persisting unknown data.

**Independent Test**: Exercise create/list/detail/edit/admin/invitation routes for unauthenticated, unrelated, related-unauthorized, invalid, and conflicting requests.

---

### P1: Administration and Membership Independence ⭐ MVP

**User Story**: As a user who may administer and play for the same team, I want those relationships to remain independent so that management changes do not erase sporting participation and roster changes do not erase authority.

**Acceptance Criteria**:

1. WHEN athlete membership changes THEN the system SHALL NOT implicitly change team administration.
2. WHEN team administration changes THEN the system SHALL NOT implicitly change athlete membership.
3. WHEN `TEAM_ADMIN + ACTIVE` loses `ACTIVE` membership THEN the system SHALL keep `TEAM_ADMIN`.
4. WHEN `TEAM_ADMIN + ACTIVE` loses `TEAM_ADMIN` administration THEN the system SHALL keep `ACTIVE`.
5. WHEN `TEAM_OWNER + ACTIVE` loses `ACTIVE` membership THEN the system SHALL keep `TEAM_OWNER`.
6. WHEN `TEAM_OWNER` explicitly joins the roster THEN the system SHALL result in `TEAM_OWNER + ACTIVE`.
7. WHEN `TEAM_OWNER + ACTIVE` transfers ownership THEN the system SHALL result in previous owner `TEAM_ADMIN + ACTIVE`.

**Independent Test**: Verify all listed combination transitions in service and API tests.

---

### P1: Concurrency and Domain Invariants ⭐ MVP

**User Story**: As Squadra, I want team invariants preserved during concurrent actions so that teams remain consistent under retries and races.

**Acceptance Criteria**:

1. IF two create requests for the same user run concurrently THEN the system SHALL create two separate teams and assign the creator as owner of each.
2. IF two invitation updates race for the same team/athlete THEN the system SHALL persist at most one athlete membership row with a valid final state.
3. IF two ownership transfers race THEN the system SHALL preserve exactly one `TEAM_OWNER`.
4. IF a user is the unique `TEAM_OWNER` of a team THEN the system SHALL prevent deletion or deactivation of that user until ownership is transferred.
5. The system SHALL enforce one administration row per `(team_id, user_id)`.
6. The system SHALL enforce one athlete membership row per `(team_id, athlete_user_id)`.

**Independent Test**: Run repository/service tests for duplicate create, concurrent invitation transition, concurrent ownership transfer, and account deletion/deactivation guard.

---

## Edge Cases

- Duplicate invitation SHALL return `409` and keep one `INVITED` membership.
- Inviting an already `ACTIVE` athlete SHALL return `409` and keep the `ACTIVE` membership.
- Accepting an invitation twice SHALL return `409` on the second accept.
- Rejecting an already accepted invitation SHALL return `409`.
- Accepting a cancelled invitation SHALL return `404`.
- Rejecting a cancelled invitation SHALL return `404`.
- Cancelling an already accepted invitation SHALL return `409`.
- Concurrent accept/cancel SHALL leave either no athlete membership or one `ACTIVE` membership, never duplicate rows.
- Removing `ACTIVE` from `TEAM_ADMIN + ACTIVE` SHALL preserve `TEAM_ADMIN`.
- Removing `TEAM_ADMIN` from `TEAM_ADMIN + ACTIVE` SHALL preserve `ACTIVE`.
- `TEAM_OWNER + ACTIVE` leaving the athlete roster SHALL preserve `TEAM_OWNER`.
- Direct owner removal/demotion SHALL return `422`; ownership transfer is required.
- Transfer to `ACTIVE` athlete SHALL create/update administration as `TEAM_OWNER` while preserving `ACTIVE`.
- Transfer to `TEAM_ADMIN` SHALL make the target the only `TEAM_OWNER`.
- Unknown request fields SHALL return `422`.
- No public endpoint SHALL expose private teams in MVP.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| TEAM-01 | P1: Create Team | Design | Pending |
| TEAM-02 | P1: List My Teams | Design | Pending |
| TEAM-03 | P1: View Team Detail | Design | Pending |
| TEAM-04 | P1: Edit Team | Design | Pending |
| TEAM-05 | P1: Join Own Team as Athlete | Design | Pending |
| TEAM-06 | P1: Invite Athlete | Design | Pending |
| TEAM-07 | P1: Accept or Reject Invitation | Design | Pending |
| TEAM-08 | P1: Manage Athlete Roster | Design | Pending |
| TEAM-09 | P1: Manage Team Administrators | Design | Pending |
| TEAM-10 | P1: Transfer Ownership | Design | Pending |
| TEAM-11 | P1: Team UI and UX | Design | Pending |
| TEAM-12 | P1: Authorization and Privacy | Design | Pending |
| TEAM-13 | P1: Administration and Membership Independence | Design | Pending |
| TEAM-14 | P1: Concurrency and Domain Invariants | Design | Pending |

**Coverage:** 14 total, 0 mapped to tasks, 14 unmapped.

---

## Success Criteria

- [ ] Authenticated users can create teams and become the single `TEAM_OWNER`.
- [ ] Team creators are not automatically `ACTIVE` athlete members.
- [ ] `TEAM_OWNER` and `TEAM_ADMIN` can edit team information.
- [ ] `TEAM_OWNER` can assign/remove admins and transfer ownership atomically.
- [ ] Team admins can invite athletes, cancel invitations, and remove active athletes.
- [ ] Athletes explicitly join or accept before becoming `ACTIVE` members.
- [ ] Administration and athlete membership remain independent across all combination states.
- [ ] Permanent team administration rejects athlete membership statuses and tournament-only roles.
- [ ] Backend tests prove ownership, authorization, validation, invitation lifecycle, roster management, and invariants.
- [ ] Frontend tests prove list/detail routes, create/edit, role-specific controls, invitation actions, failure states, auth recovery, and mobile-ready UI.
- [ ] Backend coverage remains at or above 80%, with critical Team Management domain and authorization logic targeting at least 90% where practical.
- [ ] Frontend checks pass.
