package com.recred.backend.dominio;

import com.recred.backend.infraestructura.entities.ClasificacionSaludEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AdministrarRestricciones {
    void ejecutar(UUID alumnoId, List<UUID> nuevasClasificacionesIds);
    Set<ClasificacionSaludEntity> obtener(UUID alumnoId);
}
