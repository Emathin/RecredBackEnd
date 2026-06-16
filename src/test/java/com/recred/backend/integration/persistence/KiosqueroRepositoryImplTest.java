package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KiosqueroRepositoryImplTest {

    @Mock
    private SpringDataKiosqueroRepository springDataKiosqueroRepository;

    private KiosqueroRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new KiosqueroRepositoryImpl(springDataKiosqueroRepository);
    }

    @Test
    void findBuffetById_ShouldDelegateToSpringDataRepository() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(springDataKiosqueroRepository.findBuffet(kiosqueroId)).thenReturn(buffetId);

        UUID result = repository.findBuffetById(kiosqueroId);

        assertEquals(buffetId, result);
    }

    @Test
    void findBuffetByUsuarioId_ShouldDelegateToSpringDataRepository() {
        UUID usuarioId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(springDataKiosqueroRepository.findBuffetByUsuarioId(usuarioId)).thenReturn(buffetId);

        UUID result = repository.findBuffetByUsuarioId(usuarioId);

        assertEquals(buffetId, result);
    }

    @Test
    void findAllIds_ShouldDelegateToSpringDataRepository() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(springDataKiosqueroRepository.findAllIds()).thenReturn(ids);

        List<UUID> result = repository.findAllIds();

        assertEquals(ids, result);
    }

    @Test
    void findAll_ShouldMapEntitiesToDomain() {
        KiosqueroEntity first = kiosqueroEntity(UUID.randomUUID(), "20-12345678-9");
        KiosqueroEntity second = kiosqueroEntity(UUID.randomUUID(), "20-87654321-9");
        when(springDataKiosqueroRepository.findAll()).thenReturn(List.of(first, second));

        List<Kiosquero> result = repository.findAll();

        assertEquals(2, result.size());
        assertEquals(first.getId(), result.get(0).getId());
        assertEquals(first.getCuit(), result.get(0).getCuit());
        assertEquals(second.getId(), result.get(1).getId());
        assertEquals(second.getCuit(), result.get(1).getCuit());
    }

    private KiosqueroEntity kiosqueroEntity(UUID id, String cuit) {
        KiosqueroEntity entity = new KiosqueroEntity();
        entity.setId(id);
        entity.setCuit(cuit);
        return entity;
    }
}
