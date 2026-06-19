package com.recred.backend.domain.models;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public record DateRange(LocalDate startDate, LocalDate endDate) {

    public static DateRange fromBudgetPeriod(BudgetPeriod period, LocalDate baseDate) {
        return switch (period) {
            case DIARIO -> new DateRange(baseDate, baseDate);
            case SEMANAL -> week(baseDate);
            case QUINCENAL -> fortnight(baseDate);
            case MENSUAL -> month(baseDate);
        };
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private static DateRange week(LocalDate baseDate) {
        LocalDate startDate = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = baseDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new DateRange(startDate, endDate);
    }

    private static DateRange fortnight(LocalDate baseDate) {
        LocalDate monthStart = baseDate.withDayOfMonth(1);
        if (baseDate.getDayOfMonth() <= 15) {
            return new DateRange(monthStart, monthStart.withDayOfMonth(15));
        }
        return new DateRange(baseDate.withDayOfMonth(16), baseDate.withDayOfMonth(
                baseDate.lengthOfMonth()));
    }

    private static DateRange month(LocalDate baseDate) {
        LocalDate startDate = baseDate.withDayOfMonth(1);
        return new DateRange(startDate, baseDate.withDayOfMonth(baseDate.lengthOfMonth()));
    }
}
