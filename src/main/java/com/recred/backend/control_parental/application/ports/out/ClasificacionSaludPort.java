package com.recred.backend.control_parental.application.ports.out;

import com.recred.backend.catalogo.infrastructure.entities.ClasificacionSaludEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ClasificacionSaludPort {
    Set<ClasificacionSaludEntity> buscarPorIds(List<UUID> ids);
}