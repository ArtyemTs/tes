from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, Dict, Iterable, List, Mapping, Optional, Sequence, Tuple

from .embeddings import EmbeddingsProvider, FakeEmbeddings, cosine_matrix


Episode = Mapping[str, object]  # expects keys like: id, season, episode, title, summary, arcs(optional), vector(optional)


@dataclass
class ScoringConfig:
    # weight knobs for later expansion
    summary_weight: float = 1.0
    title_weight: float = 0.3
    arcs_weight: float = 0.4
    # which episode field to embed if vector is missing
    embed_field: str = "summary"


class EpisodeScorer:
    """
    Produces relevance scores for episodes given a 'query' description.
    Uses embeddings + cosine similarity. If episode already has 'vector'
    (np.ndarray), it will be reused; otherwise, it will be computed from embed_field.
    """
    def __init__(self, embedder: Optional[EmbeddingsProvider] = None, config: Optional[ScoringConfig] = None):
        self.embedder = embedder or FakeEmbeddings()
        self.config = config or ScoringConfig()

    def _episode_text(self, ep: Episode) -> str:
        parts: List[str] = []
        title = str(ep.get("title", "") or "")
        summary = str(ep.get("summary", "") or "")
        arcs = ep.get("arcs", [])
        arcs_text = " ".join(sorted(arcs)) if isinstance(arcs, (list, tuple, set)) else str(arcs or "")
        if title:
            parts.append(("title: " + title) * (1 if self.config.title_weight > 0 else 0))
        if summary:
            parts.append(("summary: " + summary) * (1 if self.config.summary_weight > 0 else 0))
        if arcs_text:
            parts.append(("arcs: " + arcs_text) * (1 if self.config.arcs_weight > 0 else 0))
        return "\n".join([p for p in parts if p])

    def _ensure_vectors(self, episodes: Sequence[Episode]) -> np.ndarray:
        """Return matrix [N, D] of episode vectors; compute if missing."""
        vectors: List[np.ndarray] = []
        missing_texts: List[str] = []
        missing_idx: List[int] = []

        for idx, ep in enumerate(episodes):
            v = ep.get("vector")
            if isinstance(v, np.ndarray):
                vectors.append(v)
            else:
                missing_texts.append(str(ep.get(self.config.embed_field, "") or self._episode_text(ep)))
                missing_idx.append(idx)
                vectors.append(None)  # placeholder

        if missing_texts:
            computed = self.embedder.embed_texts(missing_texts)
            j = 0
            for i in range(len(vectors)):
                if vectors[i] is None:
                    vectors[i] = computed[j]
                    j += 1

        return np.stack(vectors, axis=0)

    def score(self, episodes: Sequence[Episode], query_text: str) -> List[Tuple[Episode, float]]:
        """
        Returns [(episode, score)] sorted by score desc.
        """
        if not episodes:
            return []

        ep_mat = self._ensure_vectors(episodes)                  # [N, D]
        q_vec = self.embedder.embed_text(query_text)[None, :]    # [1, D]
        sims = cosine_matrix(ep_mat, q_vec)[:, 0]                # [N]

        # Pair and sort
        paired = list(zip(episodes, sims.tolist()))
        paired.sort(key=lambda t: t[1], reverse=True)
        return paired