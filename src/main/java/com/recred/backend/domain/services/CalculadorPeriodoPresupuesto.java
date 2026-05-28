package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.RangoFechas;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class CalculadorPeriodoPresupuesto {

    public RangoFechas calcularRango(PeriodoPresupuesto periodo,
                                     LocalDateTime fechaReferencia) {
        LocalDateTime inicio;
        LocalDateTime fin;

        switch (periodo) {
            case DIARIO:
                inicio = fechaReferencia.with(LocalTime.MIN);
                fin = fechaReferencia.with(LocalTime.MAX);
                break;

            case SEMANAL:
                inicio = fechaReferencia.with(TemporalAdjusters.previousOrSame
                        (DayOfWeek.MONDAY)).with(LocalTime.MIN);
                fin = fechaReferencia.with(TemporalAdjusters.nextOrSame
                        (DayOfWeek.SUNDAY)).with(LocalTime.MAX);
                break;

            case QUINCENAL:
                if (fechaReferencia.getDayOfMonth() <= 15) {
                    inicio = fechaReferencia.withDayOfMonth(1).
                            with(LocalTime.MIN);
                    fin = fechaReferencia.withDayOfMonth(15).
                            with(LocalTime.MAX);
                } else {
                    inicio = fechaReferencia.withDayOfMonth(16).
                            with(LocalTime.MIN);
                    fin = fechaReferencia.with(TemporalAdjusters.
                            lastDayOfMonth()).with(LocalTime.MAX);
                }
                break;

            case MENSUAL:
                inicio = fechaReferencia.with(TemporalAdjusters.
                        firstDayOfMonth()).with(LocalTime.MIN);
                fin = fechaReferencia.with(TemporalAdjusters.
                        lastDayOfMonth()).with(LocalTime.MAX);
                break;

            default:
                throw new IllegalArgumentException
                        ("Período de presupuesto no soportado: " + periodo);
        }

        return new RangoFechas(inicio, fin);
    }
}