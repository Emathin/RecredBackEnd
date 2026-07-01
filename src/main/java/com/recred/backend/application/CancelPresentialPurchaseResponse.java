package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import java.math.BigDecimal;
import java.util.UUID;

public record CancelPresentialPurchaseResponse(
        UUID purchaseId,
        UUID studentId,
        UUID buffetId,
        EstadoCompra status,
        String withdrawalCode,
        BigDecimal totalAmount
) {
    static CancelPresentialPurchaseResponse fromDomain(Purchase purchase) {
        return new CancelPresentialPurchaseResponse(
                purchase.getId(),
                purchase.studentIdOrNull(),
                purchase.getBuffetId(),
                purchase.getStatus(),
                purchase.getWithdrawalCode(),
                purchase.getTotalAmount()
        );
    }
}
