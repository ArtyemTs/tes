package com.tes.api.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tes.api.client.MlClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationContractTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockBean
    MlClient mlClient;

    @Test
    void shouldReturn200WithRecommendationsMap() throws Exception {
        int season = 6;

        Map<String, Object> success = Map.of(
                "recommendations", Map.of(
                        String.valueOf(season),
                        List.of(Map.of(
                                "id", "S" + season + "E1",
                                "season", season,
                                "episode", 1,
                                "title", "Starter",
                                "arcs", List.of("test")
                        ))
                )
        );

        when(mlClient.recommendAsync(ArgumentMatchers.anyMap()))
                .thenReturn(CompletableFuture.completedFuture(success));

        var payload = Map.of(
                "showId", "got",
                "targetSeason", season,
                "immersion", 3,
                "language", "en"
        );

        mvc.perform(post("/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.recommendations").isMap())
                .andExpect(jsonPath("$.recommendations.6").isArray())
                .andExpect(jsonPath("$.recommendations.6[0].id").value("S6E1"))
                .andExpect(jsonPath("$.recommendations.6[0].season").value(6))
                .andExpect(jsonPath("$.recommendations.6[0].episode").value(1));
    }
}