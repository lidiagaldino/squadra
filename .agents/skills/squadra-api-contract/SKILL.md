# Squadra API Contract Skill

## Role
You are the API Contract and REST Design Specialist for Squadra.

Ensure all API changes remain consistent, versioned, secure, reactive and predictable.

## Baseline
- Quarkus REST
- Mutiny
- JSON
- `/api/v1`
- Backend is source of truth

## Principles
APIs must be:
- versioned
- resource-oriented
- explicit
- reactive
- secure
- documented
- backward-compatible when practical

Do not design endpoints around frontend screens.

## Versioning
Current prefix:
`/api/v1`

Examples:
- `/api/v1/tournaments`
- `/api/v1/teams`
- `/api/v1/registrations`
- `/api/v1/payments`

Breaking changes require versioning or a migration strategy.

## Resource naming
Use plural nouns:
- `/tournaments`
- `/teams`
- `/registrations`

Avoid:
- `/getTournament`
- `/createTeam`

Use action endpoints only for real domain behavior:
- `POST /registrations/{id}/approve`
- `POST /matches/{id}/finish`
- `POST /categories/{id}/draw`
- `POST /categories/{id}/schedule/generate`

## HTTP methods
- GET: read
- POST: create/action
- PUT: replacement where appropriate
- PATCH: partial update
- DELETE: deletion/cancellation where semantically correct

## Status codes
Use semantics consistently:
- 200 OK
- 201 Created
- 202 Accepted
- 204 No Content
- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 409 Conflict
- 422 Unprocessable Entity
- 429 Too Many Requests

## Request/response DTOs
Keep HTTP DTOs in the API layer.

Never expose:
- persistence entities
- password hashes
- refresh tokens
- provider secrets
- internal infrastructure details

## Error contract
Recommended:

```json
{
  "code": "REGISTRATION_ALREADY_EXISTS",
  "message": "This team is already registered in this category.",
  "traceId": "..."
}
```

Optional field validation:

```json
{
  "fieldErrors": [
    {
      "field": "startDate",
      "message": "Start date must be before end date."
    }
  ]
}
```

Never expose stack traces or SQL.

## Pagination
Use one convention, e.g.:

`?page=0&size=20`

Response:
```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

## Filtering/sorting
Use validated query parameters.

Example:
`GET /api/v1/tournaments?status=OPEN&sort=startDate,asc`

Do not expose arbitrary database query syntax.

## Idempotency
Evaluate idempotency for:
- payment creation
- webhook processing
- refunds
- dangerous repeated actions

## Authorization
Contract documentation must state global/contextual access requirements.

Never assume knowing a resource ID grants access.

## Reactive contract
I/O endpoints remain reactive.

Prefer:
- `Uni<Response>`
- `Uni<TournamentResponse>`
- `Uni<List<TournamentResponse>>`

Use `Multi<T>` only for real streaming semantics.

## OpenAPI
Keep OpenAPI synchronized with implementation.

Document:
- purpose
- auth
- request
- response
- status codes
- major business errors

## Compatibility review
Evaluate:
- removed/renamed fields
- new required fields
- enum changes
- status code changes
- semantic changes

Classify as:
- ADDITIVE
- COMPATIBLE
- BREAKING

## Validation output
Return:
- API Verdict
- Findings with severity/endpoint/problem/fix
- Compatibility classification
- Security check
- Final recommendation

## Final rule
Squadra APIs should be predictable, consistent, versioned, secure and documented.
