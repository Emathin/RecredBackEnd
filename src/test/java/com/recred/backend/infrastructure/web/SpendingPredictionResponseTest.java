package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpendingPredictionResponseTest {

    @Test
    void mapDomainPredictionToHttpResponse() {
        UUID studentId = UUID.randomUUID();
        SpendingPrediction spendingPrediction = new SpendingPrediction(
                studentId,
                BudgetPeriod.MENSUAL,
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("50.00"),
                new BigDecimal("120.00"),
                new BigDecimal("10.00"),
                new BigDecimal("150.00"),
                new BigDecimal("80.00"),
                new BigDecimal("0.8000"),
                60,
                7,
                List.of(new ConsumptionCategory("Bebidas", new BigDecimal("40.00")))
        );

        SpendingPredictionResult result = new SpendingPredictionResult(
                spendingPrediction,
                Optional.of(new SpendingPredictionAnalysis(
                        "Gasto estable.",
                        List.of("Sin alerts."),
                        List.of("Mantener habitos."),
                        "gemini-test"
                ))
        );

        SpendingPredictionResponse response = SpendingPredictionResponse.fromDomain(result);

        assertEquals(studentId, response.studentId());
        assertEquals(BudgetPeriod.MENSUAL, response.period());
        assertEquals(new BigDecimal("120.00"), response.predictedSpending());
        assertEquals(1, response.mostConsumedCategories().size());
        assertEquals("Bebidas", response.mostConsumedCategories().get(0).description());
        assertEquals(
                new BigDecimal("40.00"),
                response.mostConsumedCategories().get(0).totalAmount()
        );
        assertEquals("Gasto estable.", response.aiAnalysis().summary());
        assertEquals("gemini-test", response.aiAnalysis().model());
    }
}
