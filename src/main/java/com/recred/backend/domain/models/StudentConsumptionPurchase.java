package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StudentConsumptionPurchase(
        UUID purchaseId,
        LocalDateTime dateTime,
        PurchaseState status,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount,
        UUID buffetId,
        String buffetName,
        List<StudentConsumptionProduct> products
) {
}
