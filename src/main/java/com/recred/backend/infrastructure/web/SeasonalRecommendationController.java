package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateSeasonalRecommendation;
import com.recred.backend.application.GenerateSeasonalRecommendationRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final PlanAccessGuard planAccessGuard;

    public SeasonalRecommendationController(
            GenerateSeasonalRecommendation useCase,
            PlanAccessGuard planAccessGuard
    ) {
        this.useCase = useCase;
        this.planAccessGuard = planAccessGuard;
    }

    @GetMapping("/seasonal")
    public ResponseEntity<RecommendationResponseDTO> getSeasonalRecommendations(
            Authentication authentication,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) java.util.UUID buffetId) {
        planAccessGuard.requireVendorAdvanced(authentication);

        RecommendationResponseDTO response = RecommendationResponseDTO.fromApplication(
                useCase.execute(new GenerateSeasonalRecommendationRequest(
                        latitude,
                        longitude,
                        buffetId
                ))
        );

        return ResponseEntity.ok(response);
    }
}
