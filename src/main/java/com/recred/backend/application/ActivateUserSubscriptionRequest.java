package com.recred.backend.application;

import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActivateUserSubscriptionRequest(
        UUID usuarioId,
        TipoPlan plan,
        PeriodoSuscripcion periodo,
        BigDecimal precio,
        LocalDateTime fechaInicio
) {
}
