package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Grado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradoRepository {
    Optional<Grado> findById(UUID id);
    List<Grado> findAllActiveByColegioId(UUID colegioId);
}
