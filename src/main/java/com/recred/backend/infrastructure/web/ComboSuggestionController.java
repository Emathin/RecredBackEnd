package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateComboSuggestion;
import com.recred.backend.domain.models.ComboSuggestion;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/combo-suggestions")
public class ComboSuggestionController {

    private final GenerateComboSuggestion generateComboSuggestion;

    public ComboSuggestionController(GenerateComboSuggestion generateComboSuggestion) {
        this.generateComboSuggestion = generateComboSuggestion;
    }

    @GetMapping("/{productId}/{userId}")
    public ResponseEntity<ComboSuggestion> getComboSuggestions(
            @PathVariable UUID productId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(generateComboSuggestion.execute(productId, userId));
    }
}
