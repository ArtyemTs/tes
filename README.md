# Through Every Season (TES)

Service that recommends a minimal sufficient set of episodes per season so a user can start from any season and still understand the context.

---

## Quick start

```bash
git clone https://github.com/ArtyemTs/tes.git
cd tes
docker compose up --build
```

Open:
- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- Web UI: <http://localhost:5173>

---

## Contract first

The API contract is maintained in **api/src/main/resources/openapi/tes-openapi.yaml**.

Update flow:
1. Modify the YAML (bump `info.version`).
2. Align DTOs/validation to match the spec.
3. Run contract tests:
   ```bash
   cd api && ./mvnw test
   ```
4. Commit with message: `feat(api): contract vX.Y.Z`.

Limits:
- Request body ≤ 16KB
- API→ML timeout: 3s

---

## Error handling

- Errors follow [RFC7807](https://datatracker.ietf.org/doc/html/rfc7807) Problem+JSON.
- Every error has a `code` (`TES-xxx`) and `correlationId`.
- Localized messages (`en`, `ru`).
- Example:
```json
{
  "type": "https://tes.dev/errors/invalid-request",
  "title": "Invalid request",
  "status": 400,
  "code": "TES-001",
  "correlationId": "123e4567-e89b-12d3-a456-426614174000"
}
```

---

## ML Resilience

- API→ML calls wrapped in Resilience4j (timeouts, circuit breaker, bulkhead).
- If ML is down: API returns `503 Service Unavailable` + `Retry-After: 10`.
- Smoke test:
  ```bash
  k6 run infra/perf/smoke-10rps-1m.js
  ```

---

## Rate limiting

- Configurable per-IP rate limit (default: 60 req/hour).
- On exceeding: `429 Too Many Requests` + headers:
  - `X-RateLimit-Limit`
  - `X-RateLimit-Remaining`
  - `X-RateLimit-Reset`

---

## Development

- Java 21 (Spring Boot API).
- Python 3.11 (ML microservice).
- Node.js 20 (Web UI).
- Dockerized, `docker-compose.yml` ties components together.

---

## Testing

- API: JUnit5 + MockMvc contract & error tests.
- ML: pytest.
- Smoke: k6 scripts under `infra/perf`.

---

## Security

- No hardcoded secrets.
- Use `.env` for overrides.
- Dependencies checked via `mvn dependency:analyze` / `pip-audit`.

---
