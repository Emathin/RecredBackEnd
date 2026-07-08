package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Kiosquero;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KiosqueroRepository {
    UUID findBuffetById(UUID kiosqueroId);
    UUID findBuffetByUsuarioId(UUID usuarioId);
    Optional<UUID> findKiosqueroIdByUsuarioId(UUID usuarioId);
    Optional<UUID> findSchoolIdByUsuarioId(UUID usuarioId);
    UUID findUsuarioIdById(UUID kiosqueroId);

    List<Kiosquero> findAll();
    List<UUID> findAllIds();
    Kiosquero save(Kiosquero kiosquero);
}
