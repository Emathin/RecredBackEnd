package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Grado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradoRepository {
    Optional<Grado> findById(UUID id);
    Optional<Grado> findByIdAndColegioId(UUID id, UUID colegioId);
    List<Grado> findAllActiveByColegioId(UUID colegioId);
    List<Grado> findAllByColegioIdIncludingInactive(UUID colegioId);
    boolean existsActiveByColegioIdAndNivelIdAndAnioAndDivision(
            UUID colegioId,
            UUID nivelId,
            UUID excludedId,
            String anio,
            String division);
    Grado save(Grado grado);
}
