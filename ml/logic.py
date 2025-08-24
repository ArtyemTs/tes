from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, Iterable, List, Tuple

import numpy as np

from .embeddings import Embedder, FakeEmbedder
from .scoring import score_episodes_by_query, EpisodeScore
from .coverage import greedy_arc_coverage, per_season_budget


# --- Domain DTOs used by the ML layer ---

@dataclass(frozen=True)
class Episode:
    id: str               # unique key, e.g. "S01E03"
    season: int
    number: int
    title: str
    synopsis: str
    arcs: List[str]


@dataclass(frozen=True)
class Recommendation:
    season: int
    episode: int
    reason: str


# --- Public API ---

def recommend_minimal(
    episodes_by_season: Dict[int, List[Episode]],
    target_season: int,
    immersion_level: int,
    # Optional: arcs you deem necessary to understand before target_season
    required_arcs: Iterable[str] | None = None,
    *,
    embedder: Embedder | None = None,
    query_text: str | None = None,
) -> List[Recommendation]:
    """
    Produce minimal episode set per prior season to preserve context.

    Strategy:
      1) Coverage-first: greedily cover required arcs per season (if provided).
      2) Fill up to budget by highest cosine score to the query (if provided),
         otherwise by simple heuristic (earlier + more arcs).
    """
    embedder = embedder or FakeEmbedder()
    required_arcs_set = set(required_arcs or [])

    # Simple default query if none provided: "Context needed for season N"
    if not query_text:
        query_text = f"Key context to understand season {target_season}"

    # Precompute embeddings cache by episode id (used by scoring)
    synopsis_embedding_cache: Dict[str, np.ndarray] = {}

    results: List[Recommendation] = []

    # Work only with seasons prior to target
    for season in sorted(s for s in episodes_by_season.keys() if s < target_season):
        season_eps = sorted(episodes_by_season[season], key=lambda e: e.number)
        if not season_eps:
            continue

        budget = per_season_budget(total_eps=len(season_eps), immersion_level=immersion_level)

        # 1) Coverage step (only if arcs are provided)
        selected_ids: List[str] = []
        reasons: Dict[str, str] = {}

        if required_arcs_set:
            cov = greedy_arc_coverage(season_eps, required_arcs_set)
            for eid in cov.selected_ids:
                selected_ids.append(eid)
                covered_here = set(next(e for e in season_eps if e.id == eid).arcs or []) & required_arcs_set
                if covered_here:
                    reasons[eid] = f"covers arcs: {', '.join(sorted(covered_here))}"
            # If nothing covered, we'll fall back to scoring-only

        # 2) Scoring fill-up
        if len(selected_ids) < budget:
            scored: List[EpisodeScore] = score_episodes_by_query(
                season_eps,
                query_text=query_text,
                embedder=embedder,
                synopsis_embedding_cache=synopsis_embedding_cache,
            )
            for es in scored:
                if es.episode_id in selected_ids:
                    continue
                selected_ids.append(es.episode_id)
                if es.episode_id not in reasons:
                    # explainability: cosine bucket
                    reasons[es.episode_id] = f"high thematic relevance (cos={es.score:.2f})"
                if len(selected_ids) >= budget:
                    break

        # 3) Materialize per-season recommendations with reasons
        ep_by_id = {e.id: e for e in season_eps}
        for eid in selected_ids:
            e = ep_by_id[eid]
            reason = reasons.get(eid, "selected for context")
            results.append(Recommendation(season=e.season, episode=e.number, reason=reason))

    # Stable, deterministic order: by season, then episode number
    results.sort(key=lambda r: (r.season, r.episode))
    return results

# ---- FastAPI adapters (dataset loader + endpoint facade) ----
from pathlib import Path
import yaml  # pip install pyyaml

def load_dataset(path: str) -> dict[int, list[Episode]]:
    """
    Loads a small lawful YAML dataset into {season -> [Episode,...]}.
    Supported shapes:
      A) {"seasons": {"1": [{number,title,synopsis,arcs}], "2": [...]}}
      B) {"episodes": [{season,number,title,synopsis,arcs}]}
    """
    raw_text = Path(path).read_text(encoding="utf-8")
    data = yaml.safe_load(raw_text) or {}

    episodes_by_season: dict[int, list[Episode]] = {}

    if "seasons" in data:
        for s, eps in (data["seasons"] or {}).items():
            season_int = int(s)
            episodes_by_season.setdefault(season_int, [])
            for e in eps or []:
                num = int(e["number"])
                eid = e.get("id") or f"S{season_int:02d}E{num:02d}"
                episodes_by_season[season_int].append(
                    Episode(
                        id=eid,
                        season=season_int,
                        number=num,
                        title=e.get("title", ""),
                        synopsis=e.get("synopsis", "") or "",
                        arcs=list(e.get("arcs", []) or []),
                    )
                )
        return episodes_by_season

    if "episodes" in data:
        for e in data["episodes"] or []:
            season_int = int(e["season"])
            num = int(e["number"])
            eid = e.get("id") or f"S{season_int:02d}E{num:02d}"
            episodes_by_season.setdefault(season_int, []).append(
                Episode(
                    id=eid,
                    season=season_int,
                    number=num,
                    title=e.get("title", ""),
                    synopsis=e.get("synopsis", "") or "",
                    arcs=list(e.get("arcs", []) or []),
                )
            )
        # Keep episodes per season sorted by episode number
        for s in list(episodes_by_season.keys()):
            episodes_by_season[s] = sorted(episodes_by_season[s], key=lambda x: x.number)
        return episodes_by_season

    raise ValueError("Unsupported dataset YAML shape. Expected keys: 'seasons' or 'episodes'.")


def recommend(req, dataset: dict[int, list[Episode]]):
    """
    Thin facade to keep FastAPI handler minimal.
    Expects req to have: target_season (int), immersion_level (int), optional required_arcs (list[str]).
    Returns dict shape that should match RecommendationResponse.
    """
    # tolerant attribute access
    target_season = getattr(req, "target_season", None) or getattr(req, "season", None)
    immersion_level = getattr(req, "immersion_level", None) or getattr(req, "immersion", 2)
    required_arcs = getattr(req, "required_arcs", []) or []

    recs = recommend_minimal(
        episodes_by_season=dataset,
        target_season=int(target_season),
        immersion_level=int(immersion_level),
        required_arcs=set(required_arcs),
    )

    return {
        "items": [
            {"season": r.season, "episode": r.episode, "reason": r.reason}
            for r in recs
        ]
    }