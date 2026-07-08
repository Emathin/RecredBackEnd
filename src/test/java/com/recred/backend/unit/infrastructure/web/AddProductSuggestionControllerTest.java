package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.application.AddProductRecommendationRequest;
import com.recred.backend.infrastructure.web.AddProductSuggestionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddProductSuggestionControllerTest {

    private AddProductRecommendation addProductRecommendation;
    private MockMvc mockMvc;
    private ResultActions response;

    @BeforeEach
    void setUp() {
        addProductRecommendation = mock(AddProductRecommendation.class);
        AddProductSuggestionController controller =
                new AddProductSuggestionController(addProductRecommendation);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void givenProductSuggestionRequest_whenCreatingSuggestion_thenDelegatesToUseCase()
            throws Exception {
        whenCreateProductSuggestion();

        thenResponseIsOk();
        thenAddProductRecommendationWasCalled();
    }

    private void whenCreateProductSuggestion() throws Exception {
        response = mockMvc.perform(post("/api/v1/product-suggestions/crear"));
    }

    private void thenResponseIsOk() throws Exception {
        response.andExpect(status().isOk());
    }

    private void thenAddProductRecommendationWasCalled() {
        verify(addProductRecommendation).execute(new AddProductRecommendationRequest());
    }
}
