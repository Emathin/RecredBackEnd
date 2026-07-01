package com.recred.backend.domain.models;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public enum PeriodoPresupuesto {
    DIARIO {
        @Override
        public RangoFechas calculateRange(LocalDateTime referenceDate) {
            return new RangoFechas(
                referenceDate.with(LocalTime.MIN),
                referenceDate.with(LocalTime.MAX)
            );
        }
    },
    SEMANAL {
        @Override
        public RangoFechas calculateRange(LocalDateTime referenceDate) {
            return new RangoFechas(
                referenceDate.with(TemporalAdjusters.
                        previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN),
                referenceDate.with(TemporalAdjusters.
                        nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.MAX)
            );
        }
    },
    QUINCENAL {
        @Override
        public RangoFechas calculateRange(LocalDateTime referenceDate) {
            if (referenceDate.getDayOfMonth() <= 15) {
                return new RangoFechas(
                    referenceDate.withDayOfMonth(1).with(LocalTime.MIN),
                    referenceDate.withDayOfMonth(15).with(LocalTime.MAX)
                );
            } else {
                return new RangoFechas(
                    referenceDate.withDayOfMonth(16).with(LocalTime.MIN),
                    referenceDate.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
                );
            }
        }
    },
    MENSUAL {
        @Override
        public RangoFechas calculateRange(LocalDateTime referenceDate) {
            return new RangoFechas(
                referenceDate.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN),
                referenceDate.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
            );
        }
    };

    public abstract RangoFechas calculateRange(LocalDateTime referenceDate);
}
