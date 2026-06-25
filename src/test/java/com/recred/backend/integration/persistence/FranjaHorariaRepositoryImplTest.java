package com.recred.backend.integration.persistence;

import com.recred.backend.infrastructure.persistence.*;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.FranjaHorariaEntity;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FranjaHorariaRepositoryImplTest {

    @Mock
    private SpringDataFranjaHorariaRepository springDataRepository;

    private FranjaHorariaRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new FranjaHorariaRepositoryImpl(springDataRepository);
    }

    @Test
    void saveShouldCreateEntityFromDomainModel() {
        FranjaHoraria domain = timeSlot(UUID.randomUUID());
        when(springDataRepository.save(any(FranjaHorariaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FranjaHoraria result = repository.save(domain);

        ArgumentCaptor<FranjaHorariaEntity> captor = ArgumentCaptor.forClass(
                FranjaHorariaEntity.class
        );
        verify(springDataRepository).save(captor.capture());
        FranjaHorariaEntity saved = captor.getValue();
        assertEquals(domain.getId(), saved.getId());
        assertEquals(domain.getColegioId(), saved.getColegioEntity().getId());
        assertEquals("Primer recreo", saved.getDescripcion());
        assertEquals(LocalTime.of(10, 0), saved.getHoraInicio());
        assertEquals(LocalTime.of(10, 15), saved.getHoraFin());
        assertEquals(true, saved.getActivo());
        assertEquals(30, saved.getCupoMaximo());
        assertEquals(0, saved.getMinutosCorte());
        assertEquals(domain.getId(), result.getId());
    }

    @Test
    void saveShouldCreateEntityWhenDomainHasNoId() {
        FranjaHoraria domain = timeSlot(null);
        when(springDataRepository.save(any(FranjaHorariaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FranjaHoraria result = repository.save(domain);

        ArgumentCaptor<FranjaHorariaEntity> captor = ArgumentCaptor.forClass(
                FranjaHorariaEntity.class
        );
        verify(springDataRepository).save(captor.capture());
        assertEquals("Primer recreo", captor.getValue().getDescripcion());
        assertEquals(domain.getColegioId(), captor.getValue().getColegioEntity().getId());
        assertEquals(null, result.getId());
    }

    @Test
    void saveShouldUpdateExistingEntity() {
        UUID id = UUID.randomUUID();
        FranjaHoraria domain = timeSlot(id);
        FranjaHorariaEntity existing = entity(id);
        when(springDataRepository.findById(id)).thenReturn(Optional.of(existing));
        when(springDataRepository.save(existing)).thenReturn(existing);

        FranjaHoraria result = repository.save(domain);

        assertEquals("Primer recreo", existing.getDescripcion());
        assertEquals(domain.getColegioId(), existing.getColegioEntity().getId());
        assertEquals(id, result.getId());
        verify(springDataRepository).save(existing);
    }

    @Test
    void findByIdShouldMapEntityWhenPresent() {
        UUID id = UUID.randomUUID();
        FranjaHorariaEntity entity = entity(id);
        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<FranjaHoraria> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.orElseThrow().getId());
    }

    @Test
    void findAllByColegioIdShouldMapEntities() {
        UUID schoolId = UUID.randomUUID();
        FranjaHorariaEntity entity = entity(UUID.randomUUID());
        when(springDataRepository.findByColegioEntityIdAndActivoTrue(schoolId))
                .thenReturn(List.of(entity));

        List<FranjaHoraria> result = repository.findAllByColegioId(schoolId);

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
    }

    private FranjaHoraria timeSlot(UUID id) {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(id);
        timeSlot.setColegioId(UUID.randomUUID());
        timeSlot.setDescripcion("Primer recreo");
        timeSlot.setHoraInicio(LocalTime.of(10, 0));
        timeSlot.setHoraFin(LocalTime.of(10, 15));
        timeSlot.setActivo(true);
        timeSlot.setCupoMaximo(30);
        timeSlot.setMinutosCorte(-5);
        return timeSlot;
    }

    private FranjaHorariaEntity entity(UUID id) {
        ColegioEntity school = new ColegioEntity();
        school.setId(UUID.randomUUID());

        FranjaHorariaEntity entity = new FranjaHorariaEntity();
        entity.setId(id);
        entity.setColegioEntity(school);
        entity.setDescripcion("Recreo");
        entity.setHoraInicio(LocalTime.of(9, 0));
        entity.setHoraFin(LocalTime.of(9, 15));
        entity.setActivo(true);
        entity.setCupoMaximo(20);
        entity.setMinutosCorte(5);
        return entity;
    }
}
