package com.recred.backend.unit.application;

import com.recred.backend.application.ManualProductLock;
import com.recred.backend.application.ManualProductLockRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManualProductLockTest {

    private final AlumnoRepository alumnoRepository = mock(AlumnoRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ManualProductLock useCase =
            new ManualProductLock(alumnoRepository, productRepository);
    private UUID studentId;
    private UUID productId;
    private Alumno student;
    private Producto product;
    private RuntimeException exception;

    @Test
    void execute_WhenStudentAndProductExist_BlocksProductAndSavesStudent() {
        givenStudentCanBlockProduct();

        whenLockingProduct();

        thenProductIsBlocked();
        thenStudentWasSaved();
    }

    @Test
    void execute_WhenStudentDoesNotExist_ThrowsException() {
        givenMissingStudent();

        whenLockingProductFails();

        thenExceptionMessageIs("Alumno no encontrado");
        thenProductWasNotSearched();
        thenStudentWasNotSaved();
    }

    @Test
    void execute_WhenProductDoesNotExist_ThrowsException() {
        givenMissingProduct();

        whenLockingProductFails();

        thenExceptionMessageIs("Producto no encontrado");
        thenStudentWasNotSaved();
    }

    private void givenStudentCanBlockProduct() {
        studentId = randomUUID();
        productId = randomUUID();
        student = new Alumno();
        product = product(productId);
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    }

    private void givenMissingStudent() {
        studentId = randomUUID();
        productId = randomUUID();
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void givenMissingProduct() {
        studentId = randomUUID();
        productId = randomUUID();
        student = new Alumno();
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
    }

    private void whenLockingProduct() {
        useCase.execute(defaultRequest());
    }

    private void whenLockingProductFails() {
        Executable execution = () -> useCase.execute(defaultRequest());

        exception = assertThrows(RuntimeException.class, execution);
    }

    private void thenProductIsBlocked() {
        assertTrue(student.hasBlockedProduct(product));
    }

    private void thenStudentWasSaved() {
        verify(alumnoRepository).save(student);
    }

    private void thenExceptionMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void thenProductWasNotSearched() {
        verify(productRepository, never()).findById(any());
    }

    private void thenStudentWasNotSaved() {
        verify(alumnoRepository, never()).save(any());
    }

    private ManualProductLockRequest defaultRequest() {
        return new ManualProductLockRequest(studentId, productId);
    }

    private Producto product(UUID productId) {
        Producto product = new Producto();
        product.setId(productId);
        return product;
    }
}
