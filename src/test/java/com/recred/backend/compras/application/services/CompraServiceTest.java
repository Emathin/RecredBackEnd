package com.recred.backend.compras.application.services;

import com.recred.backend.dominio.exceptions.RestriccionSaludException;
import com.recred.backend.dominio.models.Alumno;
import com.recred.backend.dominio.models.Producto;
import com.recred.backend.control_parental.domain.validators.CompraValidator;
import com.recred.backend.dominio.implementation.CompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CompraServiceTest {

    private CompraValidator validadorMock;
    private CompraService compraService;

    @BeforeEach
    void setUp() {
        validadorMock = mock(CompraValidator.class);
        compraService = new CompraService(List.of(validadorMock));
    }

    @Test
    void procesarCompra_DebeLanzarExcepcion_CuandoUnValidadorFalla() {
        Alumno alumno = new Alumno(UUID.randomUUID(), null);
        Producto producto = new Producto(UUID.randomUUID(), "Producto con Restricción", null);
        List<Producto> carrito = List.of(producto);

        doThrow(new RestriccionSaludException("El producto contiene ingredientes restringidos."))
                .when(validadorMock).validar(alumno, carrito);

        assertThrows(RestriccionSaludException.class, () -> {
            compraService.procesarCompra(alumno, carrito);
        });

        verify(validadorMock, times(1)).validar(alumno, carrito);
    }

    @Test
    void procesarCompra_NoDebeLanzarExcepcion_CuandoElCarritoEsSaludable() {
        Alumno alumno = new Alumno(UUID.randomUUID(), null);
        Producto producto = new Producto(UUID.randomUUID(), "Producto sin Restricción", null);
        List<Producto> carrito = List.of(producto);

        doNothing().when(validadorMock).validar(alumno, carrito);

        assertDoesNotThrow(() -> {
            compraService.procesarCompra(alumno, carrito);
        });

        verify(validadorMock, times(1)).validar(alumno, carrito);
    }
}