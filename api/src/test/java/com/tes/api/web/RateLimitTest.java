package com.tes.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tes.api.client.MlClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "ratelimit.per-ip.capacity=3",
        "ratelimit.per-ip.refill-period=PT1H"
})
class RateLimitTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean MlClient mlClient;

    @Test
    void returns429AfterLimitExceeded() throws Exception {
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

        var payload = Map.of("showId","got","targetSeason",season,"immersion",3,"language","en");

        // 1–3: 200 OK
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/recommendations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        // 4: 429 Too Many Requests
        mvc.perform(post("/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(payload)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("TES-003"))
                .andExpect(jsonPath("$.status").value(429));
    }
}