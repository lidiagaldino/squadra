# Project State

## Decisions

### AD-001: Foundation topology

- **Status**: active
- **Decision**: Use npm workspaces with `frontend`, `backend`, and `infra`; use Docker Compose for local orchestration.
- **Rationale**: Keeps deployable applications isolated and local onboarding repeatable.

## Handoff

- Feature: `specs/active/tournament-visibility-invitations`.
- Status: complete and independently verified on 2026-10-30 (T1–T8).
- Delivered: visibility, public discovery, private invitations, team decisions,
  transactional registration capacity, Portuguese UI and authenticated BFF.
- Implementation commits: `42e6627`, `c7b481b`, `64f40ef`, `64711c2`.
- Gates: 127 backend tests on H2/PostgreSQL; 129 frontend tests; production build;
  two mutation sensors killed; local k6 smoke passed.
- Coverage: backend lines 95.36%; frontend lines 93.8%, branches 83.21%.
- Evidence: `specs/active/tournament-visibility-invitations/validation.md`.
- Remaining feature work: none. No next feature selected.
- Branch: `main`; commits are local (no push requested).
- Unrelated existing changes preserved: `.agents/skills/squadra-design/SKILL.md`,
  `.agents/skills/squadra-ui/SKILL.md`, `frontend/tsconfig.tsbuildinfo`.
