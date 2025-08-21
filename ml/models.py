from pydantic import BaseModel, Field
from typing import List, Optional
class RecommendationRequest(BaseModel):
    showId: str
    targetSeason: int = Field(ge=1)
    immersion: int = Field(ge=1, le=5)
    locale: Optional[str] = "en"
class RecommendationItem(BaseModel):
    season: int
    episode: int
    title: Optional[str] = None
    reason: str
class RecommendationResponse(BaseModel):
    showId: str
    targetSeason: int
    immersion: int
    items: List[RecommendationItem]
