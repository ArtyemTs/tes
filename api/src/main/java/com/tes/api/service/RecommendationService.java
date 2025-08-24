package com.tes.api.service;

import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;

public interface RecommendationService {
  RecommendationResponse recommend(RecommendationRequest req);
}