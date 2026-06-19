package com.recred.backend.unit.application;

import com.recred.backend.application.UnblockProductManually;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
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
class UnblockProductManuallyTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private UnblockProductManually unblockProductManually;

    @Test
    void executeWithValidStudentShouldUnblockAndSave() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Alumno student = new Alumno();

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));

        unblockProductManually.execute(studentId, productId);

        verify(alumnoRepository, times(1)).save(student);
    }

    @Test
    void executeWhenStudentDoesNotExistShouldThrowException() {
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                unblockProductManually.execute(studentId, productId)
        );

        verify(alumnoRepository, never()).save(any());
    }
}
