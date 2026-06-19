package com.recred.backend.unit.domain.services;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.RangoFechas;
import com.recred.backend.domain.services.BudgetPeriodCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BudgetPeriodCalculatorTest {

    private BudgetPeriodCalculator calculador;

    @BeforeEach
    void setUp() {
        calculador = new BudgetPeriodCalculator();
    }

    @Test
    void calculateRange_Diario_RetornaRangoDelMismoDia() {

        LocalDateTime fechaReferencia = LocalDateTime.of(2026, 5, 13, 15, 30);

        RangoFechas rango = calculador.calculateRange(PeriodoPresupuesto.DIARIO, fechaReferencia);

        assertEquals(fechaReferencia.with(LocalTime.MIN), rango.inicio());
        assertEquals(fechaReferencia.with(LocalTime.MAX), rango.fin());
    }

    @Test
    void calculateRange_Semanal_RetornaDeLunesADomingo() {

        LocalDateTime fechaReferencia = LocalDateTime.of(2026, 5, 13, 15, 30);

        RangoFechas rango = calculador.calculateRange(PeriodoPresupuesto.SEMANAL, fechaReferencia);

        assertEquals(LocalDateTime.of(2026, 5, 11, 0, 0, 0, 0), rango.inicio());

        assertEquals(LocalDateTime.of(2026, 5, 17, 23, 59, 59, 999999999), rango.fin());
    }

    @Test
    void calculateRange_QuincenalPrimeraMitad_RetornaDel1Al15() {

        LocalDateTime fechaReferencia = LocalDateTime.of(2026, 5, 10, 10, 0);

        RangoFechas rango = calculador.calculateRange(PeriodoPresupuesto.QUINCENAL, fechaReferencia);

        assertEquals(LocalDateTime.of(2026, 5, 1, 0, 0, 0, 0), rango.inicio());
        assertEquals(LocalDateTime.of(2026, 5, 15, 23, 59, 59, 999999999), rango.fin());
    }

    @Test
    void calculateRange_QuincenalSegundaMitad_RetornaDel16AlFinDeMes() {

        LocalDateTime fechaReferencia = LocalDateTime.of(2026, 5, 20, 10, 0);

        RangoFechas rango = calculador.calculateRange(PeriodoPresupuesto.QUINCENAL, fechaReferencia);

        assertEquals(LocalDateTime.of(2026, 5, 16, 0, 0, 0, 0), rango.inicio());
        assertEquals(LocalDateTime.of(2026, 5, 31, 23, 59, 59, 999999999), rango.fin());
    }

    @Test
    void calculateRange_Mensual_RetornaDelPrimerAlUltimoDiaDelMes() {

        LocalDateTime fechaReferencia = LocalDateTime.of(2026, 2, 15, 12, 0);

        RangoFechas rango = calculador.calculateRange(PeriodoPresupuesto.MENSUAL, fechaReferencia);

        assertEquals(LocalDateTime.of(2026, 2, 1, 0, 0, 0, 0), rango.inicio());
        assertEquals(LocalDateTime.of(2026, 2, 28, 23, 59, 59, 999999999), rango.fin());
    }

    @Test
    void calculateRange_PeriodoNulo_LanzaExcepcion() {
        LocalDateTime fechaReferencia = LocalDateTime.now();

        assertThrows(NullPointerException.class, () -> {
            calculador.calculateRange(null, fechaReferencia);
        });
    }
}