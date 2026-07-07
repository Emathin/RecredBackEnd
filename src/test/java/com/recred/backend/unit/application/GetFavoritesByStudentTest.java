package com.recred.backend.unit.application;

import com.recred.backend.application.GetFavoritesByStudent;
import com.recred.backend.application.GetFavoritesByStudentRequest;
import com.recred.backend.application.GetFavoritesByStudentResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetFavoritesByStudentTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    private GetFavoritesByStudent useCase;

    private UUID studentId;
    private Alumno student;
    private Producto favoriteProduct;
    private Set<GetFavoritesByStudentResponse> response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new GetFavoritesByStudent(alumnoRepository);
    }

    @Test
    void givenStudentWithFavoriteProduct_whenGettingFavorites_thenReturnsFavoriteProduct() {
        givenFavoriteProductScenario();
        givenStudentHasFavoriteProductWithMetadata();
        givenStudentExists();

        whenGettingFavorites();

        thenFavoriteProductIsReturned();
        thenFavoriteProductMetadataIsMapped();
        thenStudentWasRequested();
    }

    @Test
    void givenFavoriteProductWithoutOptionalMetadata_whenGettingFavorites_thenReturnsEmptyMetadata() {
        givenFavoriteProductScenario();
        givenStudentHasFavoriteProductWithoutOptionalMetadata();
        givenStudentExists();

        whenGettingFavorites();

        thenFavoriteProductIsReturned();
        thenOptionalMetadataIsEmpty();
    }

    @Test
    void givenMissingStudent_whenGettingFavorites_thenFails() {
        givenFavoriteProductScenario();
        givenStudentDoesNotExist();

        whenGettingFavoritesFails();

        thenFavoritesFailWith("Alumno no encontrado");
        thenStudentWasRequested();
    }

    private void givenFavoriteProductScenario() {
        studentId = UUID.randomUUID();
        student = new Alumno();
        student.setId(studentId);
    }

    private void givenStudentHasFavoriteProductWithMetadata() {
        favoriteProduct = product("Yogur");
        favoriteProduct.setCategoria(category("Lacteos"));
        favoriteProduct.setClasificacionesSalud(Set.of(healthClassification("Sin TACC")));
        favoriteProduct.setTipos(Set.of(typeClassification("Bebida")));
        student.agregarProductoFavorito(favoriteProduct);
    }

    private void givenStudentHasFavoriteProductWithoutOptionalMetadata() {
        favoriteProduct = product("Agua");
        favoriteProduct.setCategoria(null);
        favoriteProduct.setClasificacionesSalud(null);
        favoriteProduct.setTipos(null);
        student.agregarProductoFavorito(favoriteProduct);
    }

    private void givenStudentExists() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void whenGettingFavorites() {
        response = useCase.execute(new GetFavoritesByStudentRequest(studentId));
    }

    private void whenGettingFavoritesFails() {
        thrownException = assertThrows(RuntimeException.class, () ->
                useCase.execute(new GetFavoritesByStudentRequest(studentId)));
    }

    private void thenFavoriteProductIsReturned() {
        assertEquals(1, response.size());
        assertEquals(favoriteProduct.getId(), favoriteResponse().id());
        assertEquals(favoriteProduct.getNombre(), favoriteResponse().nombre());
    }

    private void thenFavoriteProductMetadataIsMapped() {
        GetFavoritesByStudentResponse favorite = favoriteResponse();
        assertEquals("Lacteos", favorite.categoria().descripcion());
        assertEquals(1, favorite.clasificacionesSalud().size());
        assertEquals(1, favorite.tipos().size());
        assertEquals("DISPONIBLE", favorite.estadoInventario());
        assertEquals("STOCK_EXACTO", favorite.tipoManejoInventario());
    }

    private void thenOptionalMetadataIsEmpty() {
        GetFavoritesByStudentResponse favorite = favoriteResponse();
        assertNull(favorite.categoria());
        assertTrue(favorite.clasificacionesSalud().isEmpty());
        assertTrue(favorite.tipos().isEmpty());
    }

    private void thenFavoritesFailWith(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private void thenStudentWasRequested() {
        verify(alumnoRepository).findById(studentId);
    }

    private GetFavoritesByStudentResponse favoriteResponse() {
        return response.iterator().next();
    }

    private Producto product(String name) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setDescripcion("Descripcion");
        product.setPrecio(new BigDecimal("100.00"));
        product.setActivo(true);
        product.setStockActual(10);
        product.setStockReservado(2);
        product.setStockMinimo(1);
        product.setStockDisponible(8);
        product.setEstadoInventario(EstadoInventario.DISPONIBLE);
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);
        return product;
    }

    private CategoriaProducto category(String description) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion(description);
        return category;
    }

    private ClasificacionSalud healthClassification(String description) {
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(UUID.randomUUID());
        classification.setDescripcion(description);
        return classification;
    }

    private ClasificacionTipo typeClassification(String description) {
        ClasificacionTipo classification = new ClasificacionTipo();
        classification.setId(UUID.randomUUID());
        classification.setDescripcion(description);
        return classification;
    }
}
