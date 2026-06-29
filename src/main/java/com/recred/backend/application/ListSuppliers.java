package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;

import java.util.List;
import java.util.UUID;

public class ListSuppliers {

    private final ProveedorRepository repository;

    public ListSuppliers(ProveedorRepository repository) {
        this.repository = repository;
    }

    public List<Proveedor> execute(UUID kiosqueroId) {
        if (kiosqueroId == null) {
            throw new IllegalArgumentException("El ID del kiosquero es obligatorio");
        }
        return repository.findByKiosqueroId(kiosqueroId);
    }
}
