package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.RangoFecha;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class RangoPrediccionGastoService {

    public RangoFecha calcular(PeriodoPresupuesto periodo, LocalDate fechaBase) {
        return switch (periodo) {
            case DIARIO -> new RangoFecha(fechaBase, fechaBase);
            case SEMANAL -> calcularSemana(fechaBase);
            case QUINCENAL -> calcularQuincena(fechaBase);
            case MENSUAL -> calcularMes(fechaBase);
        };
    }

    private RangoFecha calcularSemana(LocalDate fechaBase) {
        LocalDate inicio = fechaBase.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fin = fechaBase.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new RangoFecha(inicio, fin);
    }

    private RangoFecha calcularQuincena(LocalDate fechaBase) {
        LocalDate inicioMes = fechaBase.withDayOfMonth(1);
        if (fechaBase.getDayOfMonth() <= 15) {
            return new RangoFecha(inicioMes, inicioMes.withDayOfMonth(15));
        }
        return new RangoFecha(fechaBase.withDayOfMonth(16), fechaBase.withDayOfMonth(
                fechaBase.lengthOfMonth()));
    }

    private RangoFecha calcularMes(LocalDate fechaBase) {
        LocalDate inicio = fechaBase.withDayOfMonth(1);
        return new RangoFecha(inicio, fechaBase.withDayOfMonth(fechaBase.lengthOfMonth()));
    }
}
