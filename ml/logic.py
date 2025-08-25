# ml/logic.py
from __future__ import annotations

from collections import defaultdict
from typing import Dict, List, Optional, Sequence, Set, Tuple, Any

from .embeddings import FakeEmbeddings, EmbeddingsProvider
from .scoring import EpisodeScorer, ScoringConfig
from .coverage import CoveragePlanner

Episode = Dict[str, Any]  # унифицированный тип эпизода (dict)

def _build_query_text(
    targetSeason: int,
    global_arcs: Sequence[str] | None,
    showId: str | None,
) -> str:
    parts: List[str] = []
    if showId:
        parts.append(f"show: {showId}")
    parts.append(f"target season: {targetSeason}")
    if global_arcs:
        parts.append("key arcs: " + ", ".join(global_arcs))
    return "\n".join(parts)


def recommend_minimal(
    episodes: List[Episode],
    targetSeason: int,
    immersion: int = 3,
    required_arcs_by_season: Optional[Dict[int, Set[str]]] = None,
    showId: Optional[str] = None,
    embedder: Optional[EmbeddingsProvider] = None,
) -> Dict[int, List[Episode]]:
    """
    Core entry point:
    Возвращает {season: [Episode,...]} только для сезонов < targetSeason.
    """
    embedder = embedder or FakeEmbeddings()
    scorer = EpisodeScorer(embedder=embedder, config=ScoringConfig())
    planner = CoveragePlanner()

    # фильтруем только прошлые сезоны
    prior_eps: List[Episode] = [ep for ep in (episodes or []) if int(ep.get("season", 0)) < int(targetSeason)]
    if not prior_eps:
        return {}

    # объединяем требуемые арки по всем сезонам (для запроса к эмбеддингам)
    global_arcs: List[str] = []
    if required_arcs_by_season:
        uni: Set[str] = set()
        for arcs in required_arcs_by_season.values():
            uni |= set(arcs or [])
        global_arcs = sorted(list(uni))

    query_text = _build_query_text(targetSeason, global_arcs, showId)

    # скорим все эпизоды
    scored: List[Tuple[Episode, float]] = scorer.score(prior_eps, query_text)

    # группируем по сезону
    by_season: Dict[int, List[Tuple[Episode, float]]] = defaultdict(list)
    for ep, score in scored:
        s = int(ep.get("season", 0))
        by_season[s].append((ep, score))

    # выбираем минимально достаточный набор по каждому сезону
    result: Dict[int, List[Episode]] = {}
    for season, items in by_season.items():
        items.sort(key=lambda t: t[1], reverse=True)
        needed = set(required_arcs_by_season.get(season, set())) if required_arcs_by_season else set()
        chosen = planner.select_for_season(items, needed, immersion)
        result[season] = chosen

    # сортируем по номеру сезона
    return dict(sorted(result.items(), key=lambda kv: kv[0]))