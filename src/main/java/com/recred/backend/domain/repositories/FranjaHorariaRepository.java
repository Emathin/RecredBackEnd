package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.FranjaHoraria;

import java.util.List;
import java.util.UUID;

public interface FranjaHorariaRepository {
    List<FranjaHoraria> findAllByColegioId(UUID colegioID);
}
