package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.time.LocalDateTime;
import org.springframework.security.access.AccessDeniedException;

public class UpdateSupplier {

    private final ProveedorRepository repository;

    public UpdateSupplier(ProveedorRepository repository) {
        this.repository = repository;
    }

    public SupplierDetailsResponse execute(UpdateSupplierRequest request) {
        request.validate();
        Proveedor supplier = supplier(request);
        requireSupplierOwnership(supplier, request);
        updateSupplier(supplier, request);
        return SupplierDetailsResponse.fromDomain(repository.save(supplier));
    }

    private Proveedor supplier(UpdateSupplierRequest request) {
        return repository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    private void requireSupplierOwnership(Proveedor supplier, UpdateSupplierRequest request) {
        if (!supplier.belongsToKiosquero(request.kioskOwnerId())) {
            throw new AccessDeniedException("You do not have access to modify this supplier");
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
