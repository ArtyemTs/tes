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

import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationMlDownTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockBean
    MlClient mlClient;

    @Test
    void shouldReturn503WithProblemJsonWhenMlDown() throws Exception {
        when(mlClient.recommendAsync(ArgumentMatchers.anyMap()))
                .thenReturn(CompletableFuture.failedFuture(new ConnectException("ML down")));

        var payload = Map.of(
                "showId", "got",
                "targetSeason", 6,
                "immersion", 3,
                "language", "en"
        );

        mvc.perform(post("/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(payload)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("TES-002"));
    }
}