from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable, List, Protocol, Sequence
import hashlib
import math
import numpy as np


class EmbeddingsProvider(Protocol):
    """Interface for embedding backends."""
    def embed_text(self, text: str) -> np.ndarray: ...
    def embed_texts(self, texts: Sequence[str]) -> np.ndarray: ...


def _stable_hash_to_float(seed_text: str) -> float:
    """Map string -> deterministic float in [-1, 1]."""
    h = hashlib.sha256(seed_text.encode("utf-8")).hexdigest()
    # take 16 hex chars -> int -> normalize
    val = int(h[:16], 16) / float(0xFFFFFFFFFFFFFFFF)
    return (val * 2.0) - 1.0


@dataclass
class FakeEmbeddings(EmbeddingsProvider):
    """
    Deterministic 'fake' embeddings for MVP.
    - Dimension fixed, values derived from stable hashes.
    - No external deps beyond numpy.
    """
    dim: int = 64

    def embed_text(self, text: str) -> np.ndarray:
        # Derive dim floats from hash(text + position)
        vals: List[float] = []
        for i in range(self.dim):
            vals.append(_stable_hash_to_float(f"{i}:{text}"))
        v = np.array(vals, dtype=np.float32)
        # L2 normalize to behave like real embeddings
        n = np.linalg.norm(v)
        return v / (n + 1e-12)

    def embed_texts(self, texts: Sequence[str]) -> np.ndarray:
        return np.stack([self.embed_text(t) for t in texts], axis=0)


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    """Cosine similarity for two vectors."""
    denom = (np.linalg.norm(a) * np.linalg.norm(b)) + 1e-12
    return float(np.dot(a, b) / denom)


def cosine_matrix(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    """
    Cosine similarities between all rows of a and b.
    a: [N, D], b: [M, D] -> [N, M]
    """
    a_norm = a / (np.linalg.norm(a, axis=1, keepdims=True) + 1e-12)
    b_norm = b / (np.linalg.norm(b, axis=1, keepdims=True) + 1e-12)
    return np.matmul(a_norm, b_norm.T)