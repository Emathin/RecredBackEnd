package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SuscripcionColegio {

    private static final int MONTHS_PER_PAYMENT = 1;
    private static final int GRACE_MONTHS = 1;

    private UUID id;
    private UUID colegioId;
    private BigDecimal precio;
    private String moneda;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaVencimiento;
    private LocalDateTime fechaGraciaHasta;
    private EstadoSuscripcion estado;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public static SuscripcionColegio activate(
            UUID colegioId,
            BigDecimal precio,
            String moneda,
            LocalDateTime fechaInicio
    ) {
        requireSchool(colegioId);
        requirePositivePrice(precio);
        requireCurrency(moneda);
        requireStartDate(fechaInicio);

        SuscripcionColegio subscription = new SuscripcionColegio();
        subscription.setColegioId(colegioId);
        subscription.setPrecio(precio);
        subscription.setMoneda(moneda);
        subscription.setFechaInicio(fechaInicio);
        subscription.setFechaVencimiento(fechaInicio.plusMonths(MONTHS_PER_PAYMENT));
        subscription.setFechaGraciaHasta(
                subscription.getFechaVencimiento().plusMonths(GRACE_MONTHS));
        subscription.setEstado(EstadoSuscripcion.ACTIVA);
        return subscription;
    }

    public void renew(
            BigDecimal precio,
            String moneda,
            LocalDateTime fechaInicio
    ) {
        requirePositivePrice(precio);
        requireCurrency(moneda);
        requireStartDate(fechaInicio);

        this.precio = precio;
        this.moneda = moneda;
        this.fechaInicio = fechaInicio;
        this.fechaVencimiento = fechaInicio.plusMonths(MONTHS_PER_PAYMENT);
        this.fechaGraciaHasta = this.fechaVencimiento.plusMonths(GRACE_MONTHS);
        this.estado = EstadoSuscripcion.ACTIVA;
    }

    public boolean isPaidActiveAt(LocalDateTime referenceDate) {
        return estado == EstadoSuscripcion.ACTIVA
                && startsAtOrBefore(referenceDate)
                && fechaVencimiento.isAfter(referenceDate);
    }

    public boolean isInGraceAt(LocalDateTime referenceDate) {
        return estado == EstadoSuscripcion.ACTIVA
                && !fechaVencimiento.isAfter(referenceDate)
                && fechaGraciaHasta.isAfter(referenceDate);
    }

    public boolean canUseAppAt(LocalDateTime referenceDate) {
        return isPaidActiveAt(referenceDate) || isInGraceAt(referenceDate);
    }

    public EstadoLicenciaColegio estadoLicenciaAt(LocalDateTime referenceDate) {
        if (estado == EstadoSuscripcion.CANCELADA) {
            return EstadoLicenciaColegio.CANCELADA;
        }

        if (isPaidActiveAt(referenceDate)) {
            return EstadoLicenciaColegio.ACTIVA;
        }

        if (isInGraceAt(referenceDate)) {
            return EstadoLicenciaColegio.EN_GRACIA;
        }

        return EstadoLicenciaColegio.VENCIDA;
    }

    public boolean isGraceExpiredAt(LocalDateTime referenceDate) {
        return estado == EstadoSuscripcion.VENCIDA
                || estado == EstadoSuscripcion.ACTIVA
                && !fechaGraciaHasta.isAfter(referenceDate);
    }

    public void expireAt(LocalDateTime referenceDate) {
        if (isGraceExpiredAt(referenceDate)) {
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

    private static void requireSchool(UUID colegioId) {
        if (colegioId == null) {
            throw new IllegalArgumentException("School is required");
        }
    }

    private static void requirePositivePrice(BigDecimal precio) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("School subscription price must be positive");
        }
    }

    private static void requireCurrency(String moneda) {
        if (moneda == null || moneda.isBlank()) {
            throw new IllegalArgumentException("School subscription currency is required");
        }
    }

    private static void requireStartDate(LocalDateTime fechaInicio) {
        if (fechaInicio == null) {
            throw new IllegalArgumentException("School subscription start date is required");
        }
    }
}
