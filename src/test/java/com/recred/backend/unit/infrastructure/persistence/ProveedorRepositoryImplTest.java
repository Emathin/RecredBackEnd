package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import com.recred.backend.infrastructure.entities.ProveedorEntity;
import com.recred.backend.infrastructure.persistence.ProveedorRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataProveedorRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorRepositoryImplTest {

    @Mock
    private SpringDataProveedorRepository springDataRepository;

    @Test
    void saveShouldMapDomainAndReturnSavedProvider() {
        ProveedorRepositoryImpl repository = new ProveedorRepositoryImpl(springDataRepository);
        Proveedor provider = provider(UUID.randomUUID(), UUID.randomUUID(), "Distribuidora Sur");
        ArgumentCaptor<ProveedorEntity> captor =
                ArgumentCaptor.forClass(ProveedorEntity.class);
        when(springDataRepository.save(any(ProveedorEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Proveedor saved = repository.save(provider);

        verify(springDataRepository).save(captor.capture());
        assertEquals(provider.getId(), captor.getValue().getId());
        assertEquals(provider.getKiosqueroId(), captor.getValue().getKiosquero().getId());
        assertEquals("Distribuidora Sur", saved.getNombre());
        assertEquals(provider.getKiosqueroId(), saved.getKiosqueroId());
    }

    @Test
    void findMethodsShouldDelegateAndMapEntities() {
        ProveedorRepositoryImpl repository = new ProveedorRepositoryImpl(springDataRepository);
        UUID providerId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();
        ProveedorEntity entity = entity(providerId, kiosqueroId, "Proveedor Uno");
        when(springDataRepository.findById(providerId)).thenReturn(Optional.of(entity));
        when(springDataRepository.findByIdAndKiosqueroId(providerId, kiosqueroId))
                .thenReturn(Optional.of(entity));
        when(springDataRepository.findByKiosqueroId(kiosqueroId))
                .thenReturn(List.of(entity));

        Optional<Proveedor> byId = repository.findById(providerId);
        Optional<Proveedor> byOwner = repository.findByIdAndKiosqueroId(
                providerId,
                kiosqueroId
        );
        List<Proveedor> byKiosquero = repository.findByKiosqueroId(kiosqueroId);

        assertTrue(byId.isPresent());
        assertEquals("Proveedor Uno", byId.orElseThrow().getNombre());
        assertEquals(kiosqueroId, byOwner.orElseThrow().getKiosqueroId());
        assertEquals(1, byKiosquero.size());
        assertEquals(providerId, byKiosquero.get(0).getId());
    }

    @Test
    void deleteAndExistsShouldDelegateToSpringData() {
        ProveedorRepositoryImpl repository = new ProveedorRepositoryImpl(springDataRepository);
        UUID providerId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();
        Proveedor provider = provider(providerId, kiosqueroId, "Proveedor");
        when(springDataRepository.existsByIdAndKiosqueroId(providerId, kiosqueroId))
                .thenReturn(true);

        repository.delete(provider);
        boolean exists = repository.existsByIdAndKiosqueroId(providerId, kiosqueroId);

        assertEquals(true, exists);
        verify(springDataRepository).delete(any(ProveedorEntity.class));
        verify(springDataRepository).existsByIdAndKiosqueroId(providerId, kiosqueroId);
    }

    private Proveedor provider(UUID id, UUID kiosqueroId, String name) {
        Proveedor provider = new Proveedor();
        provider.setId(id);
        provider.setKiosqueroId(kiosqueroId);
        provider.setNombre(name);
        provider.setTelefono("1111");
        provider.setEmail("proveedor@test.com");
        provider.setDiasVisita("Lunes");
        provider.setNotas("Entrega temprano");
        return provider;
    }

    private ProveedorEntity entity(UUID id, UUID kiosqueroId, String name) {
        ProveedorEntity entity = new ProveedorEntity();
        entity.setId(id);
        entity.setNombre(name);
        KiosqueroEntity kiosquero = new KiosqueroEntity();
        kiosquero.setId(kiosqueroId);
        entity.setKiosquero(kiosquero);
        return entity;
    }
}
