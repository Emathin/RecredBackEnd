package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StudentConsumptionPurchase(
        UUID purchaseId,
        LocalDateTime dateTime,
        EstadoCompra status,
        MedioPago paymentMethod,
        BigDecimal totalAmount,
        UUID buffetId,
        String buffetName,
        List<StudentConsumptionProduct> products
) {

    public String statusName() {
        return status == null ? null : status.name();
    }

    public String paymentMethodName() {
        return paymentMethod == null ? null : paymentMethod.name();
    }

    public List<StudentConsumptionProduct> productsOrEmpty() {
        return products == null ? List.of() : products;
    }
}
