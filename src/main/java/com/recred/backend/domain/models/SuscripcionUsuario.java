package com.recred.backend.domain.models;

import com.recred.backend.domain.exceptions.InvalidSubscriptionPlanException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SuscripcionUsuario {

    private UUID id;
    private UUID usuarioId;
    private TipoPlan plan;
    private PeriodoSuscripcion periodo;
    private BigDecimal precio;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaVencimiento;
    private EstadoSuscripcion estado;
    private LocalDateTime ultimoAvisoVencimientoEn;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public static SuscripcionUsuario activate(
            UUID usuarioId,
            TipoPlan plan,
            PeriodoSuscripcion periodo,
            BigDecimal precio,
            LocalDateTime fechaInicio
    ) {
        requirePaidPlan(plan);
        requirePeriod(periodo);
        requirePositivePrice(precio);
        requireStartDate(fechaInicio);

        SuscripcionUsuario subscription = new SuscripcionUsuario();
        subscription.setUsuarioId(usuarioId);
        subscription.setPlan(plan);
        subscription.setPeriodo(periodo);
        subscription.setPrecio(precio);
        subscription.setFechaInicio(fechaInicio);
        subscription.setFechaVencimiento(periodo.expirationFrom(fechaInicio));
        subscription.setEstado(EstadoSuscripcion.ACTIVA);
        return subscription;
    }

    public boolean isActiveAt(LocalDateTime referenceDate) {
        return estado == EstadoSuscripcion.ACTIVA
                && startsAtOrBefore(referenceDate)
                && expiresAfter(referenceDate);
    }

    public boolean isExpiredAt(LocalDateTime referenceDate) {
        return estado == EstadoSuscripcion.VENCIDA || activeSubscriptionExpiredAt(referenceDate);
    }

    public boolean isExpiringSoonAt(LocalDateTime referenceDate, int daysBeforeExpiration) {
        return isActiveAt(referenceDate)
                && !fechaVencimiento.isAfter(referenceDate.plusDays(daysBeforeExpiration));
    }

    public boolean shouldSendExpirationWarningAt(LocalDateTime referenceDate) {
        return ultimoAvisoVencimientoEn == null
                && isExpiringSoonAt(referenceDate, periodo.expirationWarningDays());
    }

    public void markExpirationWarningSentAt(LocalDateTime referenceDate) {
        if (shouldSendExpirationWarningAt(referenceDate)) {
            this.ultimoAvisoVencimientoEn = referenceDate;
        }
    }

    public void expireAt(LocalDateTime referenceDate) {
        if (isExpiredAt(referenceDate)) {
            this.estado = EstadoSuscripcion.VENCIDA;
        }
    }

    public void cancel() {
        if (this.estado == EstadoSuscripcion.ACTIVA) {
            this.estado = EstadoSuscripcion.CANCELADA;
        }
    }

    private boolean startsAtOrBefore(LocalDateTime referenceDate) {
        return fechaInicio != null && !fechaInicio.isAfter(referenceDate);
    }

    private boolean expiresAfter(LocalDateTime referenceDate) {
        return fechaVencimiento != null && fechaVencimiento.isAfter(referenceDate);
    }

    private boolean activeSubscriptionExpiredAt(LocalDateTime referenceDate) {
        return estado == EstadoSuscripcion.ACTIVA
                && fechaVencimiento != null
                && !fechaVencimiento.isAfter(referenceDate);
    }

    private static void requirePaidPlan(TipoPlan plan) {
        if (plan == null || !plan.isPaid()) {
            throw new InvalidSubscriptionPlanException(plan);
        }
    }

    private static void requirePeriod(PeriodoSuscripcion periodo) {
        if (periodo == null) {
            throw new IllegalArgumentException("Subscription period is required");
        }
    }

    private static void requirePositivePrice(BigDecimal precio) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Subscription price must be positive");
        }
    }

    private static void requireStartDate(LocalDateTime fechaInicio) {
        if (fechaInicio == null) {
            throw new IllegalArgumentException("Subscription start date is required");
        }
    }
}
