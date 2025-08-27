## [Unreleased]

## [0.2.0] - 2025-08-27
### Added
- **Contract-first** OpenAPI spec (`tes-openapi.yaml`) with Swagger UI.
- **Problem+JSON** unified error format with TES error codes and i18n (en/ru).
- **Resilience**: WebClient with timeouts + Resilience4j (CircuitBreaker, Bulkhead, TimeLimiter).
- **ML health** endpoint (`/health`) and docker-compose healthchecks.
- **Smoke test**: k6 script (`infra/perf/smoke-10rps-1m.js`).
- **Rate limiting**: Bucket4j filter (60 req/hour per IP), 429 with headers.

### Changed
- DTOs (`RecommendationRequest`) now validated with Bean Validation.
- README updated with Contract-first, Error handling, Resilience, Rate limiting sections.

### Fixed
- Standardized error responses for invalid requests, timeouts, ML unavailability, rate limit exceeded.

## [0.1.0] - 2025-08-20
- Initial MVP: API, ML service, basic Web UI, docker-compose.