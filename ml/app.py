# ml/app.py
from fastapi import FastAPI
from pydantic import BaseModel, Field
from typing import Dict, List, Optional, Set, Any
from .models import RecommendationRequest, RecommendationResponse

import os
import yaml

from ml.logic import recommend_minimal

DATA_PATH = os.environ.get("DATA_PATH", "/app/ml/data/got.yaml")

app = FastAPI(title="TES-ML")

class Episode(BaseModel):
    id: str
    season: int
    episode: int | None = None  # допустим отсутствие, нормализуем позже
    title: str | None = ""
    summary: str | None = ""
    arcs: Optional[List[str]] = None

class RecommendIn(BaseModel):
    showId: Optional[str] = Field(None, alias="showId")
    targetSeason: int = Field(..., alias="targetSeason", ge=1)
    immersion: int = Field(3, ge=1, le=5)
    required_arcs_by_season: Optional[Dict[int, Set[str]]] = Field(None, alias="required_arcs_by_season")
    episodes: Optional[List[Episode]] = None

class MinimalEpisode(BaseModel):
    id: str | None = None
    season: int
    episode: int
    title: str = ""
    arcs: List[str] = []

class RecommendOut(BaseModel):
    recommendations: Dict[int, List[MinimalEpisode]]

def _load_episodes_from_yaml(path: str) -> List[Dict[str, Any]]:
    """
    Лояльно читаем YAML и возвращаем плоский список эпизодов.
    Поддерживаем:
      - [ {season, episode|number, title, ...}, ... ]
      - { episodes: [...] }
      - { 1: [...], 2: [...] }  (season->list)
    """
    if not os.path.exists(path):
        return []

    with open(path, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f) or {}

    episodes: List[Dict[str, Any]] = []

    if isinstance(data, list):
        episodes = data
    elif isinstance(data, dict):
         # поддержка форматов:
         # - { episodes: [...] }
         # - { seasons: { 1: [...], 2: [...] } }
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
            # может быть мапа {season: [ ... ]}
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

@app.post("/recommendations", response_model=RecommendOut)
def recommendations(inp: RecommendIn):
    # берем эпизоды из запроса или из DATA_PATH
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

@app.get("/health")
def health():
    return {"status": "ok"}