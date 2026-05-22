package com.recred.backend.control_parental.application.ports.out;

import com.recred.backend.control_parental.domain.model.ClasificacionSalud;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ClasificacionSaludPort {
    Set<ClasificacionSalud> buscarPorIds(List<UUID> ids);
}