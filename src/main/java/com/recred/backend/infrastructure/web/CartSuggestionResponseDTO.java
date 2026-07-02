package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetCartSuggestionsResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record CartSuggestionResponseDTO(
        UUID productId,
        String productName,
        BigDecimal price,
        Integer stockDisponible,
        String reason,
        String source,
        int score
) {
    public static CartSuggestionResponseDTO fromApplication(
            GetCartSuggestionsResponse.Suggestion suggestion) {
        return new CartSuggestionResponseDTO(
                suggestion.productId(),
                suggestion.productName(),
                suggestion.price(),
                suggestion.stockDisponible(),
                suggestion.reason(),
                suggestion.source(),
                suggestion.score()
        );
    }
}
