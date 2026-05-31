package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.TimeRestriction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeRestrictionRepository {

    TimeRestriction save(TimeRestriction restriccion);
    Optional<TimeRestriction> findById(UUID id);
    List<TimeRestriction> findAllByAlumnoIdAndActiva(UUID alumnoId, boolean activa);
    List<TimeRestriction> findAllByAlumnoId(UUID alumnoId);
    void deleteById(UUID id);
}