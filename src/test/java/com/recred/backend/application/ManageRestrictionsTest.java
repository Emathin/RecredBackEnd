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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageRestrictionsTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private HealthClassificationRepository clasificacionRepository;

    @InjectMocks
    private ManageRestrictions useCase;

    private UUID alumnoId;
    private UUID clasificacionId;
    private Student student;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        clasificacionId = UUID.randomUUID();
        student = new Student();
        student.setId(alumnoId);
    }

    @Test
    void ejecutar_ConDatosValidos_DeberiaReemplazarYGuardar() {
        HealthClassification clasificacion = new HealthClassification();
        clasificacion.setId(clasificacionId);
        List<UUID> nuevasClasificacionesIds = List.of(clasificacionId);
        List<HealthClassification> clasificacionesMock = List.of(clasificacion);

        when(studentRepository.findById(alumnoId)).thenReturn(Optional.of(student));
        when(clasificacionRepository.findAllById(nuevasClasificacionesIds)).thenReturn(clasificacionesMock);

        useCase.execute(alumnoId, nuevasClasificacionesIds);

        assertTrue(student.getHealthRestrictions().contains(clasificacion));
        assertEquals(1, student.getHealthRestrictions().size());
        verify(studentRepository).save(student);
    }

    @Test
    void ejecutar_CuandoAlumnoNoExiste_DeberiaLanzarExcepcion() {
        List<UUID> nuevasClasificacionesIds = List.of(clasificacionId);
        when(studentRepository.findById(alumnoId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(alumnoId, nuevasClasificacionesIds);
        });

        assertEquals("El alumno con ID " + alumnoId + " no existe.", exception.getMessage());
        verify(clasificacionRepository, never()).findAllById(any());
        verify(studentRepository, never()).save(any());
    }
}