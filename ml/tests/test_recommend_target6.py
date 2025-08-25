# ml/tests/test_recommend_target6.py
from ml.models import RecommendationRequest
from ml.logic import recommend

DATA = {
    "shows": {
        "got": {
            "title": "Game of Thrones",
            "core_arcs": ["War of the Five Kings", "White Walkers", "Daenerys Rise", "Stark Family"],
            "seasons": [
                # S1 (в т.ч. первый намёк на White Walkers)
                {"number": 1, "episodes": [
                    {"number": 1, "title": "Winter Is Coming", "importance": 5,
                     "arcs": ["Stark Family", "White Walkers"],
                     "summary_short": "Introduces Starks; first White Walker threat."}
                ]},
                # S2
                {"number": 2, "episodes": [
                    {"number": 9, "title": "Blackwater", "importance": 5,
                     "arcs": ["War of the Five Kings"], "summary_short": "Battle at King's Landing"}
                ]},
                # S3
                {"number": 3, "episodes": [
                    {"number": 9, "title": "The Rains of Castamere", "importance": 5,
                     "arcs": ["Stark Family", "War of the Five Kings"], "summary_short": "Red Wedding"}
                ]},
                # S4 (минимал)
                {"number": 4, "episodes": [
                    {"number": 2, "title": "The Lion and the Rose", "importance": 4,
                     "arcs": ["War of the Five Kings"], "summary_short": "Major political shift"}
                ]},
                # S5 — ключевое: White Walkers на E8 (для DoD)
                {"number": 5, "episodes": [
                    {"number": 8, "title": "Hardhome", "importance": 5,
                     "arcs": ["White Walkers"], "summary_short": "Major WW event at Hardhome"}
                ]},
                # S6 (не должен попасть)
                {"number": 6, "episodes": [
                    {"number": 1, "title": "The Red Woman", "importance": 5,
                     "arcs": ["Stark Family"], "summary_short": "Post-S5 aftermath"}
                ]},
            ]
        }
    }
}

def test_targetSeason_6_only_S1_to_S5_and_ww_covered():
    req = RecommendationRequest(showId="got", targetSeason=6, immersion=2, locale="en")
    res = recommend(req, DATA)
    # 1) Все сезоны < 6
    assert all(item.season < 6 for item in res.items), "Should only include S1–S5"
    # 2) Покрыт core arc White Walkers (напрямую или через coverage добор)
    #    Минимальная проверка — есть эпизод с этой аркой, желательно S5E8
    has_ww = any("White Walkers" in item.reason for item in res.items) or \
              any((it.season==5 and it.episode==8) for it in res.items)
    assert has_ww, "White Walkers arc should be covered (preferably via S5E8 Hardhome)"
    # 3) Бонус: убедимся, что S5E8 присутствует (при importance>=threshold он попадет)
    assert any((it.season==5 and it.episode==8) for it in res.items), "S5E8 expected"