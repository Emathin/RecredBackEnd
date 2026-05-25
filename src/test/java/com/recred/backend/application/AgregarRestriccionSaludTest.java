package com.recred.backend.application;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgregarRestriccionSaludTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ClasificacionSaludRepository clasificacionRepository;

    @InjectMocks
    private AgregarRestriccionSalud useCase;

    private UUID alumnoId;
    private UUID clasificacionId;
    private Alumno alumno;
    private ClasificacionSalud clasificacion;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        clasificacionId = UUID.randomUUID();

        alumno = new Alumno();
        alumno.setId(alumnoId);

        clasificacion = new ClasificacionSalud();
        clasificacion.setId(clasificacionId);
    }

    @Test
    void ejecutar_ConDatosValidos_DeberiaAgregarYGuardar() {
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(clasificacionRepository.findById(clasificacionId)).thenReturn(Optional.of(clasificacion));

        useCase.ejecutar(alumnoId, clasificacionId);

        assertTrue(alumno.getRestriccionesSalud().contains(clasificacion));
        verify(alumnoRepository).save(alumno);
    }

    @Test
    void ejecutar_CuandoAlumnoNoExiste_DeberiaLanzarExcepcion() {
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.ejecutar(alumnoId, clasificacionId);
        });

        assertEquals("El alumno no existe.", exception.getMessage());
        verify(clasificacionRepository, never()).findById(any());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void ejecutar_CuandoClasificacionNoExiste_DeberiaLanzarExcepcion() {
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(clasificacionRepository.findById(clasificacionId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            useCase.ejecutar(alumnoId, clasificacionId);
        });

        assertEquals("La clasificación médica no existe.", exception.getMessage());
        verify(alumnoRepository, never()).save(any());
    }
}