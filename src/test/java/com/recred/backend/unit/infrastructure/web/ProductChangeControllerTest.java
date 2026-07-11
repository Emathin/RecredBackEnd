package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import com.recred.backend.application.GetProductChangeRecommendation;
import com.recred.backend.application.GetProductChangeRecommendationResponse;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.ProductChangeController;
import com.recred.backend.infrastructure.web.ProductChangeRecommendationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductChangeControllerTest {

    private ProductChangeController controller;

    @Mock
    private GenerateProductChangeRecommendation generateProductChangeRecommendation;

    @Mock
    private GetProductChangeRecommendation getProductChangeRecommendation;

    @Mock
    private PlanAccessGuard planAccessGuard;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ProductChangeController(
                getProductChangeRecommendation,
                planAccessGuard
        );
    }

    @Test
    void findAllProductChangeRecommendationsShouldReturnList() {
        Authentication auth = givenAuthentication();
        givenRecommendations();

        ResponseEntity<List<ProductChangeRecommendationResponse>> response = 
                whenFindingAllRecommendations(auth);

        thenResponseIsOkAndHasOneElement(response);
        thenAccessGuardIsVerified(auth);
    }

    private Authentication givenAuthentication() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test-sub");
        return auth;
    }

    private void givenRecommendations() {
        GetProductChangeRecommendationResponse dto =
                new GetProductChangeRecommendationResponse(
                        UUID.randomUUID(),
                        "Alfajor",
                        BigDecimal.TEN,
                        "Resumen",
                        List.of(),
                        List.of(),
                        "model",
                        null
                );
        when(getProductChangeRecommendation.execute(any())).thenReturn(List.of(dto));
    }

    private ResponseEntity<List<ProductChangeRecommendationResponse>> whenFindingAllRecommendations(
            Authentication auth
    ) {
        return controller.findAllProductChangeRecommendations(auth);
    }

    private void thenResponseIsOkAndHasOneElement(
            ResponseEntity<List<ProductChangeRecommendationResponse>> response
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    private void thenAccessGuardIsVerified(Authentication auth) {
        verify(planAccessGuard).requireVendorAdvanced(auth);
    }
}
