package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.infrastructure.entities.GradoEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class GradoRepositoryImpl implements GradoRepository {

    private final SpringDataGradoRepository springDataGradoRepository;

    public GradoRepositoryImpl(SpringDataGradoRepository springDataGradoRepository) {
        this.springDataGradoRepository = springDataGradoRepository;
    }

    @Override
    public Optional<Grado> findById(UUID id) {
        return springDataGradoRepository.findById(id)
                .map(GradoEntity::toDomain);
    }

    @Override
    public List<Grado> findAllActiveByColegioId(UUID colegioId) {
        return springDataGradoRepository
                .findByColegioEntityIdAndActivoTrueOrderByAnioAscDivisionAsc(colegioId)
                .stream()
                .map(GradoEntity::toDomain)
                .toList();
    }
}
