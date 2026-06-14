package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kiosqueros/")
public class ProductChangeController {

    private final GenerateProductChangeRecommendation generateProductChangeRecommendation;

    public ProductChangeController(
            GenerateProductChangeRecommendation generateProductChangeRecommendation) {
        this.generateProductChangeRecommendation = generateProductChangeRecommendation;
    }

    @GetMapping("/{idUsuario}/sugerencia-cambio-producto")
    public ResponseEntity<ProductChangeRecommendationResponse> generateProductChangeRecommendation(
            @PathVariable("idUsuario") UUID usuarioId
    ) {

        return generateProductChangeRecommendation.findLatestResultByUsuarioId(usuarioId)
                .map(ProductChangeRecommendationResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());

    }

    @GetMapping("/{idUsuario}/lista-sugerencia-cambio-producto")
    public ResponseEntity<List<ProductChangeRecommendationResponse>>
    findAllProductChangeRecommendations(@PathVariable("idUsuario") UUID usuarioId) {
        Set<UUID> seenProducts = new HashSet<>();
        List<ProductChangeRecommendationResponse> responses = generateProductChangeRecommendation
                .findAllResultsByUsuarioId(usuarioId)
                .stream()
                .filter(result -> seenProducts.add(
                        result.getProductChangeRecommendation().getId()))
                .limit(5)
                .map(ProductChangeRecommendationResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{idUsuario}/sugerencia-cambio-producto")
    public List<ProductChangeRecommendationResponse> generateProductChangeRecommendationPost(
            @PathVariable("idUsuario") UUID usuarioId
    ) {

        return generateProductChangeRecommendation.executeByUsuarioId(usuarioId)
                .stream()
                .map(ProductChangeRecommendationResponse::fromDomain)
                .toList();

    }
}
