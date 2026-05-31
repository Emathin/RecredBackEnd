package com.recred.backend.application;

import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.HealthClassificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddHealthRestrictionTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private HealthClassificationRepository clasificacionRepository;

    @InjectMocks
    private AddHealthRestriction useCase;

    private UUID alumnoId;
    private UUID clasificacionId;
    private Student student;
    private HealthClassification clasificacion;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        clasificacionId = UUID.randomUUID();

        student = new Student();
        student.setId(alumnoId);

        clasificacion = new HealthClassification();
        clasificacion.setId(clasificacionId);
    }

    @Test
    void ejecutar_ConDatosValidos_DeberiaAgregarYGuardar() {
        when(studentRepository.findById(alumnoId)).thenReturn(Optional.of(student));
        when(clasificacionRepository.findById(clasificacionId)).thenReturn(Optional.of(clasificacion));

        useCase.execute(alumnoId, clasificacionId);

        assertTrue(student.getHealthRestrictions().contains(clasificacion));
        verify(studentRepository).save(student);
    }

    @Test
    void ejecutar_CuandoAlumnoNoExiste_DeberiaLanzarExcepcion() {
        when(studentRepository.findById(alumnoId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(alumnoId, clasificacionId);
        });

        assertEquals("El alumno no existe.", exception.getMessage());
        verify(clasificacionRepository, never()).findById(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void ejecutar_CuandoClasificacionNoExiste_DeberiaLanzarExcepcion() {
        when(studentRepository.findById(alumnoId)).thenReturn(Optional.of(student));
        when(clasificacionRepository.findById(clasificacionId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(alumnoId, clasificacionId);
        });

        assertEquals("La clasificación médica no existe.", exception.getMessage());
        verify(studentRepository, never()).save(any());
    }
}