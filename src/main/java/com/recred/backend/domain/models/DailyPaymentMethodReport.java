package com.recred.backend.domain.models;

import java.math.BigDecimal;

public record DailyPaymentMethodReport(
        MedioPago paymentMethod,
        int orders,
        BigDecimal total
) {
}
