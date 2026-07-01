package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.RangoFechas;

import java.time.LocalDateTime;

public class CalculadorPeriodoPresupuesto {

    public RangoFechas calcularRango(PeriodoPresupuesto periodo, LocalDateTime fechaReferencia) {
        if (periodo == null) {
            throw new NullPointerException("El periodo no puede ser nulo");
        }
        return periodo.calculateRange(fechaReferencia);
    }
}
