## [Unreleased]
### Added
- Dev/prod profiles for API and ML (env-driven configuration for ports, CORS, log levels, ML base URL).
- Docker Compose profiles (`dev`, `prod`) with environment wiring.
- Documentation updates: README (Quick start, profiles), ADR-0001.

### Changed
- Centralized configuration via `application-*.yml` (API) and env vars (ML).

### Security
- CORS allowlist is explicit and environment-configurable for prod.