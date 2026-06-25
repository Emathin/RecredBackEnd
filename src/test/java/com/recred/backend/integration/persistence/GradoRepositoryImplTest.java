package com.recred.backend.integration.persistence;

import com.recred.backend.infrastructure.persistence.*;

import com.recred.backend.domain.models.Grado;
import com.recred.backend.infrastructure.entities.GradoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradoRepositoryImplTest {

    @Mock
    private SpringDataGradoRepository springDataGradoRepository;

    private GradoRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new GradoRepositoryImpl(springDataGradoRepository);
    }

    @Test
    void findById_WhenGradeExists_ShouldMapToDomain() {
        UUID gradeId = UUID.randomUUID();
        GradoEntity entity = gradeEntity(gradeId, "1", "A");
        when(springDataGradoRepository.findById(gradeId)).thenReturn(Optional.of(entity));

        Optional<Grado> result = repository.findById(gradeId);

        assertTrue(result.isPresent());
        assertEquals(gradeId, result.get().getId());
        assertEquals("1", result.get().getAnio());
        assertEquals("A", result.get().getDivision());
    }

    @Test
    void findById_WhenGradeDoesNotExist_ShouldReturnEmptyOptional() {
        UUID gradeId = UUID.randomUUID();
        when(springDataGradoRepository.findById(gradeId)).thenReturn(Optional.empty());

        Optional<Grado> result = repository.findById(gradeId);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllActiveByColegioId_ShouldMapActiveGradesToDomain() {
        UUID schoolId = UUID.randomUUID();
        GradoEntity first = gradeEntity(UUID.randomUUID(), "1", "A");
        GradoEntity second = gradeEntity(UUID.randomUUID(), "2", "B");
        when(springDataGradoRepository
                .findByColegioEntityIdAndActivoTrueOrderByAnioAscDivisionAsc(schoolId))
                .thenReturn(List.of(first, second));

        List<Grado> result = repository.findAllActiveByColegioId(schoolId);

        assertEquals(2, result.size());
        assertEquals(first.getId(), result.get(0).getId());
        assertEquals(second.getId(), result.get(1).getId());
    }

    private GradoEntity gradeEntity(UUID id, String year, String division) {
        GradoEntity entity = new GradoEntity();
        entity.setId(id);
        entity.setAnio(year);
        entity.setDivision(division);
        entity.setActivo(true);
        return entity;
    }
}
