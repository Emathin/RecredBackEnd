// C:/Users/emman/OneDrive/Desktop/TPI/recred-backend/src/main/java/com/recred/backend/infrastructure/web/AdvancePurchaseResponse.java
package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.PurchaseStatus;
import java.util.Map;
import java.util.UUID;

public record AdvancePurchaseResponse(
        String orderId,
        PurchaseStatus status,
        double total,
        Map<UUID, String> codes
) {}
