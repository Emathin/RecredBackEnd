package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateTimeRestriction;
import com.recred.backend.application.UpdateTimeRestrictionRequest;
import com.recred.backend.application.UpdateTimeRestrictionResponse;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTimeRestrictionTest {

    @Mock
    private RestriccionHorariaRepository repository;
    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private UpdateTimeRestriction updateTimeRestriction;

    @Test
    void execute_ActualizaConNuevosFiltros() {
        UUID restrictionId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID nuevoTimeSlotId = UUID.randomUUID();
        UUID nuevaCategoriaId = UUID.randomUUID();
        UUID nuevaClasificacionId = UUID.randomUUID();

        UpdateTimeRestrictionRequest request = new UpdateTimeRestrictionRequest(
                restrictionId, nuevoTimeSlotId, nuevaCategoriaId, nuevaClasificacionId);

        RestriccionHoraria restriccionExistente = new RestriccionHoraria();
        restriccionExistente.setAlumnoId(alumnoId);
        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccionExistente));
        when(repository.save(any(RestriccionHoraria.class))).thenAnswer(i -> i.getArguments()[0]);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(new Alumno()));

        UpdateTimeRestrictionResponse response = updateTimeRestriction.execute(request);

        ArgumentCaptor<RestriccionHoraria> captor = ArgumentCaptor.forClass(RestriccionHoraria.class);
        verify(repository).save(captor.capture());
        RestriccionHoraria guardada = captor.getValue();

        assertEquals(nuevoTimeSlotId, guardada.getFranjaHoraria().getId());
        assertEquals(nuevaCategoriaId, guardada.getCategoria().getId());
        assertEquals(nuevaClasificacionId, guardada.getClasificacionSalud().getId());

        assertNotNull(response);
        assertEquals(nuevoTimeSlotId, response.franjaHoraria().id());
        assertEquals(nuevaCategoriaId, response.categoria().id());
        assertEquals(nuevaClasificacionId, response.clasificacionSalud().id());
    }

    @Test
    void execute_LimpiaFiltros_CuandoCommandEnviaNulls() {
        UUID restrictionId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID nuevoTimeSlotId = UUID.randomUUID();

        UpdateTimeRestrictionRequest request = new UpdateTimeRestrictionRequest(
                restrictionId, nuevoTimeSlotId, null, null);

        RestriccionHoraria restriccionExistente = new RestriccionHoraria();
        restriccionExistente.setAlumnoId(alumnoId);

        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccionExistente));
        when(repository.save(any(RestriccionHoraria.class))).thenAnswer(i -> i.getArguments()[0]);

        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(new Alumno()));

        UpdateTimeRestrictionResponse response = updateTimeRestriction.execute(request);

        ArgumentCaptor<RestriccionHoraria> captor = ArgumentCaptor.forClass(RestriccionHoraria.class);
        verify(repository).save(captor.capture());
        RestriccionHoraria guardada = captor.getValue();

        assertEquals(nuevoTimeSlotId, guardada.getFranjaHoraria().getId());
        assertNull(guardada.getCategoria());
        assertNull(guardada.getClasificacionSalud());

        assertNotNull(response);
        assertEquals(nuevoTimeSlotId, response.franjaHoraria().id());
        assertNull(response.categoria());
        assertNull(response.clasificacionSalud());
    }

    @Test
    void execute_LanzaExcepcion_CuandoNoExiste() {
        UUID restrictionId = UUID.randomUUID();
        UpdateTimeRestrictionRequest request = new UpdateTimeRestrictionRequest(restrictionId, UUID.randomUUID(), null, null);

        when(repository.findById(restrictionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> updateTimeRestriction.execute(request));
    }

    @Test
    void execute_LanzaExcepcion_CuandoAlumnoNoExiste() {
        UUID restrictionId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UpdateTimeRestrictionRequest request = new UpdateTimeRestrictionRequest(
                restrictionId,
                UUID.randomUUID(),
                null,
                null
        );
        RestriccionHoraria restriccionExistente = new RestriccionHoraria();
        restriccionExistente.setAlumnoId(alumnoId);

        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccionExistente));
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> updateTimeRestriction.execute(request)
        );
        verify(repository, never()).save(any());
    }

    @Test
    void execute_LanzaExcepcion_CuandoClasificacionYaEstaAsignadaGlobalmente() {
        UUID restrictionId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID classificationId = UUID.randomUUID();
        UpdateTimeRestrictionRequest request = new UpdateTimeRestrictionRequest(
                restrictionId,
                UUID.randomUUID(),
                null,
                classificationId
        );
        RestriccionHoraria restriccionExistente = new RestriccionHoraria();
        restriccionExistente.setAlumnoId(alumnoId);
        ClasificacionSalud clasificacion = new ClasificacionSalud();
        clasificacion.setId(classificationId);
        Alumno alumno = new Alumno();
        alumno.setRestriccionesSalud(Set.of(clasificacion));

        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccionExistente));
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        assertThrows(
                IllegalArgumentException.class,
                () -> updateTimeRestriction.execute(request)
        );
        verify(repository, never()).save(any());
    }
}
