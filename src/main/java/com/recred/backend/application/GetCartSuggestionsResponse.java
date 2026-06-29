package com.recred.backend.application;

import com.recred.backend.domain.models.CartSuggestion;
import com.recred.backend.domain.models.Producto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GetCartSuggestionsResponse(
        List<Suggestion> suggestions
) {

    public static GetCartSuggestionsResponse fromDomain(List<CartSuggestion> suggestions) {
        return new GetCartSuggestionsResponse(
                suggestions.stream()
                        .map(Suggestion::fromDomain)
                        .toList()
        );
    }

    public record Suggestion(
            UUID productId,
            String productName,
            BigDecimal price,
            Integer stockDisponible,
            String reason,
            String source,
            int score
    ) {

        private static Suggestion fromDomain(CartSuggestion suggestion) {
            Producto product = suggestion.product();
            return new Suggestion(
                    product.getId(),
                    product.getNombre(),
                    product.getPrecio(),
                    product.getStockDisponible(),
                    suggestion.reason(),
                    suggestion.source(),
                    suggestion.score()
            );
        }
    }
}
