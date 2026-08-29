# Organizer Profile Validation

## Result

PASS - Organizer Profile is implemented and verified.

## Gates

| Gate | Command | Result |
| --- | --- | --- |
| Spec validation | `python3 /Users/lidiagaldino/.codex/skills/tlc-spec-driven/scripts/validate_spec.py specs/active/organizer-profile/spec.md` | PASS |
| Task validation | `python3 /Users/lidiagaldino/.codex/skills/tlc-spec-driven/scripts/validate_tasks.py specs/active/organizer-profile/tasks.md` | PASS |
| Backend focused | `JAVA_HOME=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home PATH=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/private/tmp/squadra-m2 -Dtest=OrganizerProfileTest,DefaultOrganizerProfileServiceTest,OrganizerProfileResourceTest test -q` | PASS |
| Backend full | `JAVA_HOME=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home PATH=/Users/lidiagaldino/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/private/tmp/squadra-m2 verify -q` | PASS |
| Frontend check | `npm run check` | PASS: lint, typecheck, 13 test files and 58 tests |
| Frontend build | `npm run build` | PASS: Next.js build includes `/organizer/profile` |
| Compose config | `docker compose --env-file .env.example -f infra/docker-compose.yml config` | PASS |

The first sandboxed backend full run failed because Quarkus could not bind its local test HTTP server (`Operation not permitted`). The same command passed after escalation.

## Requirement Evidence

| Requirement | Status | Evidence |
| --- | --- | --- |
| OPROF-01 Create My Organization Profile | Verified | The organizer API exposes `GET` and `PUT /api/v1/organizers/me/profile` for `ORGANIZER` users only in `OrganizerProfileResource.java:23-49`. The service derives ownership from the authenticated user ID and creates a profile when none exists in `DefaultOrganizerProfileService.java:36-47`. The migration stores one profile per user with a unique foreign key in `V4__create_organizer_profiles.sql:1-17`. |
| OPROF-02 Edit My Organization Profile | Verified | `DefaultOrganizerProfileService.java:42-47` finds the current user's existing profile, updates it, and upserts the result. `OrganizerProfile.java:38-40` applies edited values and timestamps. |
| OPROF-03 Validate Organization Profile Input | Verified | Domain validation enforces required text, optional text bounds, email, website scheme and max lengths in `OrganizerProfile.java:56-100`. Frontend validation mirrors the same bounds in `form.tsx:11-23`. |
| OPROF-04 Use the Organizer Profile Screen | Verified | The `/organizer/profile` route renders inside the authenticated sidebar shell in `page.tsx:16-30`. The organizer form loads existing data, supports empty/load/error/success states and saves without a full reload in `form.tsx:43-77` and `form.tsx:94-163`. The Home sidebar routes organizer users to `/organizer/profile` while keeping athletes on `/athlete/profile` in `home/page.tsx:43-60`. |
| OPROF-05 Edge Cases | Verified | The database enforces one row per organizer and orphan cleanup with `UNIQUE REFERENCES squadra.users(id) ON DELETE CASCADE` in `V4__create_organizer_profiles.sql:1-17`. The API never reads an owner ID from the request body and constructs the save command only from profile fields in `OrganizerProfileResource.java:44-49`. |

## Notes

- Frontend API coverage includes empty profile mapping, one refresh retry, successful save, and failed recovery.
- Component coverage includes empty state, loaded profile, live preview updates, frontend validation, save success, load failure and save failure with typed values retained.
- Backend API coverage includes happy path, missing profile, unauthorized/forbidden access, validation and ignoring client-supplied owner data.

## Independent Verifier

Verdict: PASS after fix.

OPROF-01, OPROF-02, ownership, role enforcement and `404 ORGANIZER_PROFILE_NOT_FOUND` behavior are supported by the resource/service/migration and API tests.

Resolved blocker: OPROF-03 and OPROF-04 now support optional blank `website` consistently. The backend request DTO accepts an empty optional website or an `http://`/`https://` URL in `backend/src/main/java/com/squadra/platform/api/v1/organizers/profile/request/OrganizerProfileRequests.java:18`, and the focused/full backend gates were rerun successfully after the fix.

Residual risks: none blocking. API tests now include unauthenticated `PUT` and a valid minimal `PUT` with blank optional fields. Frontend tests cover refresh retry in the API client and typed-value preservation on save failure.
