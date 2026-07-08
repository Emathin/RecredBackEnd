package com.recred.backend.unit.application;

import com.recred.backend.application.SubscriptionPriceCatalog;
import com.recred.backend.domain.exceptions.InvalidSubscriptionPlanException;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubscriptionPriceCatalogTest {

    private final SubscriptionPriceCatalog catalog = new SubscriptionPriceCatalog();

    @Test
    void priceFor_WhenPlanIsIntermediateMonthly_ShouldReturnMonthlyPrice() {
        BigDecimal result = whenGettingPrice(TipoPlan.INTERMEDIO, PeriodoSuscripcion.MENSUAL);

        assertEquals(new BigDecimal("5500.00"), result);
    }

    @Test
    void priceFor_WhenPlanIsIntermediateAnnual_ShouldReturnAnnualPrice() {
        BigDecimal result = whenGettingPrice(TipoPlan.INTERMEDIO, PeriodoSuscripcion.ANUAL);

        assertEquals(new BigDecimal("52800.00"), result);
    }

    @Test
    void priceFor_WhenPlanIsAdvancedMonthly_ShouldReturnMonthlyPrice() {
        BigDecimal result = whenGettingPrice(TipoPlan.AVANZADO, PeriodoSuscripcion.MENSUAL);

        assertEquals(new BigDecimal("10200.00"), result);
    }

    @Test
    void priceFor_WhenPlanIsAdvancedAnnual_ShouldReturnAnnualPrice() {
        BigDecimal result = whenGettingPrice(TipoPlan.AVANZADO, PeriodoSuscripcion.ANUAL);

        assertEquals(new BigDecimal("97920.00"), result);
    }

    @Test
    void priceFor_WhenPlanIsFree_ShouldThrowException() {
        assertThrows(
                InvalidSubscriptionPlanException.class,
                () -> whenGettingPrice(TipoPlan.GRATUITO, PeriodoSuscripcion.MENSUAL)
        );
    }

    private BigDecimal whenGettingPrice(TipoPlan plan, PeriodoSuscripcion period) {
        return catalog.priceFor(plan, period);
    }
}
