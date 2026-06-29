package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.HealthyProductOption;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import com.recred.backend.domain.models.HealthyRepeatedProductPattern;
import com.recred.backend.domain.models.RecentProductConsumption;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthyRecommendationModelTest {

    @Test
    void recentConsumptionShouldExposePurchaseDateAndQuantityOrZero() {
        RecentProductConsumption consumption = consumption(null);

        assertEquals(LocalDate.parse("2026-05-25"), consumption.purchaseDate());
        assertEquals(0, consumption.quantityOrZero());
    }

    @Test
    void optionShouldKnowWhenItWasSuggestedByMessages() {
        UUID productId = UUID.randomUUID();
        HealthyProductOption option = option(productId);

        assertTrue(option.isSuggestedBy(messages(productId)));
        assertFalse(option.isSuggestedBy(messages(UUID.randomUUID())));
        assertFalse(option.isSuggestedBy(null));
    }

    @Test
    void repeatedProductPatternShouldKnowIfItMeetsMinimumRepeatedDays() {
        HealthyRepeatedProductPattern pattern = pattern(2);

        assertTrue(pattern.hasAtLeastRepeatedDays(2));
        assertFalse(pattern.hasAtLeastRepeatedDays(3));
    }

    @Test
    void enumsShouldExposeHealthyRecommendationStates() {
        assertEquals(
                Set.of(EstadoCompra.ENTREGADO),
                EstadoCompra.healthyRecommendationStatuses()
        );
        assertEquals(
                Set.of(EstadoInventario.DISPONIBLE, EstadoInventario.BAJO_STOCK),
                EstadoInventario.availableForHealthyRecommendations()
        );
    }

    private RecentProductConsumption consumption(Integer quantity) {
        return new RecentProductConsumption(
                UUID.randomUUID(),
                "Coca Cola",
                "Gaseosa",
                "Bebidas",
                UUID.randomUUID(),
                LocalDateTime.parse("2026-05-25T10:00:00"),
                quantity
        );
    }

    private HealthyProductOption option(UUID productId) {
        return new HealthyProductOption(
                productId,
                "Agua mineral",
                "Agua sin azucar",
                "Bebidas"
        );
    }

    private HealthyRecommendationMessages messages(UUID suggestedProductId) {
        return new HealthyRecommendationMessages(
                "Proba agua",
                "Elegiste gaseosa estos dias. Hoy podes probar agua mineral.",
                "Elegir agua mineral.",
                suggestedProductId,
                new BigDecimal("0.8500"),
                "gemini-test"
        );
    }

    private HealthyRepeatedProductPattern pattern(int repeatedDays) {
        return new HealthyRepeatedProductPattern(
                UUID.randomUUID(),
                "Coca Cola",
                "Bebidas",
                UUID.randomUUID(),
                repeatedDays,
                2L,
                LocalDateTime.parse("2026-05-25T10:00:00")
        );
    }
}
