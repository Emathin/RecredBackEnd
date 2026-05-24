package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ClasificacionSalud;

import java.util.List;
import java.util.UUID;

public interface ClasificacionSaludRepository {

    List<ClasificacionSalud> findAllById(List<UUID> ids);

}