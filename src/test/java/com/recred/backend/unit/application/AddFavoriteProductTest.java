package com.recred.backend.unit.application;

import com.recred.backend.application.AddFavoriteProduct;
import com.recred.backend.application.AddFavoriteProductRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddFavoriteProductTest {

    private static final int FREE_PLAN_FAVORITE_LIMIT = 5;
    private static final String FREE_PLAN_LIMIT_MESSAGE =
            "Límite de productos favoritos alcanzado para cuenta gratuita (máximo 5)";

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private TutorRepository tutorRepository;

    private AddFavoriteProduct addFavoriteProduct;
    private AddFavoriteProductRequest addFavoriteRequest;
    private Alumno alumno;
    private Alumno otherChild;
    private Producto producto;
    private Throwable thrownException;

    @BeforeEach
    void setUp() {
        addFavoriteProduct = new AddFavoriteProduct(
                alumnoRepository,
                productRepository,
                tutorRepository
        );
    }

    @Test
    void givenValidFavoriteProductRequest_whenAddingFavorite_thenAddsProductAndSavesStudent() {
        givenDefaultFavoriteProductRequest();
        givenAlumnoExists();
        givenProductExists();
        givenStudentHasNoTutors();

        whenAddFavoriteProduct();

        thenProductWasAddedAsFavorite();
        thenAlumnoWasSaved();
    }

    @Test
    void givenMissingAlumno_whenAddingFavorite_thenThrowsException() {
        givenDefaultFavoriteProductRequest();
        givenAlumnoDoesNotExist();

        whenAddFavoriteProductFails(RuntimeException.class);

        thenErrorMessageIs("Alumno no encontrado");
        thenProductWasNotSearched();
        thenAlumnoWasNotSaved();
    }

    @Test
    void givenMissingProduct_whenAddingFavorite_thenThrowsException() {
        givenDefaultFavoriteProductRequest();
        givenAlumnoExists();
        givenProductDoesNotExist();

        whenAddFavoriteProductFails(RuntimeException.class);

        thenErrorMessageIs("Producto no encontrado");
        thenTutorsWereNotSearched();
        thenAlumnoWasNotSaved();
    }

    @Test
    void givenFreePlanStudentAtFavoriteLimit_whenAddingFavorite_thenThrowsException() {
        givenFavoriteProductRequestForStudentAtFavoriteLimit();
        givenAlumnoExists();
        givenProductExists();
        givenStudentHasTutorWithPlan(TipoPlan.GRATUITO);

        whenAddFavoriteProductFails(IllegalArgumentException.class);

        thenErrorMessageIs(FREE_PLAN_LIMIT_MESSAGE);
        thenAlumnoWasNotSaved();
    }

    @Test
    void givenStudentWithoutTutorsAtFavoriteLimit_whenAddingFavorite_thenThrowsException() {
        givenFavoriteProductRequestForStudentAtFavoriteLimit();
        givenAlumnoExists();
        givenProductExists();
        givenStudentHasNoTutors();

        whenAddFavoriteProductFails(IllegalArgumentException.class);

        thenErrorMessageIs(FREE_PLAN_LIMIT_MESSAGE);
        thenAlumnoWasNotSaved();
    }

    @Test
    void givenFreePlanStudentAtLimitWithExistingFavorite_whenAddingFavorite_thenSavesStudent() {
        givenFavoriteProductRequestForExistingFavoriteAtLimit();
        givenAlumnoExists();
        givenProductExists();
        givenStudentHasTutorWithPlan(TipoPlan.GRATUITO);

        whenAddFavoriteProduct();

        thenProductWasAddedAsFavorite();
        thenAlumnoHasFavoriteProductCount(FREE_PLAN_FAVORITE_LIMIT);
        thenAlumnoWasSaved();
    }

    @Test
    void givenPaidPlanStudentAtFavoriteLimit_whenAddingFavorite_thenAddsProductAndSaves() {
        givenFavoriteProductRequestForStudentAtFavoriteLimit();
        givenAlumnoExists();
        givenProductExists();
        givenStudentHasTutorWithPlan(TipoPlan.INTERMEDIO);

        whenAddFavoriteProduct();

        thenProductWasAddedAsFavorite();
        thenAlumnoHasFavoriteProductCount(6);
        thenAlumnoWasSaved();
    }

    @Test
    void givenFreePlanChildrenAtCombinedFavoriteLimit_whenAddingFavorite_thenThrowsException() {
        givenFavoriteProductRequestForStudentWithFavoriteCount(2);
        givenOtherChildWithFavoriteCount(3);
        givenAlumnoExists();
        givenProductExists();
        givenStudentHasTutorWithPlanAndChildren(TipoPlan.GRATUITO, alumno, otherChild);

        whenAddFavoriteProductFails(IllegalArgumentException.class);

        thenErrorMessageIs(FREE_PLAN_LIMIT_MESSAGE);
        thenAlumnoWasNotSaved();
    }

    @Test
    void givenPaidPlanChildrenAtCombinedFavoriteLimit_whenAddingFavorite_thenAddsProductAndSaves() {
        givenFavoriteProductRequestForStudentWithFavoriteCount(2);
        givenOtherChildWithFavoriteCount(3);
        givenAlumnoExists();
        givenProductExists();
        givenStudentHasTutorWithPlanAndChildren(TipoPlan.AVANZADO, alumno, otherChild);

        whenAddFavoriteProduct();

        thenProductWasAddedAsFavorite();
        thenAlumnoHasFavoriteProductCount(3);
        thenAlumnoWasSaved();
    }

    private void givenDefaultFavoriteProductRequest() {
        addFavoriteRequest = request(randomUUID(), randomUUID());
        alumno = alumno(addFavoriteRequest.alumnoId());
        producto = producto(addFavoriteRequest.productoId());
    }

    private void givenFavoriteProductRequestForExistingFavoriteAtLimit() {
        addFavoriteRequest = request(randomUUID(), randomUUID());
        producto = producto(addFavoriteRequest.productoId());
        alumno = alumnoWithFavoriteProducts(
                addFavoriteRequest.alumnoId(),
                FREE_PLAN_FAVORITE_LIMIT - 1);
        alumno.agregarProductoFavorito(producto);
    }

    private void givenFavoriteProductRequestForStudentAtFavoriteLimit() {
        givenFavoriteProductRequestForStudentWithFavoriteCount(FREE_PLAN_FAVORITE_LIMIT);
    }

    private void givenFavoriteProductRequestForStudentWithFavoriteCount(int count) {
        addFavoriteRequest = request(randomUUID(), randomUUID());
        alumno = alumnoWithFavoriteProducts(addFavoriteRequest.alumnoId(), count);
        producto = producto(addFavoriteRequest.productoId());
    }

    private void givenOtherChildWithFavoriteCount(int count) {
        otherChild = alumnoWithFavoriteProducts(randomUUID(), count);
    }

    private void givenAlumnoExists() {
        when(alumnoRepository.findById(addFavoriteRequest.alumnoId()))
                .thenReturn(Optional.of(alumno));
    }

    private void givenAlumnoDoesNotExist() {
        when(alumnoRepository.findById(addFavoriteRequest.alumnoId()))
                .thenReturn(Optional.empty());
    }

    private void givenProductExists() {
        when(productRepository.findById(addFavoriteRequest.productoId()))
                .thenReturn(Optional.of(producto));
    }

    private void givenProductDoesNotExist() {
        when(productRepository.findById(addFavoriteRequest.productoId()))
                .thenReturn(Optional.empty());
    }

    private void givenStudentHasTutorWithPlan(TipoPlan plan) {
        givenStudentHasTutorWithPlanAndChildren(plan, alumno);
    }

    private void givenStudentHasTutorWithPlanAndChildren(TipoPlan plan, Alumno... children) {
        Tutor tutor = tutorWithPlan(plan);
        for (Alumno child : children)
            tutor.addHijo(child);
        when(tutorRepository.findByAlumnoId(addFavoriteRequest.alumnoId()))
                .thenReturn(List.of(tutor));
    }

    private void givenStudentHasNoTutors() {
        when(tutorRepository.findByAlumnoId(addFavoriteRequest.alumnoId()))
                .thenReturn(List.of());
    }

    private void whenAddFavoriteProduct() {
        addFavoriteProduct.execute(addFavoriteRequest);
    }

    private <T extends Throwable> void whenAddFavoriteProductFails(Class<T> expectedException) {
        Executable execution = () -> addFavoriteProduct.execute(addFavoriteRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenProductWasAddedAsFavorite() {
        assertTrue(alumno.getProductosFavoritos().contains(producto));
    }

    private void thenAlumnoHasFavoriteProductCount(int expectedCount) {
        assertEquals(expectedCount, alumno.getProductosFavoritos().size());
    }

    private void thenAlumnoWasSaved() {
        verify(alumnoRepository).save(alumno);
    }

    private void thenAlumnoWasNotSaved() {
        verify(alumnoRepository, never()).save(any());
    }

    private void thenProductWasNotSearched() {
        verify(productRepository, never()).findById(any());
    }

    private void thenTutorsWereNotSearched() {
        verify(tutorRepository, never()).findByAlumnoId(any());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private Alumno alumno(UUID alumnoId) {
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        return alumno;
    }

    private Alumno alumnoWithFavoriteProducts(UUID alumnoId, int count) {
        Alumno alumno = alumno(alumnoId);
        for (int i = 0; i < count; i++)
            alumno.agregarProductoFavorito(producto(randomUUID()));
        return alumno;
    }

    private Producto producto(UUID productoId) {
        Producto producto = new Producto();
        producto.setId(productoId);
        return producto;
    }

    private Tutor tutorWithPlan(TipoPlan plan) {
        Usuario usuario = new Usuario();
        usuario.setPlan(plan);
        Tutor tutor = new Tutor();
        tutor.setId(randomUUID());
        tutor.setUsuario(usuario);
        return tutor;
    }

    private AddFavoriteProductRequest request(UUID alumnoId, UUID productoId) {
        return new AddFavoriteProductRequest(alumnoId, productoId);
    }
}
