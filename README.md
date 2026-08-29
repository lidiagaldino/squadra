# Squadra

Squadra is a web-first tournament-management platform for women's football. This repository currently contains the engineering foundation only; no product domain, authentication flow, or tournament workflow is implemented.

## Structure

```text
frontend/  Next.js App Router application
backend/   Java 21 and Quarkus API
infra/     Docker Compose local runtime
docs/      Product source of truth
specs/     Active TLC specifications
```

## Prerequisites

- Docker Desktop with Compose (recommended)
- Node.js 22+ and npm 10+ for frontend development
- Java 21 and Maven 3.9+ for backend development

## Local runtime

```sh
cp .env.example .env
docker compose --env-file .env -f infra/docker-compose.yml up --build
```

Web: `http://localhost:3000` · API health: `http://localhost:8080/api/v1/health`.

## Host-native verification

```sh
cd frontend && npm ci && npm run check && npm run build
cd ../backend && mvn verify
```

## Conventions

- Backend authorization is the enforcement point; client visibility is never authorization.
- New backend domain modules use `api`, `services`, `domain`, and `infra` layers.
- Flyway owns schema evolution; Hibernate schema generation is disabled.
- Only non-secret local defaults belong in tracked templates. Keep deployment secrets in secret stores or ignored environment files.
- Start product work from an active TLC specification under `specs/active`.
