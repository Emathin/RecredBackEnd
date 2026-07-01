package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.springframework.security.access.AccessDeniedException;

public class DeleteSupplier {

    private final ProveedorRepository repository;

    public DeleteSupplier(ProveedorRepository repository) {
        this.repository = repository;
    }

    public DeleteSupplierResponse execute(DeleteSupplierRequest request) {
        request.validate();
        Proveedor supplier = supplier(request);
        requireSupplierOwnership(supplier, request);
        repository.delete(supplier);
        return new DeleteSupplierResponse(request.id());
    }

    private Proveedor supplier(DeleteSupplierRequest request) {
        return repository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    private void requireSupplierOwnership(Proveedor supplier, DeleteSupplierRequest request) {
        if (!supplier.belongsToKiosquero(request.kioskOwnerId())) {
            throw new AccessDeniedException("You do not have access to delete this supplier");
        }
    }
}
