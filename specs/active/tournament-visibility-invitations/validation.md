# Tournament visibility and invitations validation

**Verdict: PASS**

**Date:** 2026-10-30. **Verifier:** independent delegated verifier (not implementation author).
**Review scope:** `670fe02..64711c2` (initial review `670fe02..64f40ef`, corrected and re-verified through `64711c2`). Prerequisite categories/registration files were inspected where they enforce TVIS behavior.

## Task completion

T1–T7 implementation inspected. T8 verification evidence is recorded here; final business-document and state bookkeeping belongs to the orchestrator.

## Spec-anchored acceptance evidence

Rows follow the five stories and their numbered criteria in `spec.md`. Assertions were independently read, not inferred from test names.

| Criterion | Expected outcome | File:line and assertion evidence | Result |
| --- | --- | --- | --- |
| Visibility 1 | Omitted visibility creates PUBLIC | `backend/src/test/java/com/squadra/platform/api/v1/tournaments/TournamentsResourceTest.java:30` — `body("visibility", equalTo("PUBLIC"))` | PASS |
| Visibility 2 | Draft PUBLIC/PRIVATE edits persist | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:315` — `GET body("visibility", equalTo("PUBLIC")); final GET PRIVATE at :323` | PASS |
| Visibility 3 | Published change returns 409; remains PRIVATE | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:322` — `statusCode(409); :323 body("visibility", equalTo("PRIVATE"))` | PASS |
| Visibility 4 | Foreign visibility edit is 404 | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:319` — `statusCode(404)` | PASS |
| Visibility 5 | Invalid visibility is 422 | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:203` — `statusCode(422) for SECRET` | PASS |
| Public 1 | Only OPEN/PUBLIC; bounded disjoint pages | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:346` — `everyItem(equalTo("OPEN")); :347 everyItem(equalTo("PUBLIC")); :349 size=1; :352 assertNotEquals(first, second)` | PASS |
| Public 2 | Public overview includes event and category | `backend/src/test/java/com/squadra/platform/api/v1/tournaments/TournamentsResourceTest.java:177` — `body("tournament.visibility", equalTo("PUBLIC")); :178 categories.id hasItem(categoryId)` | PASS |
| Public 3 | Admin public request creates DRAFT, 201 | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TeamRegistrationsResourceTest.java:31` — `statusCode(201); :34 teamId; :38 categoryId; :40 status DRAFT` | PASS |
| Public 4 | Private direct request rejected, 404 | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:58` — `statusCode(404); private visibility guard precedes insert` | PASS |
| Public 5 | Non-admin registration is 403 | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TeamRegistrationsResourceTest.java:165` — `statusCode(403); :166 code TEAM_FORBIDDEN; :171 size=0` | PASS |
| Public 6 | Duplicate/full request is 409; capacity preserved | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TeamRegistrationsResourceTest.java:101` — `statusCode(409); :110 statusCode(409); :138 size=2 in race test` | PASS |
| Invite 1 | Owner creates PENDING invitation, 201 | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:36` — `statusCode(201); :37 status PENDING and teamId` | PASS |
| Invite 2 | Directory only id/name/city/state and bounded pages | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:236` — `assertEquals(Set.of("id", "name", "city", "state"), item.keySet()); :234 size=1; :241 assertNotEquals; :243 size=0` | PASS |
| Invite 3 | Non-owner send/list is 404 | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:130` — `statusCode(404) send; :76 statusCode(404) list` | PASS |
| Invite 4 | Public/not-open 409; foreign category 404 | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:210` — `statusCode(409); :216 statusCode(409); :218 statusCode(404)` | PASS |
| Invite 5 | Active registration or pending invitation conflicts | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:223` — `statusCode(409); :75 duplicate statusCode(409); :377 assertThrows(TournamentInvitationConflictException.class, repository.create)` | PASS |
| Invite 6 | Cancel becomes CANCELLED and creates no registration | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:109` — `body("status", equalTo("CANCELLED")); :111 size=0` | PASS |
| Respond 1 | Inbox team scope, names and statuses | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:249` — `size=1 and [0].id firstInvite; :250 tournamentName/categoryName; :251 teamId/status` | PASS |
| Respond 2 | Accept atomically creates one DRAFT registration | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:49` — `status ACCEPTED; :51 categoryId/status DRAFT; :55 size=1; :275 race registration count matches accepted outcome` | PASS |
| Respond 3 | Reject becomes REJECTED; zero registrations | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:146` — `status REJECTED; :148 size=0` | PASS |
| Respond 4 | Non-admin inbox/decisions return 403 | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:135` — `statusCode(403); :138 both decisions statusCode(403); :301 revoked admin inbox403` | PASS |
| Respond 5 | Terminal retries return409 without extra registration | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:53` — `statusCode(409); :55 size=1; :147 reject retry409` | PASS |
| Respond 6 | Full/closed acceptance conflicts and stays PENDING | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:105` — `statusCode(409); :107 PENDING; :144 closed409; :145 PENDING` | PASS |
| Respond 7 | Concurrent accepts/public requests stay within maxTeams | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:167` — `assertEquals(Set.of(200,409), Set.of(first.join(),second.join())); :169 size=2; public counterpart TeamRegistrationsResourceTest.java:134-138` | PASS |
| Private/UI 1 | Owner/invited or registered team admin can read | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:41` — `PRIVATE and categories.id; :299 registered TEAM_ADMIN overview200; :323 owner read PRIVATE` | PASS |
| Private/UI 2 | Unrelated private overview/category data returns404 | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:44` — `statusCode(404); :113 cancelled invite404; :303 removed administrator404` | PASS |
| Private/UI 3 | Private owner picker/action/status Portuguese | `frontend/src/features/tournaments/invitations.test.tsx:35` — `findByText("Pendente").toBeInTheDocument(); :33 Equipe convidada; :34 Convidar equipe; tournament-detail.test.tsx:53 private screen` | PASS |
| Private/UI 4 | PENDING inbox offers Portuguese accept/reject | `frontend/src/features/tournaments/invitations.test.tsx:61` — `getByRole(button, action===accept ? "Aceitar convite" : "Recusar convite"); :62 terminal label; :63 controls absent` | PASS |
| Private/UI 5 | Public category selector and registration action | `frontend/src/features/tournaments/tournament-discovery.test.tsx:39` — `findByRole(button, {name:"Inscrever equipe"}); :43 registerTeam(t1,c1,team1); :50 getByLabelText("Time")` | PASS |
| Private/UI 6 | Mutations refresh invitation/category/registration state | `frontend/src/features/tournaments/invitations.test.tsx:62` — `terminal status rendered; :64 invalidate team-registrations; :65 invalidate tournament-categories; tournament-discovery.test.tsx:42 registered state` | PASS |
| Private/UI 7 | 403/404/409 preserve selection and show safe Portuguese errors | `frontend/src/features/tournaments/invitations.test.tsx:46` — `it.each([403,404,409]); :52 alert actionError; :53 team1 preserved; :54 secret absent; tournament-discovery.test.tsx:52-53 registration409` | PASS |
| Private/UI 8 | Unauthenticated requests401 without private payload | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:127` — `GET category401; :128 POST401; :129 inbox401; :137 decision401; :324 overview401; frontend visibility-routes.test.ts:41 no data` | PASS |

## Review findings and corrections

The initial review found missing exact-outcome coverage for invalid visibility, persisted edits, discovery filtering/pagination, minimal team projection, invitation scope/state conflicts, inbox isolation, revoked administration, and cancellation racing acceptance. The author added behavioral integration assertions; all resulting test cases map to the acceptance table or the listed race/access edge cases.

A concurrency defect was identified in invitation creation: the service checked active registration before repository insertion, while acceptance could commit an active registration and clear the old pending marker between those operations. The repository now acquires the shared category lock, rechecks active registration, and inserts inside one transaction. `backend/src/main/java/com/squadra/platform/infra/db/repositories/JdbcTournamentInvitationRepository.java:32` shares the acceptance lock order; its rollback preserves failed writes. The deterministic stale-prechecked repository assertion and concurrent request test prove the correction, rather than relying solely on whether the race happens during a test run.

## Edge cases and security/concurrency review

- Acceptance, cancellation and rejection use a conditional PENDING update; acceptance locks category then invitation and commits registration and decision together. `TournamentInvitationsResourceTest.java:270` and `:275` verify one successful competing decision with matching persisted registration count.
- Pending invitations do not reserve capacity: the full-category scenario creates more invitations than slots, then rejects acceptance with409 and keeps PENDING (`backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:105`).
- Losing team administration is re-evaluated on later requests (`backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:301`).
- Rejection permits a new invitation and preserves history in storage (`backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:81`).
- Migration backfills PUBLIC and preserves active and historical registrations: `backend/src/test/java/com/squadra/platform/infra/db/TournamentVisibilityMigrationTest.java:28` asserts PUBLIC, `:33` CANCELLED, `:36` DRAFT, and `:38` no extra rows. Constraint tests verify non-null active/pending markers.
- Private reads are scoped through owner/invitation/registration and current team administration; unrelated and revoked identities receive404. BFF routes use the authenticated session helper and return safe failure payloads.

## Discrimination sensors

Scratch archive: `/private/tmp/squadra-tvis-verifier`, created from commit64f40ef. No git stash and no real source mutation were used. Build gates passed before mutation.

| Mutation | Behavior | Result | Killing evidence |
| --- | --- | --- | --- |
| Capacity `>= maxTeams` changed to `> maxTeams` | Allowed one registration beyond capacity | KILLED | `backend/src/test/java/com/squadra/platform/api/v1/registrations/TournamentInvitationsResourceTest.java:102` in original snapshot: expected409, got200; public capacity and concurrent tests also failed. 12 targeted tests: 3 failures, 1 error caused by duplicate success statuses. |
| Refresh key `team-registrations` replaced with nonexistent key | Removed team-registration refresh after accept/reject | KILLED | `frontend/src/features/tournaments/invitations.test.tsx:64`: missing expected invalidation; both parameterized decisions fail (2 failed, 7 passed). |

Both scratch files were restored from scratch-only backups. Real porcelain was captured before/after: the only intervening changes were the orchestrator's explicitly coordinated test/repository/docs changes. Existing `.agents` changes, frontend build metadata, and untracked k6 files were preserved; no verifier mutation leaked to the real tree. Full porcelain cannot be byte-identical during coordinated implementation; source restoration was verified directly in scratch.

## Gates and test integrity

Commands ran against scratch copies with Java21 and Maven repository `/private/tmp/squadra-m2`. Backend uses `mvn -Dquarkus.http.test-port=0 verify -q`; frontend uses `npm run build` then `npm run check`. Build-first generates Next route type declarations required by the clean checkout's typecheck. The initial sandbox HTTP bind denial was environmental and was rerun outside the sandbox; it is not counted as a product failure.

- Original review snapshot: backend120 passed, zero failed/errors/skipped; frontend129 passed; lint/typecheck/build passed.
- Final corrected backend independently executed: **127 passed, 0 failed, 0 errors, 0 skipped**. Scratch repository and test bytes were compared with commit64711c2 and matched. Final production-file changes are limited to the reviewed race fix; frontend files remain the successfully checked snapshot.
- Final PostgreSQL run: orchestrator confirmed exit0, 127 passed, 0 failures/errors/skipped and 95.36% line coverage. Verifier inspected `/private/tmp/squadra-postgres-verify.log`; this is supporting production-dialect evidence, distinguished from the independently executed scratch gate.
- Before-feature backend commit670fe02 contained86 `@Test` methods; original reviewed snapshot had120, corrected snapshot127 (+41 versus the pre-feature baseline). No test deletion or weakened assertion found in the scoped diff. Frontend executed baseline count before the feature was not independently measured; no test deletions were found in the diff.
- Detailed logs: `/private/tmp/squadra-tvis-verifier/{backend-gate,backend-final-gate,frontend-gate,build-gate,sensor-capacity,sensor-refresh}.log`.

## Quality and limitations

Scope/pattern checks passed: explicit domain invitation transitions, scoped service authorization, parameterized SQL, migration-backed uniqueness, category lock for capacity, atomic invitation acceptance, and reused React Query refresh handling. Prerequisite feature files explain the broad diff. Tests cover meaningful state, authorization, API, persistence and UI outcomes. Guideline applied: `.agents/skills/squadra-testing/SKILL.md`. No unrelated production refactor was required by verification.

This report establishes automated acceptance coverage, not completed human visual UAT. No interactive UAT was performed by this verifier. PostgreSQL gate logs were inspected separately from the scratch H2 gate; the orchestrator owns final environment cleanup.

Review signal for lesson distillation: enforce active-registration checks under the same category transaction as invitation insertion; response-status tests should also assert persisted state and absence of unintended writes. The orchestrator must record these lessons because this verifier was authorized to write only this report.

## Final result

**PASS: 32/32 acceptance criteria have outcome evidence; 2/2 targeted behavioral mutants killed.** One concurrency defect and the identified acceptance-test gaps were fixed and re-verified in the final commit. No unresolved ranked defect remains in the reviewed scope. TVIS-01 through TVIS-05 may be marked Verified after the orchestrator closes T8 bookkeeping.

The stock `validate_state.py` CLI searches `.specs/features` and was a no-op for this repository layout. Its actual `_check_feature` validator was then invoked directly on `specs/active/tournament-visibility-invitations`: PASS (report present, filled PASS verdict, file:line evidence found).
