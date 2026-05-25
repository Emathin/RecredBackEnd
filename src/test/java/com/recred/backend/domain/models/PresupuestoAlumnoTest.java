package com.recred.backend.domain.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}