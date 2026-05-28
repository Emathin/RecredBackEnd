package com.recred.backend.domain.repositories;
import com.recred.backend.domain.models.Kiosquero;

import java.util.List;
import java.util.UUID;

public interface KiosqueroRepository {
    UUID findBuffetById(UUID kiosqueroId);

    List<Kiosquero> findAll();
}
