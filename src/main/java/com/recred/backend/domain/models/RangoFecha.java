package com.recred.backend.domain.models;

import java.time.LocalDate;

public record RangoFecha(LocalDate fechaInicio, LocalDate fechaFin) {

    public boolean contiene(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }
}
