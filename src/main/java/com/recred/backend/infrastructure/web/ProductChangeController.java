package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateProductChangeRecommendation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ia/kiosqueros/{kiosqueroId}/sugerencia-cambio-producto")
public class ProductChangeController {

    private final GenerateProductChangeRecommendation generateProductChangeRecommendation;

    public ProductChangeController(
            GenerateProductChangeRecommendation generateProductChangeRecommendation) {
        this.generateProductChangeRecommendation = generateProductChangeRecommendation;
    }

    @GetMapping
    public ProductChangeRecommendationResponse generateProductChangeRecommendation(
            @PathVariable("kiosqueroId") UUID kiosqueroId
    ) {

        return ProductChangeRecommendationResponse.fromDomain(
                generateProductChangeRecommendation.execute(kiosqueroId)
        );

    }
}
