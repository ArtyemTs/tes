from fastapi import FastAPI
from pydantic import BaseModel, Field
from typing import Dict, List, Optional, Set

from ml.logic import recommend_minimal

app = FastAPI(title="TES-ML")

class Episode(BaseModel):
    id: str
    season: int
    episode: int
    title: str
    summary: str
    arcs: Optional[List[str]] = None

class RecommendIn(BaseModel):
    show_name: Optional[str] = Field(None, alias="show_name")
    target_season: int = Field(..., alias="target_season", ge=1)
    immersion: int = Field(3, ge=1, le=5)
    required_arcs_by_season: Optional[Dict[int, Set[str]]] = Field(None, alias="required_arcs_by_season")
    episodes: Optional[List[Episode]] = None

class MinimalEpisode(BaseModel):
    id: str
    season: int
    episode: int
    title: str
    arcs: List[str] = []

class RecommendOut(BaseModel):
    recommendations: Dict[int, List[MinimalEpisode]]

@app.post("/recommendations", response_model=RecommendOut)
def recommendations(inp: RecommendIn):
    eps = [e.model_dump() for e in (inp.episodes or [])]
    res = recommend_minimal(
        episodes=eps,
        target_season=inp.target_season,
        immersion=inp.immersion,
        required_arcs_by_season=inp.required_arcs_by_season,
        show_name=inp.show_name,
    )
    # normalize for response
    out: Dict[int, List[MinimalEpisode]] = {}
    for season, items in res.items():
        out[season] = [
            MinimalEpisode(
                id=str(x.get("id")),
                season=int(x.get("season")),
                episode=int(x.get("episode")),
                title=str(x.get("title")),
                arcs=[str(a) for a in (x.get("arcs") or [])],
            )
            for x in items
        ]
    return RecommendOut(recommendations=out)

@app.post("/recommendations", response_model=RecommendationResponse)
def recommend_endpoint_compat(req: RecommendationRequest):
    result = recommend(req, DATA)
    return RecommendationResponse.model_validate(result)

@app.post("/recommendations", response_model=RecommendationResponse)
def recommend_endpoint_alias(req: RecommendationRequest):
    return recommend_endpoint(req)

@app.get("/health")
def health(): return {"status": "ok"}