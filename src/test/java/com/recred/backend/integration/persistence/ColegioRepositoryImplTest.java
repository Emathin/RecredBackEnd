package com.recred.backend.integration.persistence;

import com.recred.backend.infrastructure.persistence.*;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColegioRepositoryImplTest {

    @Mock
    private SpringDataColegioRepository springDataColegioRepository;

    private ColegioRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ColegioRepositoryImpl(springDataColegioRepository);
    }

    @Test
    void findAllActive_ShouldMapActiveSchoolsToDomain() {
        ColegioEntity first = schoolEntity(UUID.randomUUID(), "Colegio Norte");
        ColegioEntity second = schoolEntity(UUID.randomUUID(), "Colegio Sur");
        when(springDataColegioRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(first, second));

        List<Colegio> result = repository.findAllActive();

        assertEquals(2, result.size());
        assertEquals(first.getId(), result.get(0).getId());
        assertEquals("Colegio Norte", result.get(0).getNombre());
        assertEquals(second.getId(), result.get(1).getId());
        assertEquals("Colegio Sur", result.get(1).getNombre());
    }

    @Test
    void existsActiveById_ShouldDelegateToSpringDataRepository() {
        UUID schoolId = UUID.randomUUID();
        when(springDataColegioRepository.existsByIdAndActivoTrue(schoolId)).thenReturn(true);

        assertTrue(repository.existsActiveById(schoolId));
    }

    private ColegioEntity schoolEntity(UUID id, String name) {
        ColegioEntity entity = new ColegioEntity();
        entity.setId(id);
        entity.setNombre(name);
        entity.setActivo(true);
        return entity;
    }
}
