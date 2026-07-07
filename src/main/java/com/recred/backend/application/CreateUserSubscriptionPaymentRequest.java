package com.recred.backend.application;

import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;

import java.util.UUID;

public record CreateUserSubscriptionPaymentRequest(
        UUID usuarioId,
        TipoPlan plan,
        PeriodoSuscripcion periodo
) {
}
