package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateComboSuggestion;
import com.recred.backend.application.GenerateComboSuggestionRequest;
import com.recred.backend.application.GenerateComboSuggestionResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/combo-suggestions")
public class ComboSuggestionController {

    private final GenerateComboSuggestion generateComboSuggestion;
    private final PlanAccessGuard planAccessGuard;

    public ComboSuggestionController(
            GenerateComboSuggestion generateComboSuggestion,
            PlanAccessGuard planAccessGuard
    ) {
        this.generateComboSuggestion = generateComboSuggestion;
        this.planAccessGuard = planAccessGuard;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<GenerateComboSuggestionResponse> getComboSuggestions(
            Authentication authentication,
            @PathVariable UUID productId) {
        planAccessGuard.requireVendorAdvanced(authentication);
        GenerateComboSuggestionRequest request = new GenerateComboSuggestionRequest(
                productId, cognitoSubject(authentication));
        return ResponseEntity.ok(generateComboSuggestion.execute(request));
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
