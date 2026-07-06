package com.recred.backend.application;

import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;

import java.math.BigDecimal;

public record CreateUserSubscriptionPaymentResponse(
        String paymentUrl,
        TipoPlan plan,
        PeriodoSuscripcion periodo,
        BigDecimal price,
        String currency
) {
}
