package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.TipoPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActivateUserSubscriptionResponse(
        UUID subscriptionId,
        UUID usuarioId,
        TipoPlan plan,
        PeriodoSuscripcion periodo,
        BigDecimal precio,
        LocalDateTime fechaInicio,
        LocalDateTime fechaVencimiento,
        EstadoSuscripcion estado
) {
    public static ActivateUserSubscriptionResponse fromDomain(
            SuscripcionUsuario subscription
    ) {
        return new ActivateUserSubscriptionResponse(
                subscription.getId(),
                subscription.getUsuarioId(),
                subscription.getPlan(),
                subscription.getPeriodo(),
                subscription.getPrecio(),
                subscription.getFechaInicio(),
                subscription.getFechaVencimiento(),
                subscription.getEstado()
        );
    }
}
