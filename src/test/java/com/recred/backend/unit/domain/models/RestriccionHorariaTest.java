package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RestriccionHorariaTest {

    private FranjaHoraria franjaRecreo;
    private LocalDateTime momentoCompra;
    private Producto producto;

    @BeforeEach
    void setUp() {
        franjaRecreo = new FranjaHoraria(UUID.randomUUID(), UUID.randomUUID(), "Recreo",
                LocalTime.of(10, 0), LocalTime.of(10, 30), true);

        momentoCompra = LocalDateTime.of(2026, 5, 25, 10, 15);

        producto = new Producto();
        CategoriaProducto catKiosco = new CategoriaProducto();
        catKiosco.setId(UUID.randomUUID());
        producto.setCategoria(catKiosco);

        ClasificacionSalud tipoSalud = new ClasificacionSalud();
        tipoSalud.setId(UUID.randomUUID());
        tipoSalud.setDescripcion("Sin TACC");
        producto.setClasificacionesSalud(Set.of(tipoSalud));
    }

    @Test
    void noBloquea_SiRestriccionEstaInactiva() {
        RestriccionHoraria restriccion = new RestriccionHoraria(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo,
                null, null);
        restriccion.setActiva(false);

        assertFalse(restriccion.bloquea(producto, momentoCompra));
    }

    @Test
    void noBloquea_SiEstaFueraDeLaFranjaHoraria() {
        RestriccionHoraria restriccion = new RestriccionHoraria(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo,
                null, null);
        LocalDateTime momentoFueraDeHora = LocalDateTime.of(2026, 5, 25, 11, 0);

        assertFalse(restriccion.bloquea(producto, momentoFueraDeHora));
    }

    @Test
    void bloquea_Totalmente_SiNoHayFiltrosConfigurados() {
        RestriccionHoraria restriccion = new RestriccionHoraria(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo,
                null, null);

        assertTrue(restriccion.bloquea(producto, momentoCompra));
    }

    @Test
    void bloquea_SiCoincideLaCategoria() {
        RestriccionHoraria restriccion = new RestriccionHoraria(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo,
                producto.getCategoria(), null);

        assertTrue(restriccion.bloquea(producto, momentoCompra));
    }

    @Test
    void bloquea_SiCoincideLaClasificacion() {
        ClasificacionSalud clasificacionRestringida = producto.getClasificacionesSalud().iterator().next();
        RestriccionHoraria restriccion = new RestriccionHoraria(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo,
                null, clasificacionRestringida);

        assertTrue(restriccion.bloquea(producto, momentoCompra));
    }

    @Test
    void noBloquea_SiFiltrosNoCoincidenConElProducto() {
        CategoriaProducto otraCategoria = new CategoriaProducto();
        otraCategoria.setId(UUID.randomUUID());

        RestriccionHoraria restriccion = new RestriccionHoraria(UUID.randomUUID(), UUID.randomUUID(), franjaRecreo,
                otraCategoria, null);

        assertFalse(restriccion.bloquea(producto, momentoCompra));
    }
}