package com.recred.backend.unit.application;

import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveHealthRestrictionsTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private RemoveHealthRestrictions useCase;

    private UUID studentId;
    private UUID classificationId;
    private Alumno student;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        classificationId = UUID.randomUUID();

        student = mock(Alumno.class);
    }

    @Test
    void executeWithValidDataShouldRemoveAndSave() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));

        useCase.execute(studentId, classificationId);

        verify(student).removeHealthRestriction(classificationId);
        verify(alumnoRepository).save(student);
    }

    @Test
    void executeWhenStudentDoesNotExistShouldThrowException() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(studentId, classificationId);
        });

        assertEquals("El alumno no existe.", exception.getMessage());
        verify(alumnoRepository, never()).save(any());
    }
}
