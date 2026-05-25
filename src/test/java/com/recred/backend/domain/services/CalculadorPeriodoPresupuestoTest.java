package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.RangoFechas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculadorPeriodoPresupuestoTest {

    private CalculadorPeriodoPresupuesto calculador;

    @BeforeEach
    void setUp() {
        calculador = new CalculadorPeriodoPresupuesto();
    }

    @Test
    void calcularRango_Diario_RetornaRangoDelMismoDia() {

        LocalDateTime fechaReferencia = LocalDateTime.of(2026, 5, 13, 15, 30);

        RangoFechas rango = calculador.calcularRango(PeriodoPresupuesto.DIARIO, fechaReferencia);

        assertEquals(fechaReferencia.with(LocalTime.MIN), rango.inicio());
        assertEquals(fechaReferencia.with(LocalTime.MAX), rango.fin());
    }

    @Test
    void calcularRango_Semanal_RetornaDeLunesADomingo() {

        LocalDateTime fechaReferencia = LocalDateTime.of(2026, 5, 13, 15, 30);

        RangoFechas rango = calculador.calcularRango(PeriodoPresupuesto.SEMANAL, fechaReferencia);

        assertEquals(LocalDateTime.of(2026, 5, 11, 0, 0, 0, 0), rango.inicio());

        assertEquals(LocalDateTime.of(2026, 5, 17, 23, 59, 59, 999999999), rango.fin());
    }

    @Test
    void calcularRango_QuincenalPrimeraMitad_RetornaDel1Al15() {

        LocalDateTime fechaReferencia = LocalDateTime.of(2026, 5, 10, 10, 0);

        RangoFechas rango = calculador.calcularRango(PeriodoPresupuesto.QUINCENAL, fechaReferencia);

        assertEquals(LocalDateTime.of(2026, 5, 1, 0, 0, 0, 0), rango.inicio());
        assertEquals(LocalDateTime.of(2026, 5, 15, 23, 59, 59, 999999999), rango.fin());
    }

    @Test
    void calcularRango_QuincenalSegundaMitad_RetornaDel16AlFinDeMes() {

        LocalDateTime fechaReferencia = LocalDateTime.of(2026, 5, 20, 10, 0);

        RangoFechas rango = calculador.calcularRango(PeriodoPresupuesto.QUINCENAL, fechaReferencia);

        assertEquals(LocalDateTime.of(2026, 5, 16, 0, 0, 0, 0), rango.inicio());
        assertEquals(LocalDateTime.of(2026, 5, 31, 23, 59, 59, 999999999), rango.fin());
    }

    @Test
    void calcularRango_Mensual_RetornaDelPrimerAlUltimoDiaDelMes() {

        LocalDateTime fechaReferencia = LocalDateTime.of(2026, 2, 15, 12, 0);

        RangoFechas rango = calculador.calcularRango(PeriodoPresupuesto.MENSUAL, fechaReferencia);

        assertEquals(LocalDateTime.of(2026, 2, 1, 0, 0, 0, 0), rango.inicio());
        assertEquals(LocalDateTime.of(2026, 2, 28, 23, 59, 59, 999999999), rango.fin());
    }

    @Test
    void calcularRango_PeriodoNulo_LanzaExcepcion() {
        LocalDateTime fechaReferencia = LocalDateTime.now();

        assertThrows(NullPointerException.class, () -> {
            calculador.calcularRango(null, fechaReferencia);
        });
    }
}