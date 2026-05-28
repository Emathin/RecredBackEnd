package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.RestriccionHoraria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestriccionHorariaRepository {

    RestriccionHoraria save(RestriccionHoraria restriccion);
    Optional<RestriccionHoraria> findById(UUID id);
    List<RestriccionHoraria> findAllByAlumnoIdAndActiva(UUID alumnoId, boolean activa);
    List<RestriccionHoraria> findAllByAlumnoId(UUID alumnoId);
    void deleteById(UUID id);
}