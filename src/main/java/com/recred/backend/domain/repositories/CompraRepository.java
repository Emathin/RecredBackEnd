package com.recred.backend.domain.repositories;

import java.util.List;
import java.util.UUID;

public interface CompraRepository {
    List<PreferenceAIRepository.CompraResumen> findLastPurchasesByAlumno(UUID alumnoId, int limit);
}
