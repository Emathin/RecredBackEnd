package com.recred.backend.unit.domain.exceptions;

import com.recred.backend.domain.exceptions.RestriccionSaludException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RestriccionSaludExceptionTest {

    @Test
    void crearExcepcion_GuardaElMensajeCorrectamente() {
        String mensajeEsperado = "El producto contiene ingredientes no aptos";
        RestriccionSaludException excepcion = new RestriccionSaludException(mensajeEsperado);

        assertEquals(mensajeEsperado, excepcion.getMessage());
    }
}