package com.recred.backend.infrastructure.web;

import java.util.List;
import java.util.UUID;

public record CartSuggestionRequest(
        UUID studentId,
        UUID buffetId,
        List<PurchaseItemDTO> items,
        Integer limit
) {
}
