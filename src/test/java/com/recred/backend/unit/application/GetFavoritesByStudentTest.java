package com.recred.backend.unit.application;

import com.recred.backend.application.GetFavoritesByStudent;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetFavoritesByStudentTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private GetFavoritesByStudent getFavoritesByStudent;

    @Test
    void executeWhenStudentExistsShouldReturnFavorites() {
        UUID studentId = UUID.randomUUID();
        Alumno student = new Alumno();
        student.setId(studentId);

        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        student.addFavoriteProduct(product);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));

        Set<Producto> result = getFavoritesByStudent.execute(studentId);

        assertEquals(1, result.size());
        assertTrue(result.contains(product));
        verify(alumnoRepository, times(1)).findById(studentId);
    }

    @Test
    void executeWhenStudentDoesNotExistShouldThrowException() {
        UUID studentId = UUID.randomUUID();
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                getFavoritesByStudent.execute(studentId)
        );

        verify(alumnoRepository, times(1)).findById(studentId);
    }
}
