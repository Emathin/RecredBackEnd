package com.recred.backend.unit.application;

import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.RemoveHealthRestrictionsRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
        RemoveHealthRestrictionsRequest request = new RemoveHealthRestrictionsRequest(alumnoId, clasificacionId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        useCase.execute(request);

        verify(alumno).removerRestriccionSalud(clasificacionId);
        verify(alumnoRepository).save(alumno);
    }

    @Test
    void ejecutar_CuandoAlumnoNoExiste_DeberiaLanzarExcepcion() {
        RemoveHealthRestrictionsRequest request = new RemoveHealthRestrictionsRequest(alumnoId, clasificacionId);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);

        assertEquals("El alumno no existe.", exception.getMessage());
        verify(alumnoRepository, never()).save(any());
    }
}
