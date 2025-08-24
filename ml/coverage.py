from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from math import ceil
from typing import Dict, Iterable, List, Set


class SupportsArcs:
    """Minimal episode interface needed for coverage."""
    id: str
    arcs: List[str]
    season: int
    number: int


@dataclass(frozen=True)
class CoverageResult:
    selected_ids: List[str]
    covered_arcs: Set[str]
    uncovered_arcs: Set[str]


def greedy_arc_coverage(
    episodes: Iterable[SupportsArcs],
    required_arcs: Set[str],
) -> CoverageResult:
    """
    Greedy set cover: pick episode that covers the most *uncovered* required arcs.
    Stops when all arcs are covered or no progress can be made.
    """
    required = set(required_arcs)
    selected: List[str] = []
    covered: Set[str] = set()

    # Pre-index arcs -> episodes
    arc_to_eps: Dict[str, Set[str]] = defaultdict(set)
    id_to_arcs: Dict[str, Set[str]] = {}
    for ep in episodes:
        s = set(ep.arcs or [])
        id_to_arcs[ep.id] = s
        for a in s:
            arc_to_eps[a].add(ep.id)

    while True:
        target = required - covered
        if not target:
            break

        best_id = None
        best_gain = 0
        for ep_id, arcs in id_to_arcs.items():
            gain = len((arcs & required) - covered)
            if gain > best_gain and ep_id not in selected:
                best_id = ep_id
                best_gain = gain

        if not best_id or best_gain == 0:
            # cannot progress further
            break

        selected.append(best_id)
        covered |= (id_to_arcs[best_id] & required)

    return CoverageResult(
        selected_ids=selected,
        covered_arcs=covered,
        uncovered_arcs=(required - covered),
    )


def per_season_budget(total_eps: int, immersion_level: int) -> int:
    """
    Heuristic budget by immersion level (1..5).
    Low immersion -> small subset, High -> larger.
    """
    immersion_level = max(1, min(5, immersion_level))
    # 10% + 10% per immersion step (min 1, max 8)
    frac = 0.10 + 0.10 * immersion_level
    k = ceil(total_eps * frac)
    return max(1, min(8, k))
