package com.recred.backend.domain.models;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeTest {

    @Test
    void fromBudgetPeriodShouldCalculateDailyRange() {
        LocalDate date = LocalDate.of(2026, 5, 21);

        DateRange range = DateRange.fromBudgetPeriod(BudgetPeriod.DIARIO, date);

        assertEquals(date, range.startDate());
        assertEquals(date, range.endDate());
        assertTrue(range.contains(date));
    }

    @Test
    void fromBudgetPeriodShouldCalculateWeeklyRangeFromMondayToSunday() {
        DateRange range = DateRange.fromBudgetPeriod(
                BudgetPeriod.SEMANAL,
                LocalDate.of(2026, 5, 21)
        );

        assertEquals(LocalDate.of(2026, 5, 18), range.startDate());
        assertEquals(LocalDate.of(2026, 5, 24), range.endDate());
    }

    @Test
    void fromBudgetPeriodShouldCalculateFirstHalfMonth() {
        DateRange range = DateRange.fromBudgetPeriod(
                BudgetPeriod.QUINCENAL,
                LocalDate.of(2026, 5, 15)
        );

        assertEquals(LocalDate.of(2026, 5, 1), range.startDate());
        assertEquals(LocalDate.of(2026, 5, 15), range.endDate());
    }

    @Test
    void fromBudgetPeriodShouldCalculateSecondHalfMonth() {
        DateRange range = DateRange.fromBudgetPeriod(
                BudgetPeriod.QUINCENAL,
                LocalDate.of(2026, 2, 16)
        );

        assertEquals(LocalDate.of(2026, 2, 16), range.startDate());
        assertEquals(LocalDate.of(2026, 2, 28), range.endDate());
    }

    @Test
    void fromBudgetPeriodShouldCalculateMonthlyRange() {
        DateRange range = DateRange.fromBudgetPeriod(
                BudgetPeriod.MENSUAL,
                LocalDate.of(2026, 2, 10)
        );

        assertEquals(LocalDate.of(2026, 2, 1), range.startDate());
        assertEquals(LocalDate.of(2026, 2, 28), range.endDate());
    }

    @Test
    void containsShouldDetectDatesOutsideRange() {
        DateRange range = new DateRange(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertFalse(range.contains(LocalDate.of(2026, 4, 30)));
        assertFalse(range.contains(LocalDate.of(2026, 6, 1)));
    }
}
