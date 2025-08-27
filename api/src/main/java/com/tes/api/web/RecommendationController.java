package com.tes.api.web;

import com.tes.api.dto.RecommendationRequest;
import com.tes.api.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping(
            path = "/recommendations",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> recommend(@Valid @RequestBody RecommendationRequest request) {
        ResponseEntity<?> resp = recommendationService.recommend(request);
        // Подстраховка: гарантируем application/json
        if (!resp.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE)) {
            return ResponseEntity
                    .status(resp.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(resp.getHeaders())
                    .body(resp.getBody());
        }
        return resp;
    }
}