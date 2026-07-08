package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.time.LocalDateTime;
import com.recred.backend.domain.exceptions.OperationNotAllowedException;

public class UpdateSupplier {

    private final ProveedorRepository repository;

    public UpdateSupplier(ProveedorRepository repository) {
        this.repository = repository;
    }

    public SupplierDetailsResponse execute(UpdateSupplierRequest request) {
        Proveedor supplier = findSupplierById(request);
        requireOwnership(supplier, request);
        updateSupplier(supplier, request);
        return SupplierDetailsResponse.fromDomain(repository.save(supplier));
    }

    private Proveedor findSupplierById(UpdateSupplierRequest request) {
        return repository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    private void requireOwnership(Proveedor supplier, UpdateSupplierRequest request) {
        if (!supplier.belongsToKiosquero(request.kioskOwnerId())) {
            throw new OperationNotAllowedException(
                    "You do not have access to modify this supplier");
        }
    }

    private void updateSupplier(Proveedor supplier, UpdateSupplierRequest request) {
        supplier.setNombre(request.name().trim());
        supplier.setTelefono(request.phone());
        supplier.setEmail(request.email());
        supplier.setDiasVisita(request.visitDays());
        supplier.setNotas(request.notes());
        supplier.setActualizadoEn(LocalDateTime.now());
    }
}
