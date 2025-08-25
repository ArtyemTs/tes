package com.tes.api.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;
import com.tes.api.service.RecommendationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationServiceHttp implements RecommendationService {

    private final RestClient http;
    private final ObjectMapper om;
    private final String mlUrl;

    public RecommendationServiceHttp(ObjectMapper om,
                                     @Value("${ml.base-url:http://localhost:8000}") String mlBaseUrl) {
        this.om = om;
        this.http = RestClient.create();
        this.mlUrl = mlBaseUrl + "/recommendations";
    }

    @Override
    public RecommendationResponse recommend(RecommendationRequest req) {
        // Формируем payload для ML (ожидаем формат ml/logic.recommend_minimal)
        Map<String, Object> payload = new HashMap<>();
        payload.put("showId", req.showId());
        payload.put("targetSeason", req.targetSeason());
        payload.put("immersion", req.immersion());
        payload.put("required_arcs_by_season", req.requiredArcsBySeason());
        payload.put("episodes", req.episodes()); // может быть null

        ResponseEntity<String> resp = http.post()
                .uri(mlUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toEntity(String.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("ML service error: status=" + resp.getStatusCode());
        }

        // Ожидаем JSON: { "recommendations": { "1": [ {id,season,episode,title,arcs}, ... ], ... } }
        try {
            Map<String, Object> ml = om.readValue(resp.getBody(), new TypeReference<>() {});
            Map<Integer, List<RecommendationResponse.MinimalEpisode>> recs =
                    om.convertValue(((Map<?,?>) ml.get("recommendations")), new TypeReference<>() {});
            return new RecommendationResponse(recs);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse ML response: " + e.getMessage(), e);
        }
    }
}