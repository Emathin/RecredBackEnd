package com.recred.backend.application;

import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.repositories.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetHealthRestrictionsTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private GetHealthRestrictions useCase;

    private UUID alumnoId;
    private Student student;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        student = new Student();
        student.setId(alumnoId);
    }

    @Test
    void ejecutar_CuandoAlumnoExiste_DeberiaRetornarRestricciones() {
        HealthClassification clasificacion = new HealthClassification();
        clasificacion.setId(UUID.randomUUID());
        student.getHealthRestrictions().add(clasificacion);

        when(studentRepository.findById(alumnoId)).thenReturn(Optional.of(student));

        Set<HealthClassification> resultado = useCase.ejecutar(alumnoId);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertTrue(resultado.contains(clasificacion));
    }

    @Test
    void ejecutar_CuandoAlumnoNoExiste_DeberiaLanzarExcepcion() {
        when(studentRepository.findById(alumnoId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.ejecutar(alumnoId);
        });

        assertEquals("El alumno con ID " + alumnoId + " no existe.", exception.getMessage());
    }
}