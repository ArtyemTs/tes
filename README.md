## Through Every Season (TES)

## 📖 Overview
**Through Every Season (TES)** is a service that recommends the **minimal sufficient set of TV show episodes** so viewers can start from any season while preserving context.  

The user selects:
- **Target season** (where they want to start watching)
- **Immersion level** (1–5, fewer → only critical episodes, higher → more context)

✅ Current MVP is fully working end-to-end with the *Game of Thrones* sample dataset (S1–S3).

---

## 🚀 Quick Start

```bash
git clone <your-repo-url> tes && cd tes
docker compose -f infra/docker-compose.yml up --build
```

Open in browser:
- **Web UI:** http://localhost:5173  
- **API (Spring Boot):** http://localhost:8080/actuator/health  
- **ML (FastAPI):** http://localhost:8000/docs  
- **API Docs (Swagger/OpenAPI):** http://localhost:8080/swagger-ui.html  

---

## 📡 Example Usage

```bash
curl -s http://localhost:8080/recommendations   -H 'Content-Type: application/json'   -d '{"showId":"got","targetSeason":4,"immersion":2,"locale":"en"}' | jq
```

Response example:
```json
{
  "showId": "got",
  "targetSeason": 4,
  "immersion": 2,
  "items": [
    { "season":1, "episode":1, "title":"Winter Is Coming", "reason":"Introduces Starks; first White Walker threat." },
    { "season":1, "episode":7, "title":"You Win or You Die", "reason":"Ned confronts Cersei; power struggle ignites." }
  ]
}
```

---

## 🧩 Architecture & Services

- **ML Service (FastAPI, Python 3.11)**  
  Endpoint: `POST /recommend`  
  Generates recommendations from dataset (`ml/data/got.yaml`).

- **API Service (Spring Boot, Java 21)**  
  Endpoint: `POST /recommendations`  
  Proxies requests to ML service, adds validation, exposes health checks, and documents endpoints via OpenAPI.

- **Web UI (Vite + React)**  
  Minimal client for user input (show, season, immersion) and result rendering.

---

## 📂 Repository Structure

```
tes/
  api/       # Spring Boot API
  ml/        # FastAPI ML service
  web/       # React frontend (Vite)
  infra/     # docker-compose, infra configs
  docs/      # Documentation
  .github/   # CI workflow
```

---

## 🧠 Recommendation Logic (MVP)

- **Immersion level (1–5):**  
  Lower = only critical episodes; Higher = more context.
- **Core arcs coverage:** ensures at least one episode per key arc.  
- **Explainability:** each recommendation includes a short reason (arc + summary).  

---

## 🔐 Security & Legal

- Dataset contains **short, original summaries** for demo purposes (fair use).  
- No copyrighted transcripts or full scripts are stored.  
- No secrets in repo — config is via environment variables.  
- For production: add rate limiting, CORS whitelist, audit logging, dependency scanning (OWASP, Renovate).  

---

## 🛠️ Next Steps

1. Add **i18n (EN/RU)** in the Web UI.  
2. Expand dataset (more seasons and shows).  
3. API: extend OpenAPI docs (springdoc) and maintain controller contract tests.  
4. ML: add embeddings + cosine similarity scoring for arcs.  
5. End-to-end tests with Playwright/Cypress.  

![CI](https://github.com/ArtyemTs/tes/actions/workflows/ci.yml/badge.svg)
![CodeQL](https://github.com/ArtyemTs/tes/actions/workflows/codeql.yml/badge.svg)
