package com.recred.backend.unit.application;

import com.recred.backend.application.DeleteFavoriteProduct;
import com.recred.backend.application.DeleteFavoriteProductRequest;
import com.recred.backend.application.DeleteFavoriteProductResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteFavoriteProductTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    private DeleteFavoriteProduct useCase;

    private UUID studentId;
    private UUID productId;
    private Alumno student;
    private DeleteFavoriteProductResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new DeleteFavoriteProduct(alumnoRepository);
    }

    @Test
    void givenFavoriteProduct_whenDeletingFavoriteProduct_thenRemovesAndSavesStudent() {
        givenStudentWithFavoriteProduct();
        givenStudentExists();

        whenDeletingFavoriteProduct();

        thenFavoriteProductWasRemoved();
        thenStudentWasSaved();
        thenEmptyResponseIsReturned();
    }

    @Test
    void givenMissingStudent_whenDeletingFavoriteProduct_thenFailsAndDoesNotSave() {
        givenFavoriteProductScenario();
        givenStudentDoesNotExist();

        whenDeletingFavoriteProductFails();

        thenDeleteFailsWith("Alumno no encontrado");
        thenStudentWasNotSaved();
    }

    private void givenStudentWithFavoriteProduct() {
        givenFavoriteProductScenario();
        Producto product = new Producto();
        product.setId(productId);
        student.agregarProductoFavorito(product);
    }

    private void givenFavoriteProductScenario() {
        studentId = UUID.randomUUID();
        productId = UUID.randomUUID();
        student = new Alumno();
        student.setId(studentId);
    }

    private void givenStudentExists() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void whenDeletingFavoriteProduct() {
        response = useCase.execute(deleteRequest());
    }

    private void whenDeletingFavoriteProductFails() {
        thrownException = assertThrows(RuntimeException.class, () -> useCase.execute(deleteRequest()));
    }

    private void thenFavoriteProductWasRemoved() {
        assertFalse(student.hasFavoriteProduct(productId));
    }

    private void thenStudentWasSaved() {
        verify(alumnoRepository).save(student);
    }

    private void thenEmptyResponseIsReturned() {
        assertNotNull(response);
    }

    private void thenDeleteFailsWith(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private void thenStudentWasNotSaved() {
        verify(alumnoRepository, never()).save(any());
    }

    private DeleteFavoriteProductRequest deleteRequest() {
        return new DeleteFavoriteProductRequest(studentId, productId);
    }
}
