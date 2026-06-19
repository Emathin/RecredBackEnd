package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.models.RewardLevel;
import com.recred.backend.domain.models.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void addHealthRestriction_NewRestriction_ShouldBeAdded() {
        alumno.addHealthRestriction(clasificacion);

        assertEquals(1, alumno.getRestriccionesSalud().size());
        assertTrue(alumno.getRestriccionesSalud().contains(clasificacion));
    }

    @Test
    void addHealthRestriction_DuplicateRestriction_ShouldNotBeAdded() {
        alumno.addHealthRestriction(clasificacion);

        alumno.addHealthRestriction(clasificacion);

        ClasificacionSalud clasificacionDuplicada = new ClasificacionSalud();
        clasificacionDuplicada.setId(clasificacionId);
        clasificacionDuplicada.setDescripcion("Celíaco Modificado");

        alumno.addHealthRestriction(clasificacionDuplicada);

        assertEquals(1, alumno.getRestriccionesSalud().size());
    }

    @Test
    void addHealthRestriction_NullRestriction_ShouldDoNothing() {
        alumno.addHealthRestriction(null);

        assertEquals(0, alumno.getRestriccionesSalud().size());
    }

    @Test
    void removeHealthRestriction_ExistingRestriction_ShouldBeRemoved() {
        alumno.addHealthRestriction(clasificacion);
        assertEquals(1, alumno.getRestriccionesSalud().size());

        alumno.removeHealthRestriction(clasificacionId);

        assertEquals(0, alumno.getRestriccionesSalud().size());
    }

    @Test
    void removeHealthRestriction_MissingRestriction_ShouldDoNothing() {
        alumno.addHealthRestriction(clasificacion);

        alumno.removeHealthRestriction(UUID.randomUUID());

        assertEquals(1, alumno.getRestriccionesSalud().size());
    }

    @Test
    void dadoUnProductoNuevo_cuandoBloquearProducto_entoncesSeAgregaALaLista() {
        Alumno alumno = new Alumno();
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());

        alumno.blockProduct(producto);

        assertEquals(1, alumno.getProductosBloqueados().size());
        assertTrue(alumno.getProductosBloqueados().contains(producto));
    }

    @Test
    void dadoUnProductoYaBloqueado_cuandoBloquearProducto_entoncesNoSeDuplica() {
        Alumno alumno = new Alumno();
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());

        alumno.blockProduct(producto);
        alumno.blockProduct(producto);

        assertEquals(1, alumno.getProductosBloqueados().size());
    }

    @Test
    void givenBlockedProduct_whenUnblockProduct_thenItIsRemovedFromList() {
        Alumno alumno = new Alumno();
        UUID productoId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(productoId);

        alumno.blockProduct(producto);
        alumno.unblockProduct(productoId);

        assertTrue(alumno.getProductosBloqueados().isEmpty());
    }

    @Test
    void dadoProductoNulo_cuandoBloquearProducto_entoncesNoHaceNada() {
        alumno.blockProduct(null);

        assertTrue(alumno.getProductosBloqueados().isEmpty());
    }

    @Test
    void dadoProductoFavoritoNuevo_cuandoAddFavoriteProduct_entoncesLoAgrega() {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());

        alumno.addFavoriteProduct(producto);

        assertEquals(1, alumno.getProductosFavoritos().size());
    }

    @Test
    void dadoProductoFavoritoDuplicado_cuandoAddFavoriteProduct_entoncesNoDuplica() {
        UUID productoId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(productoId);
        Producto duplicado = new Producto();
        duplicado.setId(productoId);

        alumno.addFavoriteProduct(producto);
        alumno.addFavoriteProduct(duplicado);

        assertEquals(1, alumno.getProductosFavoritos().size());
    }

    @Test
    void dadoProductoFavorito_cuandoRemoveFavoriteProduct_entoncesLoRemueve() {
        UUID productoId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(productoId);
        alumno.addFavoriteProduct(producto);

        alumno.removeFavoriteProduct(productoId);

        assertTrue(alumno.getProductosFavoritos().isEmpty());
    }

    @Test
    void givenTimeRestriction_whenAddAndRemove_thenUpdatesCollection() {
        RestriccionHoraria restriction = new RestriccionHoraria();

        alumno.addTimeRestriction(restriction);
        alumno.removeTimeRestriction(restriction);

        assertTrue(alumno.getRestriccionesHorarias().isEmpty());
    }

    @Test
    void givenActiveStudent_whenDeactivate_thenBecomesInactive() {
        alumno.deactivate();

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
