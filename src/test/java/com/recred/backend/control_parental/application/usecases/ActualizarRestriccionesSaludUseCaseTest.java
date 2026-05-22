package com.recred.backend.control_parental.application.usecases;

import com.recred.backend.control_parental.application.ports.out.AlumnoPort;
import com.recred.backend.control_parental.application.ports.out.ClasificacionSaludPort;
import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;
import com.recred.backend.catalogo.infrastructure.entities.ClasificacionSaludEntity;
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
        // Mockeamos los puertos de salida (la base de datos)
        alumnoPort = mock(AlumnoPort.class);
        clasificacionPort = mock(ClasificacionSaludPort.class);

        useCase = new ActualizarRestriccionesSaludUseCase(alumnoPort, clasificacionPort);
    }

    @Test
    void ejecutar_DebeActualizarRestricciones_CuandoDatosSonValidos() {
        // Arrange
        UUID alumnoId = UUID.randomUUID();
        UUID clasificacionId = UUID.randomUUID();
        List<UUID> nuevosIds = List.of(clasificacionId);

        AlumnoEntity alumnoMock = new AlumnoEntity();
        alumnoMock.setId(alumnoId);

        ClasificacionSaludEntity clasificacionMock = new ClasificacionSaludEntity();
        clasificacionMock.setId(clasificacionId);

        // Simulamos que la base de datos encuentra al alumno y a la clasificación
        when(alumnoPort.buscarPorId(alumnoId)).thenReturn(Optional.of(alumnoMock));
        when(clasificacionPort.buscarPorIds(nuevosIds)).thenReturn(Set.of(clasificacionMock));

        // Act
        useCase.ejecutar(alumnoId, nuevosIds);

        // Assert
        // Verificamos que al alumno se le asignó la nueva restricción
        assertEquals(1, alumnoMock.getRestriccionesSalud().size());

        // Verificamos que se llamó al puerto para guardar en la base de datos
        verify(alumnoPort, times(1)).guardar(alumnoMock);
    }

    @Test
    void ejecutar_DebeLanzarExcepcion_CuandoAlumnoNoExiste() {
        // Arrange
        UUID alumnoId = UUID.randomUUID();
        List<UUID> nuevosIds = List.of(UUID.randomUUID());

        // Simulamos que el alumno NO existe en la base de datos
        when(alumnoPort.buscarPorId(alumnoId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            useCase.ejecutar(alumnoId, nuevosIds);
        });

        // Verificamos que NUNCA se intente guardar nada
        verify(alumnoPort, never()).guardar(any());
    }
}