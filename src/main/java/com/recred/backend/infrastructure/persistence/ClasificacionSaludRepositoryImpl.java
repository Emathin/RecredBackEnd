package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ClasificacionSaludRepositoryImpl implements ClasificacionSaludRepository {

    private final SpringDataClasificacionSaludRepository springDataRepository;

    public ClasificacionSaludRepositoryImpl(
            SpringDataClasificacionSaludRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<ClasificacionSalud> findAllById(List<UUID> ids) {
        List<ClasificacionSaludEntity> entidades = springDataRepository.findAllById(ids);

        return entidades.stream()
                .map(ClasificacionSaludEntity::toDomain)
                .collect(Collectors.toList());
    }
}