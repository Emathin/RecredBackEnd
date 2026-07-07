package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.SuscripcionColegio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SuscripcionColegioRepository {
    Optional<SuscripcionColegio> findActiveByColegioId(UUID colegioId);

    List<SuscripcionColegio> findByColegioId(UUID colegioId);

    SuscripcionColegio save(SuscripcionColegio subscription);
}
