package com.recred.backend.unit.application;

import com.recred.backend.application.BlockProductManually;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockProductManuallyTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BlockProductManually blockProductManually;

    @Test
    void executeWithValidDataShouldBlockAndSave() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Alumno student = new Alumno();
        Producto product = new Producto();
        product.setId(productId);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        blockProductManually.execute(studentId, productId);

        verify(alumnoRepository, times(1)).save(student);
    }

    @Test
    void executeWhenStudentDoesNotExistShouldThrowException() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                blockProductManually.execute(studentId, productId)
        );

        verify(productRepository, never()).findById(any());
        verify(alumnoRepository, never()).save(any());
    }
}
