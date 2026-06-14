package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class ProductChangeControllerTest {

    private ProductChangeController controller;

    @Mock
    private GenerateProductChangeRecommendation generateProductChangeRecommendation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ProductChangeController(generateProductChangeRecommendation);
    }

    @Test
    void generateProductChangeRecommendation_ShouldReturnNoContentWhenNotFound() {
        UUID usuarioId = UUID.randomUUID();
        when(generateProductChangeRecommendation.findLatestResultByUsuarioId(usuarioId))
                .thenReturn(Optional.empty());

        ResponseEntity<ProductChangeRecommendationResponse> response = controller
                .generateProductChangeRecommendation(usuarioId);
        
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void findAllProductChangeRecommendations_ShouldReturnList() {
        UUID usuarioId = UUID.randomUUID();
        ProductRecommendationResult result = new ProductRecommendationResult(
                new ProductChangeRecommendation(), Optional.empty());
        when(generateProductChangeRecommendation.findAllResultsByUsuarioId(usuarioId))
                .thenReturn(List.of(result));

        ResponseEntity<List<ProductChangeRecommendationResponse>> response = controller
                .findAllProductChangeRecommendations(usuarioId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void generateProductChangeRecommendationPost_ShouldReturnList() {
        UUID usuarioId = UUID.randomUUID();
        ProductRecommendationResult result = new ProductRecommendationResult(
                new ProductChangeRecommendation(), Optional.empty());
        when(generateProductChangeRecommendation.executeByUsuarioId(usuarioId))
                .thenReturn(List.of(result));

        List<ProductChangeRecommendationResponse> response = controller
                .generateProductChangeRecommendationPost(usuarioId);
        
        assertEquals(1, response.size());
    }
}
