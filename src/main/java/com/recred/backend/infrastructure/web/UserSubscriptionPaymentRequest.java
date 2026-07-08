package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;

import java.util.UUID;

public record UserSubscriptionPaymentRequest(
        UUID usuarioId,
        TipoPlan plan,
        PeriodoSuscripcion periodo
) {
}
