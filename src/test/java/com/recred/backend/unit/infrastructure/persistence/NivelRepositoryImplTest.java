package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.Nivel;
import com.recred.backend.infrastructure.entities.NivelEntity;
import com.recred.backend.infrastructure.persistence.NivelRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataNivelRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NivelRepositoryImplTest {

    private final SpringDataNivelRepository springDataRepository =
            mock(SpringDataNivelRepository.class);
    private final NivelRepositoryImpl repository = new NivelRepositoryImpl(springDataRepository);

    @Test
    void findAllActive_ShouldMapEntitiesToDomain() {
        UUID levelId = UUID.randomUUID();
        givenActiveEntities(entity(levelId, "Inicial", true));

        List<Nivel> result = whenFindingAllActive();

        assertEquals(1, result.size());
        assertEquals(levelId, result.get(0).getId());
        assertEquals("Inicial", result.get(0).getDescripcion());
        verify(springDataRepository).findByActivoTrueOrderByDescripcionAsc();
    }

    @Test
    void findActiveById_WhenEntityExists_ShouldReturnDomain() {
        UUID levelId = UUID.randomUUID();
        givenActiveEntity(levelId, entity(levelId, "Primario", true));

        Optional<Nivel> result = whenFindingActiveById(levelId);

        assertTrue(result.isPresent());
        assertEquals("Primario", result.get().getDescripcion());
        verify(springDataRepository).findByIdAndActivoTrue(levelId);
    }

    @Test
    void save_ShouldPersistEntityAndMapResult() {
        UUID levelId = UUID.randomUUID();
        Nivel level = level("Secundario");
        givenSavedEntity(entity(levelId, "Secundario", true));

        Nivel result = whenSaving(level);

        assertEquals(levelId, result.getId());
        assertEquals("Secundario", result.getDescripcion());
    }

    private void givenActiveEntities(NivelEntity... levels) {
        when(springDataRepository.findByActivoTrueOrderByDescripcionAsc())
                .thenReturn(List.of(levels));
    }

    private void givenActiveEntity(UUID levelId, NivelEntity level) {
        when(springDataRepository.findByIdAndActivoTrue(levelId))
                .thenReturn(Optional.of(level));
    }

    private void givenSavedEntity(NivelEntity level) {
        when(springDataRepository.save(org.mockito.ArgumentMatchers.any(NivelEntity.class)))
                .thenReturn(level);
    }

    private List<Nivel> whenFindingAllActive() {
        return repository.findAllActive();
    }

    private Optional<Nivel> whenFindingActiveById(UUID levelId) {
        return repository.findActiveById(levelId);
    }

    private Nivel whenSaving(Nivel level) {
        return repository.save(level);
    }

    private NivelEntity entity(UUID id, String descripcion, boolean activo) {
        NivelEntity entity = new NivelEntity();
        entity.setId(id);
        entity.setDescripcion(descripcion);
        entity.setActivo(activo);
        return entity;
    }

    private Nivel level(String descripcion) {
        Nivel level = new Nivel();
        level.setDescripcion(descripcion);
        level.setActivo(true);
        return level;
    }
}
