package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetProductChangeRecommendation;
import com.recred.backend.application.GetProductChangeRecommendationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kiosqueros/")
public class ProductChangeController {

    private final GetProductChangeRecommendation getProductChangeRecommendation;

    public ProductChangeController(
            GetProductChangeRecommendation getProductChangeRecommendation
    ) {
        this.getProductChangeRecommendation = getProductChangeRecommendation;
    }

    @GetMapping("/{idUsuario}/lista-sugerencia-cambio-producto")
    public ResponseEntity<List<ProductChangeRecommendationResponse>>
    findAllProductChangeRecommendations(@PathVariable("idUsuario") UUID usuarioId) {
        Set<UUID> seenProducts = new HashSet<>();
        GetProductChangeRecommendationRequest request =
                new GetProductChangeRecommendationRequest(usuarioId);
        List<ProductChangeRecommendationResponse> responses = getProductChangeRecommendation
                .execute(request)
                .stream()
                .filter(dto -> seenProducts.add(dto.recommendationId()))
                .limit(5)
                .map(ProductChangeRecommendationResponse::fromUseCaseResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }
}
