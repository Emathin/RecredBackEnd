package com.recred.backend.application;

import com.recred.backend.domain.repositories.ProveedorRepository;

public class ListSuppliers {

    private final ProveedorRepository repository;

    public ListSuppliers(ProveedorRepository repository) {
        this.repository = repository;
    }

    public ListSuppliersResponse execute(ListSuppliersRequest request) {
        request.validate();
        return ListSuppliersResponse.fromDomain(
                repository.findByKiosqueroId(request.kioskOwnerId())
        );
    }
}
