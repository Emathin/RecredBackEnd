package com.recred.backend.compras.application.services;

import com.recred.backend.catalogo.infrastructure.entities.ProductoEntity;
import com.recred.backend.control_parental.domain.exceptions.RestriccionSaludException;
import com.recred.backend.control_parental.domain.validators.CompraValidator;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        AlumnoEntity alumno = new AlumnoEntity();
        ProductoEntity producto = new ProductoEntity();
        List<ProductoEntity> carrito = List.of(producto);

        doThrow(new RestriccionSaludException("El producto contiene ingredientes restringidos."))
                .when(validadorMock).validar(alumno, carrito);

        assertThrows(RestriccionSaludException.class, () -> {
            compraService.procesarCompra(alumno, carrito);
        });

        verify(validadorMock, times(1)).validar(alumno, carrito);
    }

    @Test
    void procesarCompra_NoDebeLanzarExcepcion_CuandoElCarritoEsSaludable() {
        AlumnoEntity alumno = new AlumnoEntity();
        ProductoEntity producto = new ProductoEntity();
        List<ProductoEntity> carrito = List.of(producto);

        doNothing().when(validadorMock).validar(alumno, carrito);

        assertDoesNotThrow(() -> {
            compraService.procesarCompra(alumno, carrito);
        });

        verify(validadorMock, times(1)).validar(alumno, carrito);
    }
}