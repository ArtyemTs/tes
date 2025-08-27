package com.tes.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RecommendationRequest(
        @NotBlank String showId,
        @Min(1) int targetSeason,
        @Min(1) @Max(5) int immersion,
        @Pattern(regexp = "en|ru") String language
) {
    // language по умолчанию — "en"
    public RecommendationRequest {
        if (language == null || language.isBlank()) {
            language = "en";
        }
    }
}