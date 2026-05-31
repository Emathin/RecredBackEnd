package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PeriodBudget;
import com.recred.backend.domain.models.RangoFechas;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class BudgetPeriodCalculator {

    public RangoFechas calculateRange(PeriodBudget period,
                                      LocalDateTime referenceDate) {
        LocalDateTime start;
        LocalDateTime end;

        switch (period) {
            case DIARIO:
                start = referenceDate.with(LocalTime.MIN);
                end = referenceDate.with(LocalTime.MAX);
                break;

            case SEMANAL:
                start = referenceDate.with(TemporalAdjusters.previousOrSame
                        (DayOfWeek.MONDAY)).with(LocalTime.MIN);
                end = referenceDate.with(TemporalAdjusters.nextOrSame
                        (DayOfWeek.SUNDAY)).with(LocalTime.MAX);
                break;

            case QUINCENAL:
                if (referenceDate.getDayOfMonth() <= 15) {
                    start = referenceDate.withDayOfMonth(1).
                            with(LocalTime.MIN);
                    end = referenceDate.withDayOfMonth(15).
                            with(LocalTime.MAX);
                } else {
                    start = referenceDate.withDayOfMonth(16).
                            with(LocalTime.MIN);
                    end = referenceDate.with(TemporalAdjusters.
                            lastDayOfMonth()).with(LocalTime.MAX);
                }
                break;

            case MENSUAL:
                start = referenceDate.with(TemporalAdjusters.
                        firstDayOfMonth()).with(LocalTime.MIN);
                end = referenceDate.with(TemporalAdjusters.
                        lastDayOfMonth()).with(LocalTime.MAX);
                break;

            default:
                throw new IllegalArgumentException
                        ("Período de presupuesto no soportado: " + period);
        }

        return new RangoFechas(start, end);
    }
}