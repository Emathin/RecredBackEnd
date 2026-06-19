package com.recred.backend.integration.persistence;

import com.recred.backend.infrastructure.persistence.*;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuffetRepositoryImplTest {

    @Mock
    private SpringDataBuffetRepository springDataBuffetRepository;

    private BuffetRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new BuffetRepositoryImpl(springDataBuffetRepository);
    }

    @Test
    void findById_WhenBuffetExists_ShouldMapToDomain() {
        UUID buffetId = UUID.randomUUID();
        BuffetEntity entity = buffetEntity(buffetId);
        when(springDataBuffetRepository.findById(buffetId)).thenReturn(Optional.of(entity));

        Optional<Buffet> result = repository.findById(buffetId);

        assertTrue(result.isPresent());
        assertEquals(buffetId, result.get().getId());
        assertEquals("Buffet central", result.get().getNombre());
    }

    @Test
    void findByAlumnoId_WhenBuffetExists_ShouldMapToDomain() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        BuffetEntity entity = buffetEntity(buffetId);
        when(springDataBuffetRepository.findByAlumnoId(studentId))
                .thenReturn(Optional.of(entity));

        Optional<Buffet> result = repository.findByAlumnoId(studentId);

        assertTrue(result.isPresent());
        assertEquals(buffetId, result.get().getId());
        assertEquals("Buffet central", result.get().getNombre());
    }

    private BuffetEntity buffetEntity(UUID id) {
        BuffetEntity entity = new BuffetEntity();
        entity.setId(id);
        entity.setNombre("Buffet central");
        entity.setActivo(true);
        return entity;
    }
}
