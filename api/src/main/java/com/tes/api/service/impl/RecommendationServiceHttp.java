package com.tes.api.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tes.api.client.MlClient;
import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;
import com.tes.api.service.RecommendationService;
import com.tes.api.web.MlTimeoutException;
import com.tes.api.web.MlUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class RecommendationServiceHttp implements RecommendationService {

    private final MlClient mlClient;
    private final ObjectMapper om;

    // Доп. таймаут ожидания Future (основные таймауты — в MlClient/Resilience4j)
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(4);

    public RecommendationServiceHttp(MlClient mlClient, ObjectMapper om) {
        this.mlClient = Objects.requireNonNull(mlClient, "mlClient");
        this.om = Objects.requireNonNull(om, "objectMapper");
    }

    @Override
    public ResponseEntity<?> recommend(RecommendationRequest req) {
        Map<String, Object> payload = Map.of(
                "showId", req.showId(),
                "targetSeason", req.targetSeason(),
                "immersion", req.immersion(),
                "language", req.language()
        );

        Map<String, Object> mlResponse;
        try {
            mlResponse = mlClient.recommendAsync(payload)
                    .get(CALL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new MlTimeoutException("ML request timed out", e);  // → 504 TES-004
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            boolean cbOpen = (cause != null && "CallNotPermittedException".equals(cause.getClass().getSimpleName()));
            boolean connectIssue = (cause instanceof ConnectException);
            if (cbOpen || connectIssue) {
                throw new MlUnavailableException("ML service unavailable", cause); // → 503 TES-002
            }
            throw new IllegalStateException("ML call failed: " + (cause != null ? cause.getMessage() : e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MlTimeoutException("ML request interrupted", e); // → 504 TES-004
        }

        // Ожидаем от ML: {"recommendations": { "1": [ {id,season,episode,title,arcs}, ... ], ... } }
        Object raw = mlResponse.get("recommendations");
        if (!(raw instanceof Map<?, ?> rawMap)) {
            throw new IllegalStateException("Invalid ML payload: missing 'recommendations'");
        }

        Map<Integer, List<RecommendationResponse.MinimalEpisode>> recs =
                om.convertValue(rawMap, new TypeReference<>() {});
        RecommendationResponse body = new RecommendationResponse(recs);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}