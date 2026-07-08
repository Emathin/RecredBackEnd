package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.SuscripcionColegio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActivateSchoolSubscriptionResponse(
        UUID subscriptionId,
        UUID colegioId,
        BigDecimal precio,
        String moneda,
        LocalDateTime fechaInicio,
        LocalDateTime fechaVencimiento,
        LocalDateTime fechaGraciaHasta,
        EstadoSuscripcion estado
) {
    public static ActivateSchoolSubscriptionResponse fromDomain(
            SuscripcionColegio subscription
    ) {
        return new ActivateSchoolSubscriptionResponse(
                subscription.getId(),
                subscription.getColegioId(),
                subscription.getPrecio(),
                subscription.getMoneda(),
                subscription.getFechaInicio(),
                subscription.getFechaVencimiento(),
                subscription.getFechaGraciaHasta(),
                subscription.getEstado()
        );
    }
}
