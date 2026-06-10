package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.RecessTime;
import java.util.List;
import java.util.UUID;

public record AdvancePurchaseRequest(List<OrderRequest> orders) {
    public record OrderRequest(
            UUID studentId,
            UUID buyerId,
            BuyerType buyerType,
            String date,
            RecessTime recessTime,
            List<ItemRequest> items,
            UUID suggestionId,
            UUID buffetId
    ) {}

    public record ItemRequest(
            UUID productId,
            int quantity
    ) {}
}
