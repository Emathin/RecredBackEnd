package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.FranjaHorariaEntity;
import com.recred.backend.infrastructure.entities.RestriccionHorariaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestriccionHorariaRepositoryImplTest {

    @Mock
    private SpringDataRestriccionHorariaRepository springDataRepository;

    private RestriccionHorariaRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new RestriccionHorariaRepositoryImpl(springDataRepository);
    }

    @Test
    void saveShouldCreateEntityWithAllReferences() {
        RestriccionHoraria domain = restriction(true, true, true, true);
        when(springDataRepository.save(any(RestriccionHorariaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RestriccionHoraria result = repository.save(domain);

        ArgumentCaptor<RestriccionHorariaEntity> captor = ArgumentCaptor.forClass(
                RestriccionHorariaEntity.class
        );
        verify(springDataRepository).save(captor.capture());
        RestriccionHorariaEntity saved = captor.getValue();
        assertEquals(domain.getId(), saved.getId());
        assertEquals(domain.getAlumnoId(), saved.getAlumnoEntity().getId());
        assertEquals(domain.getFranjaHoraria().getId(), saved.getFranjaHorariaEntity().getId());
        assertEquals(domain.getCategoria().getId(), saved.getCategoria().getId());
        assertEquals(
                domain.getClasificacionSalud().getId(),
                saved.getClasificacionSaludEntity().getId()
        );
        assertEquals(domain.getCreadoPorUsuarioId(), saved.getCreadoPorUsuarioEntity().getId());
        assertEquals(domain.getId(), result.getId());
    }

    @Test
    void saveShouldUpdateExistingEntityAndClearOptionalReferences() {
        RestriccionHoraria domain = restriction(false, false, false, false);
        RestriccionHorariaEntity existing = entity(domain.getId());
        when(springDataRepository.findById(domain.getId())).thenReturn(Optional.of(existing));
        when(springDataRepository.save(existing)).thenReturn(existing);

        RestriccionHoraria result = repository.save(domain);

        assertEquals(false, existing.getActiva());
        assertNull(existing.getCategoria());
        assertNull(existing.getClasificacionSaludEntity());
        assertNotNull(existing.getCreadoPorUsuarioEntity());
        assertEquals(domain.getId(), result.getId());
        verify(springDataRepository).save(existing);
    }

    @Test
    void findByIdShouldMapEntity() {
        UUID id = UUID.randomUUID();
        RestriccionHorariaEntity entity = entity(id);
        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<RestriccionHoraria> result = repository.findById(id);

        assertEquals(id, result.orElseThrow().getId());
    }

    @Test
    void findAllByAlumnoIdAndActivaShouldMapEntities() {
        UUID alumnoId = UUID.randomUUID();
        RestriccionHorariaEntity entity = entity(UUID.randomUUID());
        when(springDataRepository.findAllByAlumnoEntityIdAndActiva(alumnoId, true))
                .thenReturn(List.of(entity));

        List<RestriccionHoraria> result = repository.findAllByAlumnoIdAndActiva(
                alumnoId,
                true
        );

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
    }

    @Test
    void findAllByAlumnoEntityIdShouldMapEntities() {
        UUID alumnoId = UUID.randomUUID();
        RestriccionHorariaEntity entity = entity(UUID.randomUUID());
        when(springDataRepository.findAllByAlumnoEntityId(alumnoId)).thenReturn(List.of(entity));

        List<RestriccionHoraria> result = repository.findAllByAlumnoEntityId(alumnoId);

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
    }

    @Test
    void deleteByIdShouldDelegateToSpringDataRepository() {
        UUID id = UUID.randomUUID();

        repository.deleteById(id);

        verify(springDataRepository).deleteById(id);
    }

    private RestriccionHoraria restriction(
            boolean withCategory,
            boolean withHealth,
            boolean withUser,
            boolean active
    ) {
        RestriccionHoraria restriction = new RestriccionHoraria();
        restriction.setId(UUID.randomUUID());
        restriction.setAlumnoId(UUID.randomUUID());
        restriction.setFranjaHoraria(timeSlot());
        restriction.setActiva(active);
        if (withCategory) {
            CategoriaProducto category = new CategoriaProducto();
            category.setId(UUID.randomUUID());
            restriction.setCategoria(category);
        }
        if (withHealth) {
            ClasificacionSalud health = new ClasificacionSalud();
            health.setId(UUID.randomUUID());
            restriction.setClasificacionSalud(health);
        }
        if (withUser) {
            restriction.setCreadoPorUsuarioId(UUID.randomUUID());
        }
        return restriction;
    }

    private FranjaHoraria timeSlot() {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(UUID.randomUUID());
        return timeSlot;
    }

    private RestriccionHorariaEntity entity(UUID id) {
        AlumnoEntity alumno = new AlumnoEntity();
        alumno.setId(UUID.randomUUID());
        FranjaHorariaEntity timeSlot = new FranjaHorariaEntity();
        timeSlot.setId(UUID.randomUUID());
        timeSlot.setDescripcion("Primer recreo");
        timeSlot.setHoraInicio(LocalTime.of(10, 0));
        timeSlot.setHoraFin(LocalTime.of(10, 15));
        timeSlot.setActivo(true);

        RestriccionHorariaEntity entity = new RestriccionHorariaEntity();
        entity.setId(id);
        entity.setAlumnoEntity(alumno);
        entity.setFranjaHorariaEntity(timeSlot);
        entity.setActiva(true);
        return entity;
    }
}
