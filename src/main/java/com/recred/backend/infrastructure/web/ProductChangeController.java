package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ia/usuarios/{idUsuario}/sugerencia-cambio-producto")
public class ProductChangeController {

    private final GenerateProductChangeRecommendation generateProductChangeRecommendation;

    public ProductChangeController(
            GenerateProductChangeRecommendation generateProductChangeRecommendation) {
        this.generateProductChangeRecommendation = generateProductChangeRecommendation;
    }

    @GetMapping
    public ResponseEntity<ProductChangeRecommendationResponse> generateProductChangeRecommendation(
            @PathVariable("idUsuario") UUID usuarioId
    ) {

        return generateProductChangeRecommendation.findLatestResultByUsuarioId(usuarioId)
                .map(ProductChangeRecommendationResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());

    }

    @PostMapping
    public ProductChangeRecommendationResponse generateProductChangeRecommendationPost(
            @PathVariable("idUsuario") UUID usuarioId
    ) {

        return ProductChangeRecommendationResponse.fromDomain(
                generateProductChangeRecommendation.executeByUsuarioId(usuarioId)
        );

    }
}
