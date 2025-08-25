from ml.coverage import CoveragePlanner

def test_coverage_selects_required_arcs():
    planner = CoveragePlanner()
    episodes = [
        ({"id": "e1", "season": 1, "arcs": ["a"]}, 0.9),
        ({"id": "e2", "season": 1, "arcs": ["b"]}, 0.8),
        ({"id": "e3", "season": 1, "arcs": ["c"]}, 0.7),
    ]
    required = {"a", "b"}
    selected = planner.select_for_season(episodes, required, immersion=3)
    arc_union = set().union(*(ep["arcs"] for ep in selected))
    assert required.issubset(arc_union)
    assert len(selected) <= 3  # immersion cap=3

def test_coverage_respects_cap():
    planner = CoveragePlanner()
    episodes = [
        ({"id": f"e{i}", "season": 1, "arcs": [str(i)]}, 1.0 / (i + 1))
        for i in range(10)
    ]
    selected = planner.select_for_season(episodes, required_arcs=set(), immersion=1)
    assert len(selected) <= 1  # cap for immersion=1