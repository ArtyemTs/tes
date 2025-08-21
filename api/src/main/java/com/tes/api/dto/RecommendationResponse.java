package com.tes.api.dto;
import java.util.List;
import lombok.Data;
@Data
public class RecommendationResponse {
  private String showId;
  private int targetSeason;
  private int immersion;
  private List<RecommendationItem> items;
}
