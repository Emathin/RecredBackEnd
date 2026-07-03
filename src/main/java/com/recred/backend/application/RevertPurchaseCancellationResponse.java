package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;

import java.math.BigDecimal;
import java.util.UUID;

public record RevertPurchaseCancellationResponse(
        UUID purchaseId,
        UUID studentId,
        UUID buffetId,
        EstadoCompra status,
        String withdrawalCode,
        BigDecimal totalAmount
) {
    public static RevertPurchaseCancellationResponse fromDomain(Purchase purchase) {
        return new RevertPurchaseCancellationResponse(
                purchase.getId(),
                purchase.studentIdOrNull(),
                purchase.getBuffetId(),
                purchase.getStatus(),
                purchase.getWithdrawalCode(),
                purchase.getTotalAmount()
        );
    }
}
