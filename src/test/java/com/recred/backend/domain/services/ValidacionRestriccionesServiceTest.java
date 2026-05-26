package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class ValidacionRestriccionesServiceTest {

    @InjectMocks
    private ValidacionRestriccionesService validacionRestriccionesService;

    @Mock
    private Alumno alumnoMock;

    @Test
    void validarRestricciones_PasaExitosamente_SiNoHayConflicto() {
        Producto productoSeguro = new Producto();
        Set<Producto> carrito = Set.of(productoSeguro);

        assertDoesNotThrow(() -> validacionRestriccionesService.validarCompra(alumnoMock, carrito));
    }
}