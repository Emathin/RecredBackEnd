package com.recred.backend.domain.repositories;
import java.util.List;
import com.recred.backend.domain.models.Kiosquero;

import java.util.List;
import java.util.UUID;

public interface KiosqueroRepository {
    UUID findBuffetById(UUID kiosqueroId);
    UUID findBuffetByUsuarioId(UUID usuarioId);

    List<Kiosquero> findAll();
    List<UUID> findAllIds();
}
