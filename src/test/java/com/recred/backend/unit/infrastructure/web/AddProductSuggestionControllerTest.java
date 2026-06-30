package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.AddProductRecommendation;
import com.recred.backend.infrastructure.web.AddProductSuggestionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AddProductSuggestionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AddProductRecommendation addProductRecommendation;

    @InjectMocks
    private AddProductSuggestionController addProductSuggestionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(addProductSuggestionController).build();
    }

    @Test
    void createSuggestionShouldCallUseCase() throws Exception {
        doNothing().when(addProductRecommendation).execute();

        mockMvc.perform(post("/api/v1/product-suggestions/crear"))
                .andExpect(status().isOk());

        verify(addProductRecommendation).execute();
    }
}

