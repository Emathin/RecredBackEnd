package com.recred.backend.control_parental.application.usecases;

import com.recred.backend.control_parental.domain.ports.out.AlumnoPort;
import com.recred.backend.control_parental.domain.ports.out.ClasificacionSaludPort;
import com.recred.backend.control_parental.domain.model.Alumno;
import com.recred.backend.control_parental.domain.model.ClasificacionSalud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ActualizarRestriccionesSaludUseCaseTest {

    private AlumnoPort alumnoPort;
    private ClasificacionSaludPort clasificacionPort;
    private ActualizarRestriccionesSaludUseCase useCase;

    @BeforeEach
    void setUp() {
        alumnoPort = mock(AlumnoPort.class);
        clasificacionPort = mock(ClasificacionSaludPort.class);

        useCase = new ActualizarRestriccionesSaludUseCase(alumnoPort, clasificacionPort);
    }

    @Test
    void ejecutar_DebeActualizarRestricciones_CuandoDatosSonValidos() {
        UUID alumnoId = UUID.randomUUID();
        UUID clasificacionId = UUID.randomUUID();
        List<UUID> nuevosIds = List.of(clasificacionId);

        Alumno alumnoMock = new Alumno(alumnoId, Set.of());
        ClasificacionSalud clasificacionMock = new ClasificacionSalud(clasificacionId, "Test");

        when(alumnoPort.buscarPorId(alumnoId)).thenReturn(Optional.of(alumnoMock));
        when(clasificacionPort.buscarPorIds(nuevosIds)).thenReturn(Set.of(clasificacionMock));

        useCase.ejecutar(alumnoId, nuevosIds);

        assertEquals(1, alumnoMock.getRestriccionesSalud().size());
        verify(alumnoPort, times(1)).guardar(alumnoMock);
    }

    @Test
    void ejecutar_DebeLanzarExcepcion_CuandoAlumnoNoExiste() {
        UUID alumnoId = UUID.randomUUID();
        List<UUID> nuevosIds = List.of(UUID.randomUUID());

        when(alumnoPort.buscarPorId(alumnoId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            useCase.ejecutar(alumnoId, nuevosIds);
        });

        verify(alumnoPort, never()).guardar(any());
    }
}