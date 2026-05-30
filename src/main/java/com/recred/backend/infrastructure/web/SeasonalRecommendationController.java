package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateSeasonalRecommendation;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/recomendations")
@Validated
public class SeasonalRecommendationController {

    private final GenerateSeasonalRecommendation useCase;

    public SeasonalRecommendationController(GenerateSeasonalRecommendation useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/seasonal")
    public ResponseEntity<RecommendationResponseDTO> getSeasonalRecommendations(
            @RequestParam double latitude,
            @RequestParam double longitude) {

        RecommendationResponseDTO response = useCase.execute(latitude, longitude);

        return ResponseEntity.ok(response);
    }
}
