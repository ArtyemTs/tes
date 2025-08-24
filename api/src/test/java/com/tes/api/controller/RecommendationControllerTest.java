package com.tes.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tes.api.dto.RecommendationItem;
import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;
import com.tes.api.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(controllers = RecommendationController.class)
class RecommendationControllerTest {
    @Autowired
    MockMvc mvc;
    @MockBean
    RecommendationService service;
    ObjectMapper mapper = new ObjectMapper();

    @Test
    void recommend_returnsItems() throws Exception {
        RecommendationResponse mockResp = new RecommendationResponse();
        mockResp.setShowId("got");
        mockResp.setTargetSeason(4);
        mockResp.setImmersion(2);
        mockResp.setItems(List.of(new RecommendationItem(1, 1, "reason", "Winter Is Coming")));

        Mockito.when(service.getRecommendations(Mockito.any(RecommendationRequest.class)))
                .thenReturn(mockResp);

        var req = new RecommendationRequest();
        req.setShowId("got");
        req.setTargetSeason(4);
        req.setImmersion(2);
        req.setLocale("en");

        mvc.perform(post("/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showId", is("got")))
                .andExpect(jsonPath("$.items[0].season", is(1)))
                .andExpect(jsonPath("$.items[0].episode", is(1)));
    }

    @Test
    void targetSeason6_returnsOnlyS1toS5_fromServiceResponse() throws Exception {
        // Arrange
        var mockService = Mockito.mock(RecommendationService.class);
        var controller = new RecommendationController(mockService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        var req = new RecommendationRequest();
        req.setShowId("got");
        req.setTargetSeason(6);
        req.setImmersion(2);

        var resp = new RecommendationResponse();
        resp.setShowId("got");
        resp.setTargetSeason(6);
        resp.setImmersion(2);
        resp.setItems(List.of(
                new RecommendationItem(1, 1, "arcs: White Walkers — intro", "Winter Is Coming"),
                new RecommendationItem(5, 8, "core arc: White Walkers", "Hardhome")
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

    @Test
    @DisplayName("POST /recommendations returns items from service (contract test)")
    void recommend_ok() throws Exception {
        // Arrange: mock service response
        RecommendationResponse resp = new RecommendationResponse();
        resp.setShowId("got");
        resp.setTargetSeason(4);
        resp.setImmersion(2);
        resp.setItems(List.of(
                new RecommendationItem(1, 1, "Introduces Starks; first White Walker threat.", "Winter Is Coming"),
                new RecommendationItem(1, 9, "Shock execution pivots the realm into war.", "Baelor")
        ));

        Mockito.when(service.getRecommendations(Mockito.any()))
                .thenReturn(resp);

        // Act  Assert
        RecommendationRequest req = new RecommendationRequest();
        req.setShowId("got");
        req.setTargetSeason(4);
        req.setImmersion(2);
        req.setLocale("en");

        mvc.perform(post("/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.showId").value("got"))
                .andExpect(jsonPath("$.targetSeason").value(4))
                .andExpect(jsonPath("$.immersion").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].season").value(1))
                .andExpect(jsonPath("$.items[0].episode").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Winter Is Coming"))
                .andExpect(jsonPath("$.items[0].reason", containsString("Introduces")));
    }

    @Test
    @DisplayName("POST /recommendations fails validation on bad payload")
    void recommend_validation_error() throws Exception {
        String badJson = """
                {"showId": "", "targetSeason": 0, "immersion": 9}
                """;

        mvc.perform(post("/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Validation failed")))
                .andExpect(jsonPath("$.violations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.violations[*].field", hasItems("showId", "targetSeason", "immersion")));
    }
}


//class RecommendationControllerTest {
//
//          @Autowired MockMvc mvc;
//  @Autowired ObjectMapper mapper;
//
//          @MockBean RecommendationService service;
//

//}