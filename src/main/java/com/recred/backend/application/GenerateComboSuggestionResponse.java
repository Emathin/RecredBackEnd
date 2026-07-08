package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GenerateComboSuggestionResponse(
        UUID idProduct,
        String productName,
        List<SuggestedProductResponse> suggestedProducts
) {
    public record SuggestedProductResponse(
            UUID id,
            String nombre,
            BigDecimal precio
    ) {
    }
}
