package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Buffet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuffetRepository {
    Optional<Buffet> findById(UUID id);
    Optional<Buffet> findByAlumnoId(UUID alumnoId);
    List<Buffet> findByColegioId(UUID colegioId);

    Buffet save(Buffet buffet);
}

