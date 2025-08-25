

# Through Every Season (TES)

Minimal episode recommendations per season so you can jump into any season of a TV show with just enough context.

- **Input:** show, target season, immersion level (1–5).
- **Output:** minimal per-season episode list + short reasons ("introduces X", "resolves Y").
- **Stack:** Java 21 (API), Python (ML), Web UI with RU/EN i18n. Everything runs in Docker.

> Discussions in Russian; code, identifiers, and technical docs in English.

---

## Project layout

infra/
api/
ml/
web/
docs/
adr/
project_structure.txt

See `project_structure.txt` for the up-to-date tree.

---

## Prerequisites

- Docker + Docker Compose
- Make (optional)
- macOS/arm64 friendly (tested on Apple Silicon)

---

## Quick start (Dev)

Local development uses the **`dev`** profile by default.


## From repo root
docker compose -f infra/docker-compose.yml up --build

	•	API: http://localhost:8080
	•	(If enabled) Actuator: http://localhost:8081
	•	ML:  http://localhost:8000
	•	Web: http://localhost:5173 (if applicable)

CORS in dev allows http://localhost:5173 and http://localhost:3000 by default.

⸻

Production-like run

Use the prod profile without changing code. Provide a strict CORS origin.

SPRING_PROFILES_ACTIVE=prod \
ALLOWED_ORIGINS=https://tes.example.com \
docker compose -f infra/docker-compose.yml --profile prod up --build

Ports
	•	API_PORT (default 8080)
	•	ML_PORT  (default 8000)

⸻

Configuration

All configuration is environment-driven. Key variables:

Service	Variable	Default	Description
API	SPRING_PROFILES_ACTIVE	dev	Spring profile: dev or prod
API	API_PORT	8080	API HTTP port
API	ALLOWED_ORIGINS	http://localhost:5173,http://localhost:3000	CORS allowlist (comma-separated)
API	ML_BASE_URL	http://ml:8000	ML base URL
ML	ML_PORT	8000	ML HTTP port
ML	ALLOWED_ORIGINS	* (dev)	CORS allowlist (set a single origin in prod)
ML	ML_VERSION	0.1.0	Exposed ML version string

Create .env (optional) to centralize local values:

# .env (example)
SPRING_PROFILES_ACTIVE=dev
API_PORT=8080
ML_PORT=8000
ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
ML_BASE_URL=http://ml:8000
ML_VERSION=0.1.0

Docker Compose picks up .env automatically.

⸻

Services

API (Java 21 / Spring Boot)
	•	Profiles: dev (default), prod
	•	Config split across application-dev.yml, application-prod.yml
	•	Env overrides: API_PORT, ALLOWED_ORIGINS, ML_BASE_URL

ML (Python)
	•	Env-driven: ML_PORT, ALLOWED_ORIGINS, ML_VERSION
	•	CORS allowlist respects ALLOWED_ORIGINS

⸻

Run book

Start (dev)

docker compose -f infra/docker-compose.yml up --build

Start (prod-like)

SPRING_PROFILES_ACTIVE=prod ALLOWED_ORIGINS=https://tes.example.com \
docker compose -f infra/docker-compose.yml --profile prod up --build

Rebuild only API

docker compose -f infra/docker-compose.yml build api && docker compose -f infra/docker-compose.yml up api

Rebuild only ML

docker compose -f infra/docker-compose.yml build ml && docker compose -f infra/docker-compose.yml up ml


⸻

Development conventions
	•	Conventional Commits
	•	Short PRs, clear DoD
	•	Docs live in docs/ and ADRs in docs/adr/

⸻

Security & legal
	•	No unlicensed data.
	•	Secrets/config only via env variables or secret stores.
	•	Restrictive CORS in production (ALLOWED_ORIGINS must be set to a single trusted origin).

⸻

Next steps

Phase 0 (today):
	1.	Profiles & config ✅ (this PR)
	2.	Health/Readiness endpoints
	3.	JSON logs + request correlation

![CI](https://github.com/ArtyemTs/tes/actions/workflows/ci.yml/badge.svg)
![CodeQL](https://github.com/ArtyemTs/tes/actions/workflows/codeql.yml/badge.svg)
