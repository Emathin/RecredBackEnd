package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Proveedor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProveedorRepository {
    Proveedor save(Proveedor proveedor);
    Optional<Proveedor> findById(UUID id);
    Optional<Proveedor> findByIdAndKiosqueroId(UUID id, UUID kiosqueroId);
    List<Proveedor> findByKiosqueroId(UUID kiosqueroId);
    void delete(Proveedor proveedor);
    boolean existsByIdAndKiosqueroId(UUID id, UUID kiosqueroId);
}
