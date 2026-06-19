package com.recred.backend.unit.application;

import com.recred.backend.application.RemoveFavoriteProduct;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveFavoriteProductTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private RemoveFavoriteProduct removeFavoriteProduct;

    @Test
    void executeWithValidDataShouldRemoveFavoriteAndSave() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Alumno student = new Alumno();
        student.setId(studentId);
        Producto product = new Producto();
        product.setId(productId);
        student.addFavoriteProduct(product);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));

        removeFavoriteProduct.execute(studentId, productId);

        assertFalse(student.getProductosFavoritos().contains(product));
        verify(alumnoRepository, times(1)).save(student);
    }

    @Test
    void executeWhenStudentDoesNotExistShouldThrowException() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                removeFavoriteProduct.execute(studentId, productId)
        );

        verify(alumnoRepository, never()).save(any());
    }
}
