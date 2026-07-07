package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;

public class GetSupplier {

    private final ProveedorRepository repository;

    public GetSupplier(ProveedorRepository repository) {
        this.repository = repository;
    }

    public SupplierDetailsResponse execute(GetSupplierRequest request) {
        Proveedor supplier = supplier(request);
        requireSupplierOwnership(supplier, request);
        return SupplierDetailsResponse.fromDomain(supplier);
    }

    private Proveedor supplier(GetSupplierRequest request) {
        return repository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    private void requireSupplierOwnership(Proveedor supplier, GetSupplierRequest request) {
        if (!supplier.belongsToKiosquero(request.kioskOwnerId())) {
            throw new SecurityException("You do not have access to this supplier");
        }
    }
}
