from __future__ import annotations

from collections import defaultdict
from typing import Dict, Iterable, List, Mapping, Optional, Sequence, Set

from .embeddings import FakeEmbeddings, EmbeddingsProvider
from .scoring import EpisodeScorer, ScoringConfig
from .coverage import CoveragePlanner

Episode = Mapping[str, object]


def _build_query_text(
    target_season: int,
    global_arcs: Sequence[str] | None,
    show_name: str | None,
) -> str:
    """
    Simple query composer for embeddings.
    Later we can enrich with character states, antagonist names, etc.
    """
    parts: List[str] = []
    if show_name:
        parts.append(f"show: {show_name}")
    parts.append(f"target season: {target_season}")
    if global_arcs:
        parts.append("key arcs: " + ", ".join(global_arcs))
    return "\n".join(parts)


def recommend_minimal(
    episodes: Sequence[Episode],
    target_season: int,
    immersion: int = 3,
    required_arcs_by_season: Optional[Dict[int, Set[str]]] = None,
    show_name: Optional[str] = None,
    embedder: Optional[EmbeddingsProvider] = None,
) -> Dict[int, List[Episode]]:
    """
    Core entry point:
    - Scores episodes by semantic proximity to a simple query.
    - Applies per-season coverage (set‑cover over arcs) within an immersion budget.
    Returns: {season: [episodes...]} for all seasons < target_season.
    """
    embedder = embedder or FakeEmbeddings()
    scorer = EpisodeScorer(embedder=embedder, config=ScoringConfig())
    planner = CoveragePlanner()

    # Filter prior seasons only
    prior_eps: List[Episode] = [ep for ep in episodes if int(ep.get("season", 0)) < target_season]

    if not prior_eps:
        return {}

    # Build a single query for now (can be made per-season in the future)
    global_arcs: List[str] = []
    if required_arcs_by_season:
        # union of arcs can make the query slightly more informative
        uni: Set[str] = set()
        for s, arcs in required_arcs_by_season.items():
            uni |= set(arcs)
        global_arcs = sorted(list(uni))

    query_text = _build_query_text(target_season, global_arcs, show_name)

    # Score all episodes
    scored = scorer.score(prior_eps, query_text)  # List[(ep, score)]

    # Group by season
    by_season: Dict[int, List[tuple]] = defaultdict(list)
    for ep, score in scored:
        s = int(ep.get("season", 0))
        by_season[s].append((ep, score))

    # For each season, apply coverage
    result: Dict[int, List[Episode]] = {}
    for season, items in by_season.items():
        items.sort(key=lambda t: t[1], reverse=True)
        needed = set(required_arcs_by_season.get(season, set())) if required_arcs_by_season else set()
        chosen = planner.select_for_season(items, needed, immersion)
        result[season] = chosen

    # Keep only seasons < target and sort by season
    return dict(sorted(result.items(), key=lambda kv: kv[0]))