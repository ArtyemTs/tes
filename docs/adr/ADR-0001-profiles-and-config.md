# ADR-0001: Introduce dev/prod profiles and env-driven configuration

## Status
Accepted (2025-08-25)

## Context
We need predictable local and production-like runs with secure defaults (CORS, ports, log levels) and zero code changes between environments.

## Decision
- API: Spring Boot profiles `dev` (default) and `prod`; split config into `application-dev.yml` and `application-prod.yml`.
- ML: Environment-driven configuration for port, allowed origins, and version; CORS allowlist.
- Infra: Docker Compose profiles `dev`/`prod` to wire services consistently.

## Consequences
- + Clear separation of local vs prod settings.
- + Safer defaults (restrictive CORS in prod).
- + Simpler ops via env vars.
- ± Requires updating README and compose environment variables.