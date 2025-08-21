from fastapi import FastAPI
from models import RecommendationRequest, RecommendationResponse
from logic import load_dataset, recommend
import os

app = FastAPI(title="TES ML Service", version="0.1.0")
DATA_PATH = os.environ.get("DATA_PATH", "/app/data/got.yaml")
DATA = load_dataset(DATA_PATH)

@app.post("/recommend", response_model=RecommendationResponse)
def recommend_endpoint(req: RecommendationRequest):
    return recommend(req, DATA)
