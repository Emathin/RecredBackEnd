package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Colegio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ColegioRepository {
    Optional<Colegio> findById(UUID id);

    List<Colegio> findAllActive();
    boolean existsActiveById(UUID id);
    boolean existsByEmail(String email);
    boolean existsByCue(String cue);
    Colegio save(Colegio colegio);
}
