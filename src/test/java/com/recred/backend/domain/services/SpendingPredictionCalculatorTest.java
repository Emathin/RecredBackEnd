package com.recred.backend.domain.services;

import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.PurchaseSpending;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPeriodPreference;
import com.recred.backend.domain.models.DateRange;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpendingPredictionCalculatorTest {

    private final SpendingPredictionCalculator calculator = new SpendingPredictionCalculator();

    @Test
    void calculatePredictionWithHistoryBudgetAndCategories() {
        UUID studentId = UUID.randomUUID();
        LocalDate calculationDate = LocalDate.of(2026, 5, 8);
        DateRange currentRange = new DateRange(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 15)
        );
        DateRange historicalRange = new DateRange(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10)
        );

        SpendingPrediction prediction = calculator.calculate(
                studentId,
                BudgetPeriod.QUINCENAL,
                calculationDate,
                currentRange,
                historicalRange,
                List.of(spending("2026-05-02T10:00:00", "30.00")),
                List.of(
                        spending("2026-04-01T10:00:00", "40.00"),
                        spending("2026-04-03T10:00:00", "40.00")
                ),
                Optional.of(new SpendingPeriodPreference(
                        BudgetPeriod.QUINCENAL,
                        new BigDecimal("100.00")
                )),
                List.of(new ConsumptionCategory("Bebidas", new BigDecimal("30.00")))
        );

        assertEquals(studentId, prediction.studentId());
        assertEquals(BudgetPeriod.QUINCENAL, prediction.period());
        assertEquals(new BigDecimal("30.00"), prediction.currentSpending());
        assertEquals(new BigDecimal("80.00"), prediction.predictedSpending());
        assertEquals(new BigDecimal("10.00"), prediction.averageDailySpending());
        assertEquals(new BigDecimal("100.00"), prediction.limitAmount());
        assertEquals(new BigDecimal("80.00"), prediction.budgetPercentage());
        assertEquals(new BigDecimal("0.3700"), prediction.confidence());
        assertEquals(8, prediction.historicalDaysUsed());
        assertEquals(5, prediction.remainingDays());
        assertEquals("Bebidas", prediction.mostConsumedCategories().get(0).description());
    }

    @Test
    void useCurrentAverageWhenThereIsNoHistory() {
        SpendingPrediction prediction = calculator.calculate(
                UUID.randomUUID(),
                BudgetPeriod.QUINCENAL,
                LocalDate.of(2026, 5, 8),
                new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15)),
                new DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10)),
                List.of(spending("2026-05-08T10:00:00", "90.00")),
                List.of(),
                Optional.empty(),
                List.of()
        );

        assertEquals(new BigDecimal("15.00"), prediction.averageDailySpending());
        assertEquals(new BigDecimal("165.00"), prediction.predictedSpending());
        assertNull(prediction.limitAmount());
        assertNull(prediction.budgetPercentage());
        assertEquals(new BigDecimal("0.3500"), prediction.confidence());
    }

    @Test
    void returnZeroAndLowConfidenceWhenThereIsNoData() {
        SpendingPrediction prediction = calculator.calculate(
                UUID.randomUUID(),
                BudgetPeriod.MENSUAL,
                LocalDate.of(2026, 5, 8),
                new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10)),
                List.of(),
                List.of(),
                Optional.empty(),
                List.of()
        );

        assertEquals(new BigDecimal("0.00"), prediction.currentSpending());
        assertEquals(new BigDecimal("0.00"), prediction.predictedSpending());
        assertEquals(new BigDecimal("0.00"), prediction.averageDailySpending());
        assertEquals(new BigDecimal("0.1000"), prediction.confidence());
    }

    @Test
    void returnZeroAverageWhenThereAreNoValidDaysToAverage() {
        SpendingPrediction prediction = calculator.calculate(
                UUID.randomUUID(),
                BudgetPeriod.DIARIO,
                LocalDate.of(2026, 4, 30),
                new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2)),
                new DateRange(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 9)),
                List.of(spending("2026-05-01T10:00:00", "50.00")),
                List.of(spending("2026-04-08T10:00:00", "80.00")),
                Optional.empty(),
                List.of()
        );

        assertEquals(new BigDecimal("0.00"), prediction.averageDailySpending());
        assertEquals(new BigDecimal("50.00"), prediction.predictedSpending());
    }

    @Test
    void notCalculatePercentageWhenLimitIsZero() {
        SpendingPrediction prediction = calculator.calculate(
                UUID.randomUUID(),
                BudgetPeriod.MENSUAL,
                LocalDate.of(2026, 5, 8),
                new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10)),
                List.of(spending("2026-05-08T10:00:00", "90.00")),
                List.of(),
                Optional.of(new SpendingPeriodPreference(
                        BudgetPeriod.MENSUAL,
                        BigDecimal.ZERO
                )),
                List.of()
        );

        assertEquals(new BigDecimal("0.00"), prediction.limitAmount());
        assertNull(prediction.budgetPercentage());
    }

    @Test
    void capMaximumConfidence() {
        List<PurchaseSpending> historicalItems = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            historicalItems.add(spending("2026-04-01T10:00:00", "1.00"));
        }

        SpendingPrediction prediction = calculator.calculate(
                UUID.randomUUID(),
                BudgetPeriod.MENSUAL,
                LocalDate.of(2026, 5, 8),
                new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)),
                List.of(),
                historicalItems,
                Optional.empty(),
                List.of()
        );

        assertEquals(new BigDecimal("0.9500"), prediction.confidence());
    }

    @Test
    void notCountOperatingDaysWhenRangeIsInverted() {
        int days = calculator.countOperatingDays(
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 8)
        );

        assertEquals(0, days);
    }

    private PurchaseSpending spending(String dateTime, String amount) {
        return new PurchaseSpending(LocalDateTime.parse(dateTime), new BigDecimal(amount));
    }
}
