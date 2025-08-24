from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache
from hashlib import blake2b
from typing import Iterable, List, Protocol

import math
import numpy as np


class Embedder(Protocol):
    """Interface for text embedders."""

    def embed_text(self, text: str) -> np.ndarray: ...
    def embed_batch(self, texts: Iterable[str]) -> List[np.ndarray]: ...


@dataclass(frozen=True)
class FakeEmbedder(Embedder):
    """
    Deterministic, offline fake embedder for MVP/testing.
    Generates fixed-length vectors from text using a seeded PRNG.
    """
    dim: int = 128

    def _seed_from_text(self, text: str) -> int:
        h = blake2b(text.encode("utf-8"), digest_size=8).digest()
        return int.from_bytes(h, "little")

    @lru_cache(maxsize=4096)
    def embed_text(self, text: str) -> np.ndarray:
        rng = np.random.default_rng(self._seed_from_text(text))
        v = rng.normal(loc=0.0, scale=1.0, size=self.dim).astype(np.float32)
        # L2-normalize for stable cosine
        norm = np.linalg.norm(v) + 1e-12
        return (v / norm).astype(np.float32)

    def embed_batch(self, texts: Iterable[str]) -> List[np.ndarray]:
        return [self.embed_text(t) for t in texts]


def cosine(a: np.ndarray, b: np.ndarray) -> float:
    """Cosine similarity for L2-normalized vectors."""
    # tolerate non-normalized inputs
    an = float(np.linalg.norm(a)) or 1.0
    bn = float(np.linalg.norm(b)) or 1.0
    return float(np.dot(a, b) / (an * bn))


def cosdist(a: np.ndarray, b: np.ndarray) -> float:
    """Cosine distance (1 - cosine). Useful if you need a distance metric."""
    return 1.0 - cosine(a, b)