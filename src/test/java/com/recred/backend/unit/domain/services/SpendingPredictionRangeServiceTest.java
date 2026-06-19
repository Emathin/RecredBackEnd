package com.recred.backend.unit.domain.services;

import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.DateRange;
import java.time.LocalDate;

import com.recred.backend.domain.services.SpendingPredictionRangeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpendingPredictionRangeServiceTest {

    private final SpendingPredictionRangeService service = new SpendingPredictionRangeService();

    @Test
    void calculateDailyRange() {
        LocalDate date = LocalDate.of(2026, 5, 21);

        DateRange range = service.calculate(BudgetPeriod.DIARIO, date);

        assertEquals(date, range.startDate());
        assertEquals(date, range.endDate());
        assertTrue(range.contains(date));
    }

    @Test
    void calculateWeeklyRangeFromMondayToSunday() {
        DateRange range = service.calculate(
                BudgetPeriod.SEMANAL,
                LocalDate.of(2026, 5, 21)
        );

        assertEquals(LocalDate.of(2026, 5, 18), range.startDate());
        assertEquals(LocalDate.of(2026, 5, 24), range.endDate());
    }

    @Test
    void calculateFirstHalfMonth() {
        DateRange range = service.calculate(
                BudgetPeriod.QUINCENAL,
                LocalDate.of(2026, 5, 15)
        );

        assertEquals(LocalDate.of(2026, 5, 1), range.startDate());
        assertEquals(LocalDate.of(2026, 5, 15), range.endDate());
    }

    @Test
    void calculateSecondHalfMonth() {
        DateRange range = service.calculate(
                BudgetPeriod.QUINCENAL,
                LocalDate.of(2026, 2, 16)
        );

        assertEquals(LocalDate.of(2026, 2, 16), range.startDate());
        assertEquals(LocalDate.of(2026, 2, 28), range.endDate());
    }

    @Test
    void calculateMonthlyRange() {
        DateRange range = service.calculate(
                BudgetPeriod.MENSUAL,
                LocalDate.of(2026, 2, 10)
        );

        assertEquals(LocalDate.of(2026, 2, 1), range.startDate());
        assertEquals(LocalDate.of(2026, 2, 28), range.endDate());
    }

    @Test
    void detectDatesOutsideRange() {
        DateRange range = new DateRange(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertFalse(range.contains(LocalDate.of(2026, 4, 30)));
        assertFalse(range.contains(LocalDate.of(2026, 6, 1)));
    }
}
