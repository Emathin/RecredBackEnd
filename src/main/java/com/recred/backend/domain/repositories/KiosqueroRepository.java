package com.recred.backend.domain.repositories;
import java.util.UUID;

public interface KiosqueroRepository {
    UUID findBuffetById(UUID kiosqueroId);
}
