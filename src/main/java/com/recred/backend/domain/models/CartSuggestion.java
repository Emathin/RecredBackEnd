package com.recred.backend.domain.models;

public record CartSuggestion(
        Producto product,
        String reason,
        String source,
        int score
) {
}
