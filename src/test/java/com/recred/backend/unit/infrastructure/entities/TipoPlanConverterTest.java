package com.recred.backend.unit.infrastructure.entities;

import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.infrastructure.entities.TipoPlanConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TipoPlanConverterTest {

    private final TipoPlanConverter converter = new TipoPlanConverter();

    @Test
    void convertToEntityAttribute_WhenStoredPlanIsLegacyPremium_ShouldReturnAdvanced() {
        String storedPlan = givenStoredPlan("PREMIUM");

        TipoPlan result = whenConvertingToEntityAttribute(storedPlan);

        assertEquals(TipoPlan.AVANZADO, result);
    }

    @Test
    void convertToEntityAttribute_WhenStoredPlanIsNull_ShouldReturnFree() {
        String storedPlan = givenStoredPlan(null);

        TipoPlan result = whenConvertingToEntityAttribute(storedPlan);

        assertEquals(TipoPlan.GRATUITO, result);
    }

    @Test
    void convertToDatabaseColumn_WhenPlanIsIntermediate_ShouldReturnIntermediateName() {
        TipoPlan plan = givenPlan(TipoPlan.INTERMEDIO);

        String result = whenConvertingToDatabaseColumn(plan);

        assertEquals("INTERMEDIO", result);
    }

    @Test
    void convertToDatabaseColumn_WhenPlanIsNull_ShouldReturnFreeName() {
        TipoPlan plan = givenPlan(null);

        String result = whenConvertingToDatabaseColumn(plan);

        assertEquals("GRATUITO", result);
    }

    private String givenStoredPlan(String plan) {
        return plan;
    }

    private TipoPlan givenPlan(TipoPlan plan) {
        return plan;
    }

    private TipoPlan whenConvertingToEntityAttribute(String storedPlan) {
        return converter.convertToEntityAttribute(storedPlan);
    }

    private String whenConvertingToDatabaseColumn(TipoPlan plan) {
        return converter.convertToDatabaseColumn(plan);
    }
}
