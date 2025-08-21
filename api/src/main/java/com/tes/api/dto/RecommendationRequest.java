package com.tes.api.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class RecommendationRequest {
  @NotBlank private String showId;
  @Min(1) private int targetSeason;
  @Min(1) @Max(5) private int immersion;
  private String locale;
}
