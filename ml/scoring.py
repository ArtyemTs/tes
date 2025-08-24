from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, Iterable, List, Protocol

import numpy as np

from .embeddings import Embedder, cosine


class SupportsEmbedding(Protocol):
    """Minimal episode interface needed for scoring."""
    id: str
    synopsis: str


@dataclass(frozen=True)
class EpisodeScore:
    episode_id: str
    score: float


def score_episodes_by_query(
    episodes: Iterable[SupportsEmbedding],
    query_text: str,
    embedder: Embedder,
    synopsis_embedding_cache: Dict[str, np.ndarray] | None = None,
) -> List[EpisodeScore]:
    """
    Scores episodes by cosine(query, episode_synopsis).
    synopsis_embedding_cache lets you reuse precomputed embeddings by episode_id.
    """
    synopsis_embedding_cache = synopsis_embedding_cache or {}

    q_vec = embedder.embed_text(query_text)
    scored: List[EpisodeScore] = []

    for ep in episodes:
        if ep.id in synopsis_embedding_cache:
            e_vec = synopsis_embedding_cache[ep.id]
        else:
            e_vec = embedder.embed_text(ep.synopsis)
            synopsis_embedding_cache[ep.id] = e_vec
        scored.append(EpisodeScore(episode_id=ep.id, score=cosine(q_vec, e_vec)))

    scored.sort(key=lambda s: s.score, reverse=True)
    return scored