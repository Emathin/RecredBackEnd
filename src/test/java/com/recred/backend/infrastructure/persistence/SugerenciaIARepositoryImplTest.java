package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SugerenciaIARepositoryImplTest {

    @Mock
    private SpringDataSugerenciaIARepository springDataRepository;

    private SugerenciaIARepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new SugerenciaIARepositoryImpl(springDataRepository);
    }

    @Test
    void delegateSaveToSpringDataRepository() {
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        repository.guardar(entity);
        verify(springDataRepository).save(entity);
    }

    @Test
    void delegateFindByAlumnoIdToSpringDataRepository() {
        UUID alumnoId = UUID.randomUUID();
        List<SugerenciaIAEntity> list = List.of(new SugerenciaIAEntity());
        when(springDataRepository.findAllByAlumnoEntityIdOrderByCreadoEnDesc(alumnoId)).thenReturn(list);

        List<SugerenciaIAEntity> result = repository.findByAlumnoId(alumnoId);

        assertEquals(list, result);
    }

    @Test
    void delegateFindByIdToSpringDataRepository() {
        UUID id = UUID.randomUUID();
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<SugerenciaIAEntity> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }
}
