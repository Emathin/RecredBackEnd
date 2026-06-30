package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import com.recred.backend.application.GetProductChangeRecommendation;
import com.recred.backend.application.GetProductChangeRecommendationResponse;
import com.recred.backend.infrastructure.web.ProductChangeController;
import com.recred.backend.infrastructure.web.ProductChangeRecommendationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductChangeControllerTest {

    private ProductChangeController controller;

    @Mock
    private GenerateProductChangeRecommendation generateProductChangeRecommendation;

    @Mock
    private GetProductChangeRecommendation getProductChangeRecommendation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ProductChangeController(
                getProductChangeRecommendation
        );
    }

    @Test
    void findAllProductChangeRecommendations_ShouldReturnList() {
        UUID usuarioId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        GetProductChangeRecommendationResponse dto =
                new GetProductChangeRecommendationResponse(
                        recommendationId,
                        "Alfajor",
                        BigDecimal.TEN,
                        "Resumen",
                        List.of(),
                        List.of(),
                        "model",
                        null
                );

        String cognitoSubject = "test-sub";
        org.springframework.security.core.Authentication auth = 
                org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn(cognitoSubject);

        when(getProductChangeRecommendation.execute(any()))
                .thenReturn(List.of(dto));

        ResponseEntity<List<ProductChangeRecommendationResponse>> response = controller
                .findAllProductChangeRecommendations(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

}

