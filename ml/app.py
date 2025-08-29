# ml/app.py
import os
import time
from typing import Dict, List, Optional, Set, Any

import structlog
import yaml
from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from ml.logic import recommend_minimal

APP_NAME = "tes-ml"
VERSION = os.getenv("ML_VERSION", "0.1.0")
ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS", "*").split(",")
PORT = int(os.getenv("ML_PORT", "8000"))
DATA_PATH = os.environ.get("DATA_PATH", "/app/ml/data/got.yaml")

# ---- logging ----
structlog.configure(
    processors=[
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.add_log_level,
        structlog.processors.JSONRenderer(),
    ]
)
log = structlog.get_logger(app=APP_NAME)

# ---- app ----
app = FastAPI(title="TES-ML", version=VERSION)

app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_methods=["*"],
    allow_headers=["*"],
)

READY = False


@app.on_event("startup")
async def startup_event():
    global READY
    t0 = time.time()
    # TODO: preload dataset/embeddings if needed
    READY = True
    log.info(
        "startup_complete",
        ready=READY,
        took_ms=int((time.time() - t0) * 1000),
        version=VERSION,
    )


@app.middleware("http")
async def request_id_middleware(request: Request, call_next):
    request_id = request.headers.get("X-Request-Id") or os.urandom(8).hex()
    response: Response = await call_next(request)
    response.headers["X-Request-Id"] = request_id
    structlog.contextvars.bind_contextvars(requestId=request_id)
    log.info(
        "request_served",
        path=str(request.url.path),
        method=request.method,
        status=response.status_code,
    )
    structlog.contextvars.clear_contextvars()
    return response


@app.get("/health")
def health():
    return {"status": "UP", "ready": READY, "version": VERSION}


# --------- Models ----------
class Episode(BaseModel):
    id: Optional[str] = None
    season: int
    episode: Optional[int] = None  # допускаем отсутствие, нормализуем позже
    title: Optional[str] = ""
    summary: Optional[str] = ""
    arcs: Optional[List[str]] = None


class RecommendIn(BaseModel):
    showId: Optional[str] = Field(None, alias="showId")
    targetSeason: int = Field(..., alias="targetSeason", ge=1)
    immersion: int = Field(3, ge=1, le=5)
    required_arcs_by_season: Optional[Dict[int, Set[str]]] = Field(
        None, alias="required_arcs_by_season"
    )
    episodes: Optional[List[Episode]] = None

    class Config:
        populate_by_name = True  # принимать и имена полей, и alias


class MinimalEpisode(BaseModel):
    id: str = ""
    season: int
    episode: int
    title: str = ""
    arcs: List[str] = []


class RecommendOut(BaseModel):
    recommendations: Dict[int, List[MinimalEpisode]]


# ---------- Helpers ----------
def _load_episodes_from_yaml(path: str) -> List[Dict[str, Any]]:
    """
    Лояльно читаем YAML и возвращаем плоский список эпизодов.
    Поддерживаем:
      - [ {season, episode|number, title, ...}, ... ]
      - { episodes: [...] }
      - { 1: [...], 2: [...] }  (season->list)
      - { seasons: { 1: [...], 2: [...] } }
    """
    if not os.path.exists(path):
        return []

    with open(path, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f) or {}

    episodes: List[Dict[str, Any]] = []

    if isinstance(data, list):
        episodes = data
    elif isinstance(data, dict):
        if "episodes" in data and isinstance(data["episodes"], list):
            episodes = data["episodes"]
        elif "seasons" in data and isinstance(data["seasons"], dict):
            for key, val in data["seasons"].items():
                try:
                    season_num = int(key)
                except Exception:
                    continue
                if isinstance(val, list):
                    for ep in val:
                        ep = dict(ep or {})
                        ep["season"] = ep.get("season", season_num)
                        episodes.append(ep)
        else:
            # мапа {season: [ ... ]}
            for key, val in data.items():
                try:
                    season_num = int(key)
                except Exception:
                    continue
                if isinstance(val, list):
                    for ep in val:
                        ep = dict(ep or {})
                        ep["season"] = ep.get("season", season_num)
                        episodes.append(ep)

    # нормализация номера эпизода
    for ep in episodes:
        if ep.get("episode") is None and ep.get("number") is not None:
            ep["episode"] = ep.get("number")

    return episodes


# ---------- Routes ----------
@app.post("/recommendations", response_model=RecommendOut)
def recommendations(inp: RecommendIn):
    # эпизоды из запроса или из DATA_PATH
    eps = [e.model_dump() for e in (inp.episodes or [])]
    if not eps:
        eps = _load_episodes_from_yaml(DATA_PATH)

    res = recommend_minimal(
        episodes=eps,
        targetSeason=inp.targetSeason,
        immersion=inp.immersion,
        required_arcs_by_season=inp.required_arcs_by_season,
        showId=inp.showId,
    )

    # normalize for response
    out: Dict[int, List[MinimalEpisode]] = {}
    for season, items in res.items():
        out[season] = [
            MinimalEpisode(
                id=str(x.get("id") or ""),
                season=int(x.get("season")),
                episode=int(x.get("episode") or x.get("number") or 0),
                title=str(x.get("title") or ""),
                arcs=[str(a) for a in (x.get("arcs") or [])],
            )
            for x in items
        ]
    return RecommendOut(recommendations=out)