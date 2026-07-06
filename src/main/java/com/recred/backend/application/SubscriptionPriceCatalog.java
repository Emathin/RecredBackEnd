package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidSubscriptionPlanException;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;

import java.math.BigDecimal;

public class SubscriptionPriceCatalog {

    public BigDecimal priceFor(TipoPlan plan, PeriodoSuscripcion period) {
        if (plan == TipoPlan.INTERMEDIO && period == PeriodoSuscripcion.MENSUAL) {
            return new BigDecimal("5500.00");
        }
        if (plan == TipoPlan.INTERMEDIO && period == PeriodoSuscripcion.ANUAL) {
            return new BigDecimal("52800.00");
        }
        if (plan == TipoPlan.AVANZADO && period == PeriodoSuscripcion.MENSUAL) {
            return new BigDecimal("10200.00");
        }
        if (plan == TipoPlan.AVANZADO && period == PeriodoSuscripcion.ANUAL) {
            return new BigDecimal("97920.00");
        }

        throw new InvalidSubscriptionPlanException(plan);
    }
}
