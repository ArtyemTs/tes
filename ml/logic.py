import yaml
from typing import Dict, Any, List
from models import RecommendationRequest, RecommendationItem, RecommendationResponse

def load_dataset(path: str) -> Dict[str, Any]:
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)

def recommend(req: RecommendationRequest, data: Dict[str, Any]) -> RecommendationResponse:
    show = data["shows"].get(req.showId)
    if not show:
        return RecommendationResponse(showId=req.showId, targetSeason=req.targetSeason, immersion=req.immersion, items=[])
    threshold = {1:5, 2:4, 3:3, 4:2, 5:1}[req.immersion]
    items: List[RecommendationItem] = []
    for season in show["seasons"]:
        if season["number"] >= req.targetSeason:
            break
        for ep in season["episodes"]:
            imp = ep.get("importance", 3)
            if imp >= threshold:
                reason_parts = []
                 if ep.get("arcs"):
                     reason_parts.append("arcs: " + ", ".join(ep["arcs"][:3]))
                 if ep.get("summary_short"):
                     reason_parts.append(ep["summary_short"])
                reason = " — ".join(reason_parts) if reason_parts else "Key plot episode"
                items.append(RecommendationItem(season=season["number"], episode=ep["number"], title=ep.get("title"), reason=reason))
    core_arcs = set(show.get("core_arcs", []))
    covered = set()
    for it in items:
        for ep in next(s for s in show["seasons"] if s["number"]==it.season)["episodes"]:
            if ep["number"]==it.episode:
                covered.update(ep.get("arcs", []))
    missing = list(core_arcs - covered)
    if missing:
        for arc in missing:
            candidate = None
            for season in show["seasons"]:
                if season["number"] >= req.targetSeason:
                    break
                for ep in sorted(season["episodes"], key=lambda e: -e.get("importance",3)):
                    if arc in ep.get("arcs", []):
                        candidate = (season["number"], ep)
                        break
                if candidate:
                    break
            if candidate:
                snum, ep = candidate
                if not any((it.season==snum and it.episode==ep["number"]) for it in items):
                    items.append(RecommendationItem(season=snum, episode=ep["number"], title=ep.get("title"), reason=f"core arc: {arc}"))
    items.sort(key=lambda x: (x.season, x.episode))
    return RecommendationResponse(showId=req.showId, targetSeason=req.targetSeason, immersion=req.immersion, items=items)
