package com.recred.backend.dominio.repositories;

import com.recred.backend.dominio.models.ClasificacionSalud;

import java.util.List;
import java.util.UUID;

public interface ClasificacionSaludRepository {

    List<ClasificacionSalud> findAllById(List<UUID> ids);

}