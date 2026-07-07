package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public enum PeriodoSuscripcion {
    MENSUAL(1, ChronoUnit.MONTHS, 7),
    ANUAL(1, ChronoUnit.YEARS, 30);

    private final long amount;
    private final ChronoUnit unit;
    private final int expirationWarningDays;

    PeriodoSuscripcion(long amount, ChronoUnit unit, int expirationWarningDays) {
        this.amount = amount;
        this.unit = unit;
        this.expirationWarningDays = expirationWarningDays;
    }

    public LocalDateTime expirationFrom(LocalDateTime startDate) {
        return startDate.plus(amount, unit);
    }

    public int expirationWarningDays() {
        return expirationWarningDays;
    }
}
