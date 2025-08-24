package com.tes.api.dto;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record RecommendationRequest(
        @NotBlank String showId,
        @Min(1) int targetSeason,
        @Min(1) @Max(5) int immersion,
        // season -> set of arcs
        Map<Integer, Set<String>> requiredArcsBySeason,
        // optional preloaded episodes (for offline/demo)
        List<EpisodeDto> episodes
) {
  public record EpisodeDto(
          @NotBlank String id,
          @Min(1) int season,
          @Min(1) int episode,
          @NotBlank String title,
          @NotBlank String summary,
          List<String> arcs
  ) {}
}