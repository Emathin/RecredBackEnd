package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GenerateSeasonalRecommendation;
import com.recred.backend.infrastructure.web.RecommendationResponseDTO;
import com.recred.backend.application.GenerateSeasonalRecommendationRequest;
import com.recred.backend.application.SeasonalSuggestionResponse;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.SeasonalRecommendationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonalRecommendationControllerTest {

    @Mock private GenerateSeasonalRecommendation useCase;
    @Mock private PlanAccessGuard planAccessGuard;

    private SeasonalRecommendationController controller;

    @BeforeEach
    void setUp() {
        controller = new SeasonalRecommendationController(useCase, planAccessGuard);
    }

    @Test
    void givenUseCaseRespondsSuccessfullyWhenEndpointIsCalledThenReturns200WithResponse() {
        givenSuccessfulRecommendation();

        ResponseEntity<RecommendationResponseDTO> response = whenGettingSeasonalRecommendation();

        thenResponseIsOkAndMatches(response);
    }

    @Test
    void givenUseCaseThrowsExceptionWhenEndpointIsCalledThenExceptionIsPropagated() {
        givenUseCaseThrowsException();

        Executable execution = this::whenGettingSeasonalRecommendation;

        thenExceptionIsPropagated(execution);
    }

    private void givenSuccessfulRecommendation() {
        com.recred.backend.application.SeasonalRecommendationResponse answer = new com.recred.backend.application.SeasonalRecommendationResponse(
                List.of(new SeasonalSuggestionResponse(
                        "Bebidas Frias",
                        "AUMENTAR",
                        "Clima caluroso.",
                        null,
                        List.of()
                )),
                "Promo 2x1 en helados.",
                null,
                null,
                null,
                null
        );
        when(useCase.execute(any(GenerateSeasonalRecommendationRequest.class))).thenReturn(answer);
    }

    private void givenUseCaseThrowsException() {
        when(useCase.execute(any(GenerateSeasonalRecommendationRequest.class)))
                .thenThrow(new RuntimeException("Weather service is currently unavailable"));
    }

    private ResponseEntity<RecommendationResponseDTO> whenGettingSeasonalRecommendation() {
        return controller.getSeasonalRecommendations(null, -34.7, -58.47, null);
    }

    private void thenResponseIsOkAndMatches(ResponseEntity<RecommendationResponseDTO> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Promo 2x1 en helados.", response.getBody().getPromocionalTip());
        assertEquals("Bebidas Frias", response.getBody().getSugerences().get(0).getCategoria());
        assertEquals("AUMENTAR", response.getBody().getSugerences().get(0).getAccion());
        
        ArgumentCaptor<GenerateSeasonalRecommendationRequest> captor = 
                ArgumentCaptor.forClass(GenerateSeasonalRecommendationRequest.class);
        verify(useCase).execute(captor.capture());
        assertEquals(-34.7, captor.getValue().latitude());
        assertEquals(-58.47, captor.getValue().longitude());
        verify(planAccessGuard).requireVendorAdvanced(null);
    }

    private void thenExceptionIsPropagated(Executable execution) {
        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("Weather service is currently unavailable", exception.getMessage());
    }
}
