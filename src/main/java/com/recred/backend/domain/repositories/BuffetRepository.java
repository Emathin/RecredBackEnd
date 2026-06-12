package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Buffet;
import java.util.Optional;
import java.util.UUID;

public interface BuffetRepository {
    Optional<Buffet> findByAlumnoId(UUID alumnoId);
}
