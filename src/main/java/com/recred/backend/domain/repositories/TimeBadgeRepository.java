package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.TimeBadge;

import java.util.List;
import java.util.UUID;

public interface TimeBadgeRepository {
    List<TimeBadge> findAllByColegioId(UUID colegioID);
    TimeBadge save(TimeBadge timeBadge);
}
