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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveHealthRestrictionsTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private RemoveHealthRestrictions useCase;

    private UUID alumnoId;
    private UUID clasificacionId;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        clasificacionId = UUID.randomUUID();

        alumno = mock(Alumno.class);
    }

    @Test
    void ejecutar_ConDatosValidos_DeberiaRemoverYGuardar() {
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        useCase.ejecutar(alumnoId, clasificacionId);

        verify(alumno).removerRestriccionSalud(clasificacionId);
        verify(alumnoRepository).save(alumno);
    }

    @Test
    void ejecutar_CuandoAlumnoNoExiste_DeberiaLanzarExcepcion() {
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.ejecutar(alumnoId, clasificacionId);
        });

        assertEquals("El alumno no existe.", exception.getMessage());
        verify(alumnoRepository, never()).save(any());
    }
}
