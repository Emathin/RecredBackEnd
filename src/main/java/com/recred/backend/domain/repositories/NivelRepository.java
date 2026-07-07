package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Nivel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NivelRepository {

    List<Nivel> findAllActive();

    Optional<Nivel> findActiveById(UUID id);

    Optional<Nivel> findByDescripcionIgnoreCase(String descripcion);

    Nivel save(Nivel nivel);
}
