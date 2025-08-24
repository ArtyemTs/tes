package com.tes.api.controller;
import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;
import com.tes.api.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
//@RestController
//@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RecommendationController {
  private final RecommendationService service;
  @PostMapping("/recommendations")
  public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request) {
    return service.recommend(request);
  }
}
