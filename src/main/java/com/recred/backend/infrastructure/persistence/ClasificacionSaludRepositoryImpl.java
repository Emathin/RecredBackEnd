package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
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

    @Override
    public Optional<ClasificacionSalud> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(ClasificacionSaludEntity::toDomain);
    }

    @Override
    public List<ClasificacionSalud> findAll() {
        return springDataRepository.findAll().stream()
                .map(ClasificacionSaludEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ClasificacionSalud> findByDescripcion(String descripcion) {
        return springDataRepository.findByDescripcion(descripcion)
                .map(ClasificacionSaludEntity::toDomain);
    }

    @Override
    public ClasificacionSalud save(ClasificacionSalud clasificacion) {
        ClasificacionSaludEntity entity = toEntity(clasificacion);
        return springDataRepository.save(entity).toDomain();
    }

    private ClasificacionSaludEntity toEntity(ClasificacionSalud modelo) {
        ClasificacionSaludEntity entity = new ClasificacionSaludEntity();

        entity.setId(modelo.getId());
        entity.setDescripcion(modelo.getDescripcion());
        entity.setActivo(modelo.getActivo());

        return entity;
    }
}