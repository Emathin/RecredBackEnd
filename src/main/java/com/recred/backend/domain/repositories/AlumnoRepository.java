package com.recred.backend.domain.repositories;

import java.util.UUID;

public interface AlumnoRepository {
    boolean existsById(UUID alumnoId);
}
