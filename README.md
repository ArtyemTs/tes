# Through Every Season (MVP)

**Through Every Season (TES)** helps viewers jump into any season of a TV show while still understanding the story.  
Instead of watching every episode, TES recommends a **minimal set of episodes per season**, adjustable by immersion level.

## Quick start

```bash
git clone <your-repo-url> tes && cd tes
docker compose -f infra/docker-compose.yml up --build
```

Open in browser:
- Web UI → http://localhost:5173  
- API → http://localhost:8080/actuator/health  
- ML service → http://localhost:8000/docs  

## Example

```bash
curl -s http://localhost:8080/recommendations   -H 'Content-Type: application/json'   -d '{"showId":"got","targetSeason":4,"immersion":2,"locale":"en"}' | jq
```

→ Returns key episodes from earlier seasons with short explanations.

---

🔎 Full documentation: [docs/README.md](docs/README.md)

![CI](https://github.com/ArtyemTs/tes/actions/workflows/ci.yml/badge.svg)
![CodeQL](https://github.com/ArtyemTs/tes/actions/workflows/codeql.yml/badge.svg)
