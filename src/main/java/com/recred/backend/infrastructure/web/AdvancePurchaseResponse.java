// C:/Users/emman/OneDrive/Desktop/TPI/recred-backend/src/main/java/com/recred/backend/infrastructure/web/AdvancePurchaseResponse.java
package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.PurchaseStatus;
import java.util.Map;

public record AdvancePurchaseResponse(
        String orderId,
        PurchaseStatus status,
        double total,
        Map<String, String> codes
) {}
