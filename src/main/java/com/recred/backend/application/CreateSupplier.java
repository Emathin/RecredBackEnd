package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.time.LocalDateTime;

public class CreateSupplier {

    private final ProveedorRepository repository;

    public CreateSupplier(ProveedorRepository repository) {
        this.repository = repository;
    }

    public SupplierDetailsResponse execute(CreateSupplierRequest request) {
        request.validate();
        return SupplierDetailsResponse.fromDomain(repository.save(supplier(request)));
    }

    private Proveedor supplier(CreateSupplierRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Proveedor supplier = new Proveedor();
        supplier.setKiosqueroId(request.kioskOwnerId());
        supplier.setNombre(request.name().trim());
        supplier.setTelefono(request.phone());
        supplier.setEmail(request.email());
        supplier.setDiasVisita(request.visitDays());
        supplier.setNotas(request.notes());
        supplier.setCreadoEn(now);
        supplier.setActualizadoEn(now);
        return supplier;
    }
}
