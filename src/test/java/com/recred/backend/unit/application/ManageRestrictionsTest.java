package com.recred.backend.unit.application;

import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.ManageRestrictionsRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
    private AlumnoRepository alumnoRepository;

    @Mock
    private ClasificacionSaludRepository clasificacionRepository;

    @InjectMocks
    private ManageRestrictions useCase;

    private UUID alumnoId;
    private UUID clasificacionId;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        clasificacionId = UUID.randomUUID();
        alumno = new Alumno();
        alumno.setId(alumnoId);
    }

    @Test
    void ejecutar_ConDatosValidos_DeberiaReemplazarYGuardar() {
        ClasificacionSalud clasificacion = new ClasificacionSalud();
        clasificacion.setId(clasificacionId);
        List<UUID> nuevasClasificacionesIds = List.of(clasificacionId);
        List<ClasificacionSalud> clasificacionesMock = List.of(clasificacion);
        ManageRestrictionsRequest request = new ManageRestrictionsRequest(alumnoId, nuevasClasificacionesIds);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        when(clasificacionRepository.findAllById(nuevasClasificacionesIds)).thenReturn(clasificacionesMock);

        useCase.execute(request);

        assertTrue(alumno.getRestriccionesSalud().contains(clasificacion));
        assertEquals(1, alumno.getRestriccionesSalud().size());
        verify(alumnoRepository).save(alumno);
    }

    @Test
    void ejecutar_CuandoAlumnoNoExiste_DeberiaLanzarExcepcion() {
        List<UUID> nuevasClasificacionesIds = List.of(clasificacionId);
        ManageRestrictionsRequest request = new ManageRestrictionsRequest(alumnoId, nuevasClasificacionesIds);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);

        assertEquals("El alumno con ID " + alumnoId + " no existe.", exception.getMessage());
        verify(clasificacionRepository, never()).findAllById(any());
        verify(alumnoRepository, never()).save(any());
    }
}
