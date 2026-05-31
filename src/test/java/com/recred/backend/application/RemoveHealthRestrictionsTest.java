package com.recred.backend.application;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.repositories.StudentRepository;
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
class RemoveHealthRestrictionsTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private RemoveHealthRestrictions useCase;

    private UUID alumnoId;
    private UUID clasificacionId;
    private Student student;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        clasificacionId = UUID.randomUUID();

        student = mock(Student.class);
    }

    @Test
    void ejecutar_ConDatosValidos_DeberiaRemoverYGuardar() {
        when(studentRepository.findById(alumnoId)).thenReturn(Optional.of(student));

        useCase.ejecutar(alumnoId, clasificacionId);

        verify(student).removeHealthRestriction(clasificacionId);
        verify(studentRepository).save(student);
    }

    @Test
    void ejecutar_CuandoAlumnoNoExiste_DeberiaLanzarExcepcion() {
        when(studentRepository.findById(alumnoId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.ejecutar(alumnoId, clasificacionId);
        });

        assertEquals("El alumno no existe.", exception.getMessage());
        verify(studentRepository, never()).save(any());
    }
}