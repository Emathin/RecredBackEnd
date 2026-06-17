package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.repositories.ClasificacionTipoRepository;
import com.recred.backend.infrastructure.entities.ClasificacionTipoEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ClasificacionTipoRepositoryImpl implements ClasificacionTipoRepository {

    private final SpringDataClasificacionTipoRepository springDataRepository;

    public ClasificacionTipoRepositoryImpl(SpringDataClasificacionTipoRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<ClasificacionTipo> findAllById(List<UUID> ids) {
        return springDataRepository.findAllById(ids).stream()
                .map(ClasificacionTipoEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ClasificacionTipo> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(ClasificacionTipoEntity::toDomain);
    }

    @Override
    public List<ClasificacionTipo> findAll() {
        return springDataRepository.findAll().stream()
                .map(ClasificacionTipoEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ClasificacionTipo> findByDescripcion(String descripcion) {
        return springDataRepository.findByDescripcion(descripcion)
                .map(ClasificacionTipoEntity::toDomain);
    }
}
