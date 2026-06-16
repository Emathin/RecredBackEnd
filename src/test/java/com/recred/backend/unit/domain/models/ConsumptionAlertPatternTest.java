package com.recred.backend.domain.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
