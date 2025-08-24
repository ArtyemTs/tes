from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable, List, Mapping, Optional, Sequence, Set, Tuple


Episode = Mapping[str, object]


@dataclass
class CoverageConfig:
    """
    Control how aggressively we cover arcs.
    immersion in [1..5]: higher -> more episodes.
    """
    max_per_season_by_immersion: dict[int, int] = None

    def __post_init__(self):
        # sensible defaults
        if self.max_per_season_by_immersion is None:
            self.max_per_season_by_immersion = {
                1: 1,  # ultra‑minimal
                2: 2,
                3: 3,
                4: 4,
                5: 6,  # generous
            }

    def max_per_season(self, immersion: int) -> int:
        return self.max_per_season_by_immersion.get(max(1, min(5, immersion)), 3)


class CoveragePlanner:
    """
    Greedy set‑cover over arcs with a hard cap per season derived from immersion.
    Inputs are per-season lists of (episode, score).
    """
    def __init__(self, config: Optional[CoverageConfig] = None):
        self.config = config or CoverageConfig()

    @staticmethod
    def _episode_arcs(ep: Episode) -> Set[str]:
        v = ep.get("arcs", [])
        if isinstance(v, (list, tuple, set)):
            return set([str(x) for x in v])
        return set(str(v).split()) if v else set()

    def select_for_season(
        self,
        season_episodes: Sequence[Tuple[Episode, float]],
        required_arcs: Set[str],
        immersion: int,
    ) -> List[Episode]:
        """
        Greedy: pick highest-scored episode that covers the most uncovered arcs
        until we either cover all required_arcs or hit the per-season cap.
        """
        cap = self.config.max_per_season(immersion)
        uncovered = set(required_arcs)
        selected: List[Episode] = []

        # Sort by score desc once; within loop compute coverage gain
        candidates = list(season_episodes)

        while candidates and len(selected) < cap and (uncovered or not required_arcs):
            best_idx = -1
            best_gain = -1
            best_score = float("-inf")
            for idx, (ep, score) in enumerate(candidates):
                arcs = self._episode_arcs(ep)
                gain = len(arcs & uncovered) if uncovered else 0
                # tie-break by score
                if gain > best_gain or (gain == best_gain and score > best_score):
                    best_gain = gain
                    best_score = score
                    best_idx = idx

            if best_idx == -1:
                break

            ep, _ = candidates.pop(best_idx)
            selected.append(ep)
            uncovered -= self._episode_arcs(ep)

        return selected