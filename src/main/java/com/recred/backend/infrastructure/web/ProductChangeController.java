package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetProductChangeRecommendation;
import com.recred.backend.application.GetProductChangeRecommendationRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kiosqueros")
public class ProductChangeController {

    private final GetProductChangeRecommendation getProductChangeRecommendation;
    private final PlanAccessGuard planAccessGuard;

    public ProductChangeController(
            GetProductChangeRecommendation getProductChangeRecommendation,
            PlanAccessGuard planAccessGuard
    ) {
        this.getProductChangeRecommendation = getProductChangeRecommendation;
        this.planAccessGuard = planAccessGuard;
    }

    @GetMapping("/me/lista-sugerencia-cambio-producto")
    public ResponseEntity<List<ProductChangeRecommendationResponse>>
    findAllProductChangeRecommendations(Authentication authentication) {
        planAccessGuard.requireVendorAdvanced(authentication);
        Set<java.util.UUID> seenProducts = new HashSet<>();
        var request = new GetProductChangeRecommendationRequest(cognitoSubject(authentication));
        List<ProductChangeRecommendationResponse> responses = getProductChangeRecommendation
                .execute(request)
                .stream()
                .filter(dto -> seenProducts.add(dto.recommendationId()))
                .limit(5)
                .map(ProductChangeRecommendationResponse::fromUseCaseResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
