package com.tes.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tes.api.dto.RecommendationItem;
import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;
import com.tes.api.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecommendationControllerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void targetSeason6_returnsOnlyS1toS5_fromServiceResponse() throws Exception {
        // Arrange
        var mockService = Mockito.mock(RecommendationService.class);
        var controller = new RecommendationController(mockService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        var req = new RecommendationRequest();
        req.setShowId("got"); req.setTargetSeason(6); req.setImmersion(2);

        var resp = new RecommendationResponse();
        resp.setShowId("got"); resp.setTargetSeason(6); resp.setImmersion(2);
        resp.setItems(List.of(
                new RecommendationItem(1,1,"arcs: White Walkers — intro","Winter Is Coming"),
                new RecommendationItem(5,8,"core arc: White Walkers","Hardhome")
        ));
        Mockito.when(mockService.getRecommendations(Mockito.any())).thenReturn(resp);

        // Act & Assert
        mvc.perform(post("/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.season>=6)]").doesNotExist())
                .andExpect(jsonPath("$.items[?(@.season==5 && @.episode==8)]").exists());
    }
}