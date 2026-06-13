package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Colegio;

import java.util.List;
import java.util.UUID;

public interface ColegioRepository {
    List<Colegio> findAllActive();
    boolean existsActiveById(UUID id);
}
