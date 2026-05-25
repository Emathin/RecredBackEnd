package com.recred.backend.domain.services;

import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.DateRange;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class SpendingPredictionRangeService {

    public DateRange calculate(BudgetPeriod period, LocalDate baseDate) {
        return switch (period) {
            case DIARIO -> new DateRange(baseDate, baseDate);
            case SEMANAL -> calculateSemana(baseDate);
            case QUINCENAL -> calculateQuincena(baseDate);
            case MENSUAL -> calculateMes(baseDate);
        };
    }

    private DateRange calculateSemana(LocalDate baseDate) {
        LocalDate inicio = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fin = baseDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new DateRange(inicio, fin);
    }

    private DateRange calculateQuincena(LocalDate baseDate) {
        LocalDate inicioMes = baseDate.withDayOfMonth(1);
        if (baseDate.getDayOfMonth() <= 15) {
            return new DateRange(inicioMes, inicioMes.withDayOfMonth(15));
        }
        return new DateRange(baseDate.withDayOfMonth(16), baseDate.withDayOfMonth(
                baseDate.lengthOfMonth()));
    }

    private DateRange calculateMes(LocalDate baseDate) {
        LocalDate inicio = baseDate.withDayOfMonth(1);
        return new DateRange(inicio, baseDate.withDayOfMonth(baseDate.lengthOfMonth()));
    }
}
