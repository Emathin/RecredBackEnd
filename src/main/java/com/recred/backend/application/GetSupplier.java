package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

public class GetSupplier {

    private final ProveedorRepository repository;

    public GetSupplier(ProveedorRepository repository) {
        this.repository = repository;
    }

    public Proveedor execute(UUID id, UUID kiosqueroId) {
        Proveedor proveedor = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        if (!proveedor.getKiosqueroId().equals(kiosqueroId)) {
            throw new AccessDeniedException("No tiene permisos sobre este proveedor");
        }
        return proveedor;
    }
}
