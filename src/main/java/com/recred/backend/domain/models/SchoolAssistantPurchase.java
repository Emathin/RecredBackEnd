package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SchoolAssistantPurchase(
        BigDecimal totalAmount,
        EstadoCompra status,
        MedioPago paymentMethod,
        LocalDateTime dateTime
) {
}
