package com.recred.backend.domain.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthRestrictionExceptionTest {

    @Test
    void crearExcepcion_GuardaElMensajeCorrectamente() {
        String mensajeEsperado = "El producto contiene ingredientes no aptos";
        HealthRestrictionException excepcion = new HealthRestrictionException(mensajeEsperado);

        assertEquals(mensajeEsperado, excepcion.getMessage());
    }
}