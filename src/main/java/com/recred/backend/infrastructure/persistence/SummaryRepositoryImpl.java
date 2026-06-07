package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.infrastructure.entities.SummaryEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SummaryRepositoryImpl implements SummaryRepository {

    private final SpringDataSummaryRepository springDataSummaryRepository;
    private final InfrastructureMapper infrastructureMapper;

    public SummaryRepositoryImpl(
            SpringDataSummaryRepository springDataSummaryRepository,
            InfrastructureMapper infrastructureMapper) {
        this.springDataSummaryRepository = springDataSummaryRepository;
        this.infrastructureMapper = infrastructureMapper;
    }

    @Override
    public Optional<Summary> findLastByUsuarioId(UUID usuarioId) {
        return springDataSummaryRepository
                .findTopByUsuarioIdOrderByFechaHastaDesc(usuarioId)
                .map(this::toDomain);
    }

    private Summary toDomain(SummaryEntity entity) {
        Summary summary = new Summary();
        summary.setId(entity.getId());
        summary.setFechaDesde(entity.getStartDate());
        summary.setFechaHasta(entity.getEndDate());
        summary.setResumen(entity.getSummary());
        return summary;
    }

    @Override
    public Summary save(Summary summary) {
        SummaryEntity entity = new SummaryEntity();
        entity.setStartDate(summary.getFechaDesde());
        entity.setEndDate(summary.getFechaHasta());
        entity.setSummary(summary.getResumen());
        
        entity.setUser(infrastructureMapper.reference(UsuarioEntity.class, summary.getUsuario().getId()));
        
        SummaryEntity saved = springDataSummaryRepository.save(entity);
        summary.setId(saved.getId());
        
        return summary;
    }
}
