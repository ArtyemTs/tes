package com.tes.api.client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class MlClient {
  private final ObjectMapper mapper = new ObjectMapper();
  @Value("${ml.base-url}") private String baseUrl;
  public RecommendationResponse recommend(RecommendationRequest request) {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost post = new HttpPost(baseUrl + "/recommend");
      post.setEntity(new StringEntity(mapper.writeValueAsString(request), ContentType.APPLICATION_JSON));
      return client.execute(post, resp -> mapper.readValue(resp.getEntity().getContent(), RecommendationResponse.class));
    } catch (Exception e) {
      throw new RuntimeException("Failed to call ML service: " + e.getMessage(), e);
    }
  }
}
