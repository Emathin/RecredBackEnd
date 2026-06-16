package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlumnoTest {

    private Alumno alumno;
    private UUID clasificacionId;
    private ClasificacionSalud clasificacion;

    @BeforeEach
    void setUp() {
        alumno = new Alumno();
        alumno.setId(UUID.randomUUID());

        clasificacionId = UUID.randomUUID();
        clasificacion = new ClasificacionSalud();
        clasificacion.setId(clasificacionId);
        clasificacion.setDescripcion("Celíaco");
    }

    @Test
    void agregarRestriccionSalud_NuevaRestriccion_DeberiaAgregarse() {
        alumno.agregarRestriccionSalud(clasificacion);

        assertEquals(1, alumno.getRestriccionesSalud().size());
        assertTrue(alumno.getRestriccionesSalud().contains(clasificacion));
    }

    @Test
    void agregarRestriccionSalud_RestriccionDuplicada_NoDeberiaAgregarse() {
        alumno.agregarRestriccionSalud(clasificacion);

        alumno.agregarRestriccionSalud(clasificacion);

        ClasificacionSalud clasificacionDuplicada = new ClasificacionSalud();
        clasificacionDuplicada.setId(clasificacionId);
        clasificacionDuplicada.setDescripcion("Celíaco Modificado");

        alumno.agregarRestriccionSalud(clasificacionDuplicada);

        assertEquals(1, alumno.getRestriccionesSalud().size());
    }

    @Test
    void agregarRestriccionSalud_Nulo_NoDeberiaHacerNada() {
        alumno.agregarRestriccionSalud(null);

        assertEquals(0, alumno.getRestriccionesSalud().size());
    }

    @Test
    void removerRestriccionSalud_RestriccionExistente_DeberiaRemoverse() {
        alumno.agregarRestriccionSalud(clasificacion);
        assertEquals(1, alumno.getRestriccionesSalud().size());

        alumno.removerRestriccionSalud(clasificacionId);

        assertEquals(0, alumno.getRestriccionesSalud().size());
    }

    @Test
    void removerRestriccionSalud_RestriccionNoExistente_NoDeberiaHacerNada() {
        alumno.agregarRestriccionSalud(clasificacion);

        alumno.removerRestriccionSalud(UUID.randomUUID());

        assertEquals(1, alumno.getRestriccionesSalud().size());
    }

    @Test
    void dadoUnProductoNuevo_cuandoBloquearProducto_entoncesSeAgregaALaLista() {
        Alumno alumno = new Alumno();
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());

        alumno.bloquearProducto(producto);

        assertEquals(1, alumno.getProductosBloqueados().size());
        assertTrue(alumno.getProductosBloqueados().contains(producto));
    }

    @Test
    void dadoUnProductoYaBloqueado_cuandoBloquearProducto_entoncesNoSeDuplica() {
        Alumno alumno = new Alumno();
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());

        alumno.bloquearProducto(producto);
        alumno.bloquearProducto(producto);

        assertEquals(1, alumno.getProductosBloqueados().size());
    }

    @Test
    void dadoUnProductoBloqueado_cuandoDesbloquearProducto_entoncesSeEliminaDeLaLista() {
        Alumno alumno = new Alumno();
        UUID productoId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(productoId);

        alumno.bloquearProducto(producto);
        alumno.desbloquearProducto(productoId);

        assertTrue(alumno.getProductosBloqueados().isEmpty());
    }
}