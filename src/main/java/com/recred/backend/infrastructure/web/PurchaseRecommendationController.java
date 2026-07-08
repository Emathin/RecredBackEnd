package com.recred.backend.infrastructure.web;

import com.recred.backend.application.FindPurchaseSuggestionForToday;
import com.recred.backend.application.FindPurchaseSuggestionForTomorrow;
import com.recred.backend.application.FindPurchaseSuggestionRequest;
import com.recred.backend.application.FindPurchaseSuggestionResponse;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PurchaseRecommendationController {

    private final FindPurchaseSuggestionForToday findPurchaseSuggestionForToday;
    private final FindPurchaseSuggestionForTomorrow findPurchaseSuggestionForTomorrow;

    public PurchaseRecommendationController(
            FindPurchaseSuggestionForToday findPurchaseSuggestionForToday,
            FindPurchaseSuggestionForTomorrow findPurchaseSuggestionForTomorrow) {
        this.findPurchaseSuggestionForToday = findPurchaseSuggestionForToday;
        this.findPurchaseSuggestionForTomorrow = findPurchaseSuggestionForTomorrow;
    }

    @GetMapping("/api/v1/ia/alumnos/me/obtenerSugerenciaDelDia")
    public FindPurchaseSuggestionResponse getTodayPurchaseRecommendation(
            Authentication authentication) {
        var request = new FindPurchaseSuggestionRequest(cognitoSubject(authentication));
        return findPurchaseSuggestionForToday.execute(request);
    }

    @GetMapping("/api/v1/ia/alumnos/me/obtenerSugerenciaManiana")
    public FindPurchaseSuggestionResponse getTomorrowPurchaseRecommendation(
            Authentication authentication) {
        var request = new FindPurchaseSuggestionRequest(cognitoSubject(authentication));
        return findPurchaseSuggestionForTomorrow.execute(request);
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
