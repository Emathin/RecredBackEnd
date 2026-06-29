package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.ConsumptionAlertPattern;
import com.recred.backend.domain.models.ConsumptionAlertType;
import com.recred.backend.domain.models.ConsumptionCategoryStatistic;
import com.recred.backend.domain.models.ConsumptionProductStatistic;
import com.recred.backend.domain.models.EstadoCompra;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumptionAlertPatternTest {

    @Test
    void suggestedValues_ShouldReturnPresentOptionalsWhenValuesExist() {
        UUID productId = UUID.randomUUID();
        ConsumptionAlertPattern pattern = pattern(productId, "Manzana");

        assertEquals(productId, pattern.suggestedProductIdValue().orElseThrow());
        assertEquals("Manzana", pattern.suggestedProductNameValue().orElseThrow());
    }

    @Test
    void suggestedValues_ShouldReturnEmptyOptionalsWhenValuesAreMissingOrBlank() {
        ConsumptionAlertPattern pattern = pattern(null, " ");

        assertTrue(pattern.suggestedProductIdValue().isEmpty());
        assertTrue(pattern.suggestedProductNameValue().isEmpty());
    }

    @Test
    void categoryStatisticShouldExposeZeroWhenTotalAmountIsMissing() {
        ConsumptionCategoryStatistic statistic = new ConsumptionCategoryStatistic(
                "Bebidas",
                2L,
                2L,
                null
        );

        assertEquals(BigDecimal.ZERO, statistic.totalAmountOrZero());
    }

    @Test
    void productStatisticShouldKnowIfQuantityMeetsMinimum() {
        ConsumptionProductStatistic statistic = new ConsumptionProductStatistic(
                UUID.randomUUID(),
                "Agua",
                "Bebidas",
                2L,
                3L,
                BigDecimal.TEN
        );

        assertTrue(statistic.hasQuantityAtLeast(3L));
        assertFalse(statistic.hasQuantityAtLeast(4L));
    }

    @Test
    void productStatisticShouldRejectMinimumWhenQuantityIsMissing() {
        ConsumptionProductStatistic statistic = new ConsumptionProductStatistic(
                UUID.randomUUID(),
                "Agua",
                "Bebidas",
                2L,
                null,
                BigDecimal.TEN
        );

        assertFalse(statistic.hasQuantityAtLeast(1L));
    }

    @Test
    void alertTypeShouldResolveFromPurchaseCount() {
        assertEquals(
                ConsumptionAlertType.STATISTICAL_PATTERN,
                ConsumptionAlertType.fromPurchaseCount(3L, 3)
        );
        assertEquals(
                ConsumptionAlertType.HEALTHY_RECOMMENDATION,
                ConsumptionAlertType.fromPurchaseCount(2L, 3)
        );
        assertTrue(ConsumptionAlertType.STATISTICAL_PATTERN.isStatisticalPattern());
        assertTrue(ConsumptionAlertType.HEALTHY_RECOMMENDATION.isHealthyRecommendation());
    }

    @Test
    void estadoCompraShouldExposeStatisticalAlertStatuses() {
        assertEquals(
                Set.of(EstadoCompra.ENTREGADO),
                EstadoCompra.statisticalConsumptionAlertStatuses()
        );
    }

    private ConsumptionAlertPattern pattern(UUID suggestedProductId, String suggestedProductName) {
        return new ConsumptionAlertPattern(
                ConsumptionAlertType.STATISTICAL_PATTERN,
                UUID.randomUUID(),
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15),
                4,
                BigDecimal.TEN,
                "Frutas",
                BigDecimal.TEN,
                suggestedProductId,
                suggestedProductName
        );
    }
}
