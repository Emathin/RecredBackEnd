package com.recred.backend.unit.domain.services;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

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

    @Test
    void validarRestricciones_CuandoHayConflicto_DeberiaLanzarExcepcion() {
        ClasificacionSalud restriccion = new ClasificacionSalud();
        restriccion.setDescripcion("Sin TACC");
        Producto producto = new Producto();
        producto.setNombre("Alfajor");
        producto.setClasificacionesSalud(Set.of(restriccion));
        when(alumnoMock.getRestriccionesSalud()).thenReturn(Set.of(restriccion));

        assertThrows(RestriccionSaludException.class,
                () -> validacionRestriccionesService.validarCompra(alumnoMock, Set.of(producto)));
    }
}
