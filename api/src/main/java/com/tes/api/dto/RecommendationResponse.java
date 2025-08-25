package com.tes.api.dto;

import java.util.List;
import java.util.Map;

public record RecommendationResponse(
        Map<Integer, List<MinimalEpisode>> recommendations
) {
    public record MinimalEpisode(
            String id, int season, int episode, String title, List<String> arcs
    ) {
    }
}