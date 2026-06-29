package com.recred.backend.web;

import com.recred.backend.infrastructure.web.*;

import com.recred.backend.application.GenerateComboSuggestion;
import com.recred.backend.domain.models.ComboSuggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ComboSuggestionControllerTest {

    @Mock
    private GenerateComboSuggestion generateComboSuggestion;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ComboSuggestionController(generateComboSuggestion)).build();
    }

    @Test
    void getComboSuggestions_ShouldReturnComboSuggestion() throws Exception {
        UUID productId = UUID.randomUUID();
        String cognitoSubject = "test-sub";
        
        com.recred.backend.application.GenerateComboSuggestionResponse suggestion = 
                new com.recred.backend.application.GenerateComboSuggestionResponse(
                productId,
                "Product Name",
                List.of(new com.recred.backend.application.GenerateComboSuggestionResponse.SuggestedProductResponse(
                        UUID.randomUUID(), "S1", java.math.BigDecimal.TEN))
        );

        com.recred.backend.application.GenerateComboSuggestionRequest request = 
                new com.recred.backend.application.GenerateComboSuggestionRequest(productId, cognitoSubject);

        when(generateComboSuggestion.execute(request)).thenReturn(suggestion);

        org.springframework.security.core.Authentication auth = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn(cognitoSubject);

        mockMvc.perform(get("/api/v1/combo-suggestions/{productId}", productId).principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduct").value(productId.toString()))
                .andExpect(jsonPath("$.productName").value("Product Name"))
                .andExpect(jsonPath("$.suggestedProducts[0].nombre").value("S1"));
    }
}
