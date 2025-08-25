from ml.scoring import EpisodeScorer
from ml.embeddings import FakeEmbeddings

def test_scoring_orders_by_similarity():
    scorer = EpisodeScorer(embedder=FakeEmbeddings(dim=8))
    episodes = [
        {"id": "e1", "season": 1, "title": "Intro", "summary": "Alice meets Bob"},
        {"id": "e2", "season": 1, "title": "Conflict", "summary": "Alice fights Carl"},
    ]
    results = scorer.score(episodes, query_text="conflict with Carl")
    # Top result must be the episode about conflict with Carl
    assert results[0][0]["id"] == "e2"
    assert results[0][1] >= results[1][1]

def test_scoring_returns_empty_for_no_episodes():
    scorer = EpisodeScorer(embedder=FakeEmbeddings())
    results = scorer.score([], "any query")
    assert results == []