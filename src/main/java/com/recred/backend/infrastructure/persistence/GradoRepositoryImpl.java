package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.infrastructure.entities.GradoEntity;
import org.springframework.stereotype.Component;

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
}
