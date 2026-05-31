package com.recred.backend.domain.repositories;
import java.util.List;
import java.util.UUID;

public interface KiosqueroRepository {
    UUID findBuffetById(UUID kiosqueroId);
    List<UUID> findAllIds();
}
