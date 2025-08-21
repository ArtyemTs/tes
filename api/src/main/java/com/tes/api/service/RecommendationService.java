package com.tes.api.service;
import com.tes.api.client.MlClient;
import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class RecommendationService {
  private final MlClient mlClient;
  public RecommendationResponse getRecommendations(RecommendationRequest req) {
    return mlClient.recommend(req);
  }
}
