package com.recred.backend.unit.application;

import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddHealthRestrictionTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ClasificacionSaludRepository clasificacionRepository;

    @InjectMocks
    private AddHealthRestriction useCase;

    private UUID studentId;
    private UUID classificationId;
    private Alumno student;
    private ClasificacionSalud classification;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        classificationId = UUID.randomUUID();

        student = new Alumno();
        student.setId(studentId);

        classification = new ClasificacionSalud();
        classification.setId(classificationId);
    }

    @Test
    void executeWithValidDataShouldAddAndSave() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(clasificacionRepository.findById(classificationId)).thenReturn(Optional.of(classification));

        useCase.execute(studentId, classificationId);

        assertTrue(student.getRestriccionesSalud().contains(classification));
        verify(alumnoRepository).save(student);
    }

    @Test
    void executeWhenStudentDoesNotExistShouldThrowException() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(studentId, classificationId);
        });

        assertEquals("El alumno no existe.", exception.getMessage());
        verify(clasificacionRepository, never()).findById(any());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void executeWhenClassificationDoesNotExistShouldThrowException() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(clasificacionRepository.findById(classificationId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(studentId, classificationId);
        });

        assertEquals("La clasificación médica no existe.", exception.getMessage());
        verify(alumnoRepository, never()).save(any());
    }
}
