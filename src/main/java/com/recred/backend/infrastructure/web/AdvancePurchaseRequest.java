// C:/Users/emman/OneDrive/Desktop/TPI/recred-backend/src/main/java/com/recred/backend/infrastructure/web/AdvancePurchaseRequest.java
package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.RecessTime;
import java.util.List;

public record AdvancePurchaseRequest(List<OrderRequest> orders) {
    public record OrderRequest(
            String studentId,
            String buyerId,
            BuyerType buyerType,
            String date,
            RecessTime recessTime,
            List<ItemRequest> items
    ) {}

    public record ItemRequest(
            String productId,
            int quantity
    ) {}
}
