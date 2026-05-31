package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.repositories.HealthClassificationRepository;
import com.recred.backend.infrastructure.entities.HealthClassificationEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class HealthClassificationRepositoryImpl implements HealthClassificationRepository {

    private final SpringDataClasificacionSaludRepository springDataRepository;

    public HealthClassificationRepositoryImpl(
            SpringDataClasificacionSaludRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<HealthClassification> findAllById(List<UUID> ids) {
        List<HealthClassificationEntity> entidades = springDataRepository.findAllById(ids);

        return entidades.stream()
                .map(HealthClassificationEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HealthClassification> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(HealthClassificationEntity::toDomain);
    }

    @Override
    public List<HealthClassification> findAll() {
        return springDataRepository.findAll().stream()
                .map(HealthClassificationEntity::toDomain)
                .collect(Collectors.toList());
    }

    private HealthClassificationEntity toEntity(HealthClassification modelo) {
        HealthClassificationEntity entity = new HealthClassificationEntity();

        entity.setId(modelo.getId());
        entity.setDescription(modelo.getDescription());
        entity.setActive(modelo.getActive());

        return entity;
    }
}