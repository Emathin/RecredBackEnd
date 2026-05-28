package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SchoolAssistantPayment(
        BigDecimal amount,
        EstadoPagoExterno status,
        LocalDateTime createdAt,
        LocalDateTime approvedAt
) {
}
