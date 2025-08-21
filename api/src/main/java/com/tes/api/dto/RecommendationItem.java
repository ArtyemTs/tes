package com.tes.api.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class RecommendationItem {
  private int season;
  private int episode;
  private String reason;
  private String title;
}
