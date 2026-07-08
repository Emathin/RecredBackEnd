package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.FranjaHoraria;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FranjaHorariaRepository {
    List<FranjaHoraria> findAllByColegioId(UUID colegioID);

    List<FranjaHoraria> findAllByColegioIdIncludingInactive(UUID colegioId);

    Optional<FranjaHoraria> findById(UUID id);

    Optional<FranjaHoraria> findByIdAndColegioId(UUID id, UUID colegioId);

    boolean existsActiveOverlap(UUID colegioId, UUID excludedId,
                                LocalTime horaInicio, LocalTime horaFin);

    FranjaHoraria save(FranjaHoraria franjaHoraria);
}
