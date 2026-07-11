package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.*;

import com.recred.backend.application.GenerateComboSuggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ComboSuggestionControllerTest {

    @Mock
    private GenerateComboSuggestion generateComboSuggestion;

    @Mock
    private PlanAccessGuard planAccessGuard;

    private MockMvc mockMvc;
    private Authentication auth;

    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final String COGNITO_SUBJECT = "test-sub";
    private final String PRODUCT_NAME = "Product Name";
    private final String SUGGESTED_NAME = "S1";

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ComboSuggestionController(
                generateComboSuggestion,
                planAccessGuard
        )).build();
        auth = mock(Authentication.class);
    }

    @Test
    void getComboSuggestionsShouldReturnComboSuggestion() throws Exception {
        givenAuthenticatedUser();
        givenComboSuggestionGenerated();
        
        ResultActions response = whenGettingComboSuggestions();
        
        thenSuggestionResponseIsOk(response);
        thenDelegatesToGenerateComboSuggestion();
        thenValidatesVendorPlan();
    }

    private void givenAuthenticatedUser() {
        when(auth.getName()).thenReturn(COGNITO_SUBJECT);
    }

    private void givenComboSuggestionGenerated() {
        com.recred.backend.application.GenerateComboSuggestionResponse suggestion = 
                new com.recred.backend.application.GenerateComboSuggestionResponse(
                PRODUCT_ID,
                PRODUCT_NAME,
                List.of(new com.recred.backend.application.GenerateComboSuggestionResponse.SuggestedProductResponse(
                        UUID.randomUUID(), SUGGESTED_NAME, java.math.BigDecimal.TEN))
        );

        com.recred.backend.application.GenerateComboSuggestionRequest request = 
                new com.recred.backend.application.GenerateComboSuggestionRequest(PRODUCT_ID, COGNITO_SUBJECT);

        when(generateComboSuggestion.execute(request)).thenReturn(suggestion);
    }

    private ResultActions whenGettingComboSuggestions() throws Exception {
        return mockMvc.perform(get("/api/v1/combo-suggestions/{productId}", PRODUCT_ID).principal(auth));
    }

    private void thenSuggestionResponseIsOk(ResultActions response) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduct").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.productName").value(PRODUCT_NAME))
                .andExpect(jsonPath("$.suggestedProducts[0].nombre").value(SUGGESTED_NAME));
    }

    private void thenDelegatesToGenerateComboSuggestion() {
        verify(generateComboSuggestion).execute(
                new com.recred.backend.application.GenerateComboSuggestionRequest(PRODUCT_ID, COGNITO_SUBJECT));
    }

    private void thenValidatesVendorPlan() {
        verify(planAccessGuard).requireVendorAdvanced(auth);
    }
}
