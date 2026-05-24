package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.RangoFecha;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangoPrediccionGastoServiceTest {

    private final RangoPrediccionGastoService service = new RangoPrediccionGastoService();

    @Test
    void calcularRangoDiario() {
        LocalDate fecha = LocalDate.of(2026, 5, 21);

        RangoFecha rango = service.calcular(PeriodoPresupuesto.DIARIO, fecha);

        assertEquals(fecha, rango.fechaInicio());
        assertEquals(fecha, rango.fechaFin());
        assertTrue(rango.contiene(fecha));
    }

    @Test
    void calcularRangoSemanalDeLunesADomingo() {
        RangoFecha rango = service.calcular(
                PeriodoPresupuesto.SEMANAL,
                LocalDate.of(2026, 5, 21)
        );

        assertEquals(LocalDate.of(2026, 5, 18), rango.fechaInicio());
        assertEquals(LocalDate.of(2026, 5, 24), rango.fechaFin());
    }

    @Test
    void calcularPrimeraQuincena() {
        RangoFecha rango = service.calcular(
                PeriodoPresupuesto.QUINCENAL,
                LocalDate.of(2026, 5, 15)
        );

        assertEquals(LocalDate.of(2026, 5, 1), rango.fechaInicio());
        assertEquals(LocalDate.of(2026, 5, 15), rango.fechaFin());
    }

    @Test
    void calcularSegundaQuincena() {
        RangoFecha rango = service.calcular(
                PeriodoPresupuesto.QUINCENAL,
                LocalDate.of(2026, 2, 16)
        );

        assertEquals(LocalDate.of(2026, 2, 16), rango.fechaInicio());
        assertEquals(LocalDate.of(2026, 2, 28), rango.fechaFin());
    }

    @Test
    void calcularRangoMensual() {
        RangoFecha rango = service.calcular(
                PeriodoPresupuesto.MENSUAL,
                LocalDate.of(2026, 2, 10)
        );

        assertEquals(LocalDate.of(2026, 2, 1), rango.fechaInicio());
        assertEquals(LocalDate.of(2026, 2, 28), rango.fechaFin());
    }
}
