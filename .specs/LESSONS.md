# LESSONS - auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `.specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Specify/Design)

Corroborated across multiple features. Safe to apply as guidance.

_none_

## Candidates (under observation - do NOT load as guidance yet)

Seen once or not yet corroborated. Tracked, not trusted.

### L-001 - Recheck active registration under the category lock in the same transaction that inserts a tournament invitation.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `infra/db` · harmful: 0
- features: tournament-visibility-invitations
- evidence: specs/active/tournament-visibility-invitations/validation.md:48 (infra/db)
- last seen: 2026-10-30T15:28:19Z

### L-002 - Assert persisted participation state and absence of unintended writes after rejected, retried, or competing requests.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `tournament-tests` · harmful: 0
- features: tournament-visibility-invitations
- evidence: specs/active/tournament-visibility-invitations/validation.md:46 (tournament-tests)
- last seen: 2026-10-30T15:28:19Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
