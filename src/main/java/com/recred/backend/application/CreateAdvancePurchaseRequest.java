package com.recred.backend.application;

import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.RecessTime;

import java.util.List;
import java.util.UUID;

public record CreateAdvancePurchaseRequest(String cognitoSub, List<OrderRequest> orders) {

    public CreateAdvancePurchaseRequest(List<OrderRequest> orders) {
        this(null, orders);
    }

    public CreateAdvancePurchaseRequest withCognitoSub(String cognitoSub) {
        return new CreateAdvancePurchaseRequest(cognitoSub, orders);
    }

    public record OrderRequest(
            UUID studentId,
            UUID buyerId,
            BuyerType buyerType,
            UUID buffetId,
            String date,
            RecessTime recessTime,
            List<ItemRequest> items,
            UUID suggestionId,
            UUID tutorId
    ) {}

    public record ItemRequest(
            UUID productId,
            int quantity
    ) {}
}
