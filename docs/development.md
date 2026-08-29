# Development Conventions

## Scope boundary

The initial foundation intentionally has no product modules, business entities, authentication implementation, payments, or public tournament pages. Implement each product capability from a TLC specification and the relevant business docs.

## Backend

Create modules by domain. A module keeps HTTP resources in `api`, use cases in `services`, business decisions in `domain`, and adapters/persistence in `infra`. Dependencies flow inward. REST resources validate transport input and delegate; they do not hold business rules.

Every future product endpoint must define backend authentication and authorization. For contextual permissions, verify authenticated user, role, resource, and context. Do not trust client-supplied IDs, roles, payment states, or hidden UI controls.

## Database

Add forward-only Flyway migrations under `backend/src/main/resources/db/migration`. Do not modify an applied migration. Product tables, constraints, indexes, and audit requirements belong to the owning feature specification.

## Frontend

Use Next.js App Router, TypeScript, Tailwind, shadcn/ui, TanStack Query, React Hook Form, and Zod. The backend is the server-state authority. Avoid global domain state unless a feature has a documented need.

## Quality

Run the relevant application check before submitting work. The project targets at least 80% automated coverage for executable code; tests must assert specification outcomes, not implementation details. Use conventional commits once repository write access allows Git metadata to be created.
