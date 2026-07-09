package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.SuscripcionUsuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SuscripcionUsuarioRepository {
    Optional<SuscripcionUsuario> findActiveByUsuarioId(UUID usuarioId);

    List<SuscripcionUsuario> findByUsuarioId(UUID usuarioId);

    List<SuscripcionUsuario> findActiveExpiredAt(LocalDateTime referenceDate);

    List<SuscripcionUsuario> findActiveExpiringBetween(
            LocalDateTime from,
            LocalDateTime to
    );

    SuscripcionUsuario save(SuscripcionUsuario subscription);

    boolean hasUsedTrial(UUID usuarioId);
}
