package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
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

    @Test
    void dadoProductoNulo_cuandoBloquearProducto_entoncesNoHaceNada() {
        alumno.bloquearProducto(null);

        assertTrue(alumno.getProductosBloqueados().isEmpty());
    }

    @Test
    void dadoProductoFavoritoNuevo_cuandoAgregarProductoFavorito_entoncesLoAgrega() {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());

        alumno.agregarProductoFavorito(producto);

        assertEquals(1, alumno.getProductosFavoritos().size());
    }

    @Test
    void dadoProductoFavoritoDuplicado_cuandoAgregarProductoFavorito_entoncesNoDuplica() {
        UUID productoId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(productoId);
        Producto duplicado = new Producto();
        duplicado.setId(productoId);

        alumno.agregarProductoFavorito(producto);
        alumno.agregarProductoFavorito(duplicado);

        assertEquals(1, alumno.getProductosFavoritos().size());
    }

    @Test
    void dadoProductoFavorito_cuandoRemoverProductoFavorito_entoncesLoRemueve() {
        UUID productoId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(productoId);
        alumno.agregarProductoFavorito(producto);

        alumno.removerProductoFavorito(productoId);

        assertTrue(alumno.getProductosFavoritos().isEmpty());
    }

    @Test
    void dadoRestriccionHoraria_cuandoAgregarYRemover_entoncesActualizaColeccion() {
        RestriccionHoraria restriccion = new RestriccionHoraria();

        alumno.agregarRestriccionHoraria(restriccion);
        alumno.removerRestriccionHoraria(restriccion);

        assertTrue(alumno.getRestriccionesHorarias().isEmpty());
    }

    @Test
    void dadoAlumnoActivo_cuandoDarDeBaja_entoncesQuedaInactivo() {
        alumno.darDeBaja();

        assertEquals(false, alumno.getActivo());
    }

    @Test
    void dadoPuntosNulos_cuandoAddRewardPoints_entoncesInicializaYAcumula() {
        alumno.setPuntosRecompensa(null);

        alumno.addRewardPoints(20);

        assertEquals(20, alumno.getPuntosRecompensa());
    }

    @Test
    void constructorParametrizadoDebeAsignarCamposYUsarRestriccionesVaciasSiSonNulas() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario();
        Grado grado = new Grado();

        Alumno creado = new Alumno(id, usuario, false, grado, null);

        assertEquals(id, creado.getId());
        assertEquals(usuario, creado.getUsuario());
        assertEquals(false, creado.getActivo());
        assertEquals(grado, creado.getGrado());
        assertTrue(creado.getRestriccionesSalud().isEmpty());
    }

    @Test
    void constructorParametrizadoDebeConservarRestriccionesRecibidas() {
        UUID id = UUID.randomUUID();

        Alumno creado = new Alumno(id, null, true, null, Set.of(clasificacion));

        assertEquals(1, creado.getRestriccionesSalud().size());
        assertTrue(creado.getRestriccionesSalud().contains(clasificacion));
    }

    @Test
    void calculateCurrentLevelShouldReturnBronzeSilverAndGold() {
        alumno.setPuntosRecompensa(null);
        assertEquals(RewardLevel.BRONZE, alumno.calculateCurrentLevel());

        alumno.setPuntosRecompensa(99);
        assertEquals(RewardLevel.BRONZE, alumno.calculateCurrentLevel());

        alumno.setPuntosRecompensa(100);
        assertEquals(RewardLevel.SILVER, alumno.calculateCurrentLevel());

        alumno.setPuntosRecompensa(250);
        assertEquals(RewardLevel.GOLD, alumno.calculateCurrentLevel());
    }

    @Test
    void getPointsToNextLevelShouldHandleAllLevels() {
        alumno.setPuntosRecompensa(null);
        assertEquals(100, alumno.getPointsToNextLevel());

        alumno.setPuntosRecompensa(120);
        assertEquals(130, alumno.getPointsToNextLevel());

        alumno.setPuntosRecompensa(260);
        assertEquals(0, alumno.getPointsToNextLevel());
    }
}
