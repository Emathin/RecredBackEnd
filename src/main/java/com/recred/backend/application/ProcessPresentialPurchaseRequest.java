package com.recred.backend.application;

import java.util.List;
import java.util.UUID;

public record ProcessPresentialPurchaseRequest(
        UUID studentId,
        List<ItemRequest> cart,
        UUID buffetId
) {
    public record ItemRequest(UUID productId, int quantity) {}
}
