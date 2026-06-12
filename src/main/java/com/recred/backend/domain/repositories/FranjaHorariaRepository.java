package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.FranjaHoraria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FranjaHorariaRepository {
    List<FranjaHoraria> findAllByColegioId(UUID colegioID);
    FranjaHoraria save(FranjaHoraria franjaHoraria);
    Optional<FranjaHoraria> findById(UUID id);
}
