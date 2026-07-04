package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.infrastructure.entities.ClasificacionTipoEntity;
import com.recred.backend.infrastructure.persistence.ClasificacionTipoRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataClasificacionTipoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClasificacionTipoRepositoryImplTest {

    private final SpringDataClasificacionTipoRepository springDataRepository =
            mock(SpringDataClasificacionTipoRepository.class);
    private final ClasificacionTipoRepositoryImpl repository =
            new ClasificacionTipoRepositoryImpl(springDataRepository);

    @Test
    void findMethodsShouldDelegateAndMapEntities() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        ClasificacionTipoEntity first = entity(firstId, "Organico", true);
        ClasificacionTipoEntity second = entity(secondId, "Industrial", false);
        when(springDataRepository.findAllById(List.of(firstId, secondId)))
                .thenReturn(List.of(first, second));
        when(springDataRepository.findById(firstId)).thenReturn(Optional.of(first));
        when(springDataRepository.findAll()).thenReturn(List.of(first, second));
        when(springDataRepository.findByDescripcion("Organico")).thenReturn(Optional.of(first));

        List<ClasificacionTipo> byIds = repository.findAllById(List.of(firstId, secondId));
        Optional<ClasificacionTipo> byId = repository.findById(firstId);
        List<ClasificacionTipo> all = repository.findAll();
        Optional<ClasificacionTipo> byDescription = repository.findByDescripcion("Organico");

        assertEquals(2, byIds.size());
        assertEquals("Organico", byId.orElseThrow().getDescripcion());
        assertEquals(false, all.get(1).getActivo());
        assertEquals(firstId, byDescription.orElseThrow().getId());
    }

    private ClasificacionTipoEntity entity(UUID id, String description, Boolean active) {
        ClasificacionTipoEntity entity = new ClasificacionTipoEntity();
        entity.setId(id);
        entity.setDescripcion(description);
        entity.setActivo(active);
        return entity;
    }
}
