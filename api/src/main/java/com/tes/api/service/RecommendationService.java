package com.tes.api.service;

import com.tes.api.dto.RecommendationRequest;
import org.springframework.http.ResponseEntity;

public interface RecommendationService {
    ResponseEntity<?> recommend(RecommendationRequest request);
}