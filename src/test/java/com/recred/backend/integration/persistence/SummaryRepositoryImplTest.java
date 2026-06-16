package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.SummaryEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryRepositoryImplTest {

    @Mock
    private SpringDataSummaryRepository springDataSummaryRepository;

    @Mock
    private InfrastructureMapper infrastructureMapper;

    private SummaryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new SummaryRepositoryImpl(
                springDataSummaryRepository,
                infrastructureMapper
        );
    }

    @Test
    void findLastByUsuarioIdShouldMapEntity() {
        UUID userId = UUID.randomUUID();
        SummaryEntity entity = entity(UUID.randomUUID());
        when(springDataSummaryRepository.findTopByUsuarioIdOrderByFechaHastaDesc(userId))
                .thenReturn(Optional.of(entity));

        Optional<Summary> result = repository.findLastByUsuarioId(userId);

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.orElseThrow().getId());
        assertEquals(entity.getStartDate(), result.orElseThrow().getFechaDesde());
        assertEquals(entity.getEndDate(), result.orElseThrow().getFechaHasta());
        assertEquals(entity.getSummary(), result.orElseThrow().getResumen());
    }

    @Test
    void findAllByUsuarioIdShouldMapEntities() {
        UUID userId = UUID.randomUUID();
        SummaryEntity entity = entity(UUID.randomUUID());
        when(springDataSummaryRepository.findByUsuarioIdOrderByEndDateDesc(userId))
                .thenReturn(List.of(entity));

        List<Summary> result = repository.findAllByUsuarioId(userId);

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
    }

    @Test
    void saveShouldMapSummaryAndAssignGeneratedId() {
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();
        Usuario user = new Usuario();
        user.setId(userId);
        Summary summary = new Summary();
        summary.setFechaDesde(LocalDateTime.of(2026, 6, 1, 0, 0));
        summary.setFechaHasta(LocalDateTime.of(2026, 6, 15, 23, 59));
        summary.setResumen("{\"mensaje\":\"ok\"}");
        summary.setUsuario(user);
        UsuarioEntity userReference = new UsuarioEntity();
        userReference.setId(userId);
        SummaryEntity savedEntity = entity(summaryId);

        when(infrastructureMapper.reference(UsuarioEntity.class, userId))
                .thenReturn(userReference);
        when(springDataSummaryRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(savedEntity);

        Summary result = repository.save(summary);

        ArgumentCaptor<SummaryEntity> captor = ArgumentCaptor.forClass(SummaryEntity.class);
        verify(springDataSummaryRepository).save(captor.capture());
        SummaryEntity saved = captor.getValue();
        assertEquals(summary.getFechaDesde(), saved.getStartDate());
        assertEquals(summary.getFechaHasta(), saved.getEndDate());
        assertEquals(summary.getResumen(), saved.getSummary());
        assertEquals(userReference, saved.getUser());
        assertEquals(summaryId, result.getId());
    }

    private SummaryEntity entity(UUID id) {
        SummaryEntity entity = new SummaryEntity();
        entity.setId(id);
        entity.setStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        entity.setEndDate(LocalDateTime.of(2026, 6, 15, 23, 59));
        entity.setSummary("{\"mensaje\":\"ok\"}");
        return entity;
    }
}
