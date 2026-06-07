package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Summary;
import java.util.Optional;
import java.util.UUID;

public interface SummaryRepository {
    Summary save(Summary summary);
    Optional<Summary> findLastByUsuarioId(UUID usuarioId);
}
