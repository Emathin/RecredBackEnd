package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RestriccionHoraria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorHorarioServiceTest {

    @InjectMocks
    private ValidadorHorarioService validadorService;

    @Mock
    private Alumno alumno;

    @Mock
    private RestriccionHoraria restriccionMock;

    @Mock
    private Producto productoMock;

    @Test
    void validarCompra_LanzaExcepcion_SiProductoEstaBloqueado() {
        when(productoMock.getNombre()).thenReturn("Alfajor de Chocolate");

        when(alumno.getRestriccionesHorarias()).thenReturn(Set.of(restriccionMock));

        when(restriccionMock.bloquea(eq(productoMock), any(LocalDateTime.class))).thenReturn(true);

        Set<Producto> carrito = Set.of(productoMock);
        LocalDateTime ahora = LocalDateTime.now();

        RestriccionHorariaException exception = assertThrows(
                RestriccionHorariaException.class,
                () -> validadorService.validarCompra(alumno, carrito, ahora)
        );

        assertTrue(exception.getMessage().contains("Alfajor de Chocolate"));
        assertTrue(exception.getMessage().contains("bloqueadas en este horario"));
    }

    @Test
    void validarCompra_PasaExitosamente_SiNingunProductoEstaBloqueado() {
        when(alumno.getRestriccionesHorarias()).thenReturn(Set.of(restriccionMock));

        when(restriccionMock.bloquea(eq(productoMock), any(LocalDateTime.class))).thenReturn(false);

        Set<Producto> carrito = Set.of(productoMock);
        LocalDateTime ahora = LocalDateTime.now();

        assertDoesNotThrow(() -> validadorService.validarCompra(alumno, carrito, ahora));
    }

    @Test
    void validarCompra_ConRestriccionRealGlobal_LanzaExcepcion() {
        Alumno alumnoReal = new Alumno();
        FranjaHoraria franjaHoraria = new FranjaHoraria(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Recreo",
                LocalTime.MIN,
                LocalTime.MAX,
                true
        );
        alumnoReal.setRestriccionesHorarias(Set.of(new RestriccionHoraria(
                UUID.randomUUID(),
                UUID.randomUUID(),
                franjaHoraria,
                null,
                null
        )));
        Producto producto = new Producto();
        producto.setNombre("Alfajor");

        assertThrows(
                RestriccionHorariaException.class,
                () -> validadorService.validarCompra(
                        alumnoReal,
                        Set.of(producto),
                        LocalDateTime.of(2026, 6, 15, 10, 0)
                )
        );
    }
}
