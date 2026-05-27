package com.recred.backend.domain.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresupuestoAlumnoTest {

    @Test
    void esVigente_FechaAnteriorAlInicio_RetornaFalse() {
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();

        presupuesto.setFechaInicio(LocalDate.now().plusDays(1));

        boolean resultado = presupuesto.esVigente(LocalDate.now());

        assertFalse(resultado, "No debería ser vigente si la fecha de consulta es anterior al inicio");
    }

    @Test
    void esVigente_FechaIgualAlInicio_RetornaTrue() {
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();

        presupuesto.setFechaInicio(LocalDate.now());

        boolean resultado = presupuesto.esVigente(LocalDate.now());

        assertTrue(resultado, "Debería ser vigente si la fecha coincide con el inicio");
    }

    @Test
    void esVigente_FechaPosteriorAlInicio_RetornaTrue() {
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();

        presupuesto.setFechaInicio(LocalDate.now().minusDays(1));

        boolean resultado = presupuesto.esVigente(LocalDate.now());

        assertTrue(resultado, "Debería ser vigente si la fecha es posterior al inicio");
    }

    @Test
    void validarPorcentajes_SumaMenorOIgualA100_NoHaceNada() {
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();

        ReglaPresupuestoCategoria regla1 = new ReglaPresupuestoCategoria();
        regla1.setPorcentajeLimite(new BigDecimal("50.00"));
        regla1.setActivo(true);

        ReglaPresupuestoCategoria regla2 = new ReglaPresupuestoCategoria();
        regla2.setPorcentajeLimite(new BigDecimal("50.00"));
        regla2.setActivo(true);

        presupuesto.setReglasCategoria(List.of(regla1, regla2));

        assertDoesNotThrow(presupuesto::validarPorcentajes);
    }

    @Test
    void validarPorcentajes_SumaMayorA100_LanzaExcepcion() {
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();

        ReglaPresupuestoCategoria regla1 = new ReglaPresupuestoCategoria();
        regla1.setPorcentajeLimite(new BigDecimal("60.00"));
        regla1.setActivo(true);

        ReglaPresupuestoCategoria regla2 = new ReglaPresupuestoCategoria();
        regla2.setPorcentajeLimite(new BigDecimal("50.00"));
        regla2.setActivo(true);

        presupuesto.setReglasCategoria(List.of(regla1, regla2));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, presupuesto::validarPorcentajes);
        assertTrue(ex.getMessage().contains("no puede superar el 100%"));
    }

    @Test
    void recalcularMontosReglas_CalculaElPorcentajeCorrectamente() {
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();
        presupuesto.setMontoLimiteGeneral(new BigDecimal("10000.00"));

        ReglaPresupuestoCategoria regla = new ReglaPresupuestoCategoria();
        regla.setPorcentajeLimite(new BigDecimal("20.00"));
        regla.setActivo(true);

        presupuesto.setReglasCategoria(List.of(regla));

        presupuesto.recalcularMontosReglas();

        assertEquals(new BigDecimal("2000.00"), presupuesto.getReglasCategoria().get(0).getMontoLimiteCalculado());
    }
}