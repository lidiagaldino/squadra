# Tournament participation smoke test

Run only against a disposable local or staging database. Setup creates two test
accounts, a team, a private tournament, a category, and an accepted invitation.
The test retains these fixtures for diagnosis; dispose of the test database
when finished.

```sh
BASE_URL=http://localhost:58080 k6 run k6/smoke/tournament-participation.js
```

The 10-second scenario uses three virtual users. It checks the accepted team's
private overview and exactly one draft registration on every iteration. Setup
also checks the accepted invitation state. Concurrent last-slot and decision
races are tested by backend integration tests.

Local run on 2026-10-28 with PostgreSQL 17: 13,506 HTTP requests, 0% failed
requests, 13,497 successful checks, and p95 latency 3.46 ms. Thresholds passed:
checks = 100%, error rate < 1%, and p95 < 500 ms. This is a local smoke result,
not a production capacity estimate.
