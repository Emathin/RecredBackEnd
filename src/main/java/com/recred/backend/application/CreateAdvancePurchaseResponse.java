package com.recred.backend.application;

import com.recred.backend.domain.models.PurchaseStatus;

import java.util.Map;
import java.util.UUID;

public record CreateAdvancePurchaseResponse(
        String orderId,
        PurchaseStatus status,
        double total,
        Map<UUID, String> codes
) {}
