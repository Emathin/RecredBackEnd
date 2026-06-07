package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.CartSuggestion;
import com.recred.backend.domain.models.Producto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartSuggestionResponse(
        UUID productId,
        String productName,
        BigDecimal price,
        Integer stockActual,
        String reason,
        String source,
        int score
) {
    public static CartSuggestionResponse fromDomain(CartSuggestion suggestion) {
        Producto product = suggestion.product();
        return new CartSuggestionResponse(
                product.getId(),
                product.getNombre(),
                product.getPrecio(),
                product.getStockActual(),
                suggestion.reason(),
                suggestion.source(),
                suggestion.score()
        );
    }
}
