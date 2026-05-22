package com.recred.backend.control_parental.infrastructure.persistence;

import com.recred.backend.catalogo.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.catalogo.infrastructure.repositories.ClasificacionSaludRepository;
import com.recred.backend.control_parental.application.ports.out.ClasificacionSaludPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ClasificacionSaludPersistenceAdapter implements ClasificacionSaludPort {

    private final ClasificacionSaludRepository repository;

    public ClasificacionSaludPersistenceAdapter(ClasificacionSaludRepository repository) {
        this.repository = repository;
    }

    @Override
    public Set<ClasificacionSaludEntity> buscarPorIds(List<UUID> ids) {
        return repository.findAllById(ids).stream().collect(Collectors.toSet());
    }
}