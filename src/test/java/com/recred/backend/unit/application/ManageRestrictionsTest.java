package com.recred.backend.unit.application;

import com.recred.backend.application.ManageRestrictions;
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

import java.util.List;
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
class ManageRestrictionsTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ClasificacionSaludRepository clasificacionRepository;

    @InjectMocks
    private ManageRestrictions useCase;

    private UUID studentId;
    private UUID classificationId;
    private Alumno student;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        classificationId = UUID.randomUUID();
        student = new Alumno();
        student.setId(studentId);
    }

    @Test
    void executeWithValidDataShouldReplaceAndSave() {
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(classificationId);
        List<UUID> newClassificationIds = List.of(classificationId);
        List<ClasificacionSalud> mockClassifications = List.of(classification);

        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(clasificacionRepository.findAllById(newClassificationIds)).thenReturn(mockClassifications);

        useCase.execute(studentId, newClassificationIds);

        assertTrue(student.getRestriccionesSalud().contains(classification));
        assertEquals(1, student.getRestriccionesSalud().size());
        verify(alumnoRepository).save(student);
    }

    @Test
    void executeWhenStudentDoesNotExistShouldThrowException() {
        List<UUID> newClassificationIds = List.of(classificationId);
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(studentId, newClassificationIds);
        });

        assertEquals("El alumno con ID " + studentId + " no existe.", exception.getMessage());
        verify(clasificacionRepository, never()).findAllById(any());
        verify(alumnoRepository, never()).save(any());
    }
}
