package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.time.Clock;
import java.time.LocalDateTime;

public class CreateSupplier {

    private final ProveedorRepository repository;
    private final Clock clock;

    public CreateSupplier(ProveedorRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    public CreateSupplier(ProveedorRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public SupplierDetailsResponse execute(CreateSupplierRequest supplierRequest) {
        Proveedor supplier = buildSupplier(supplierRequest);
        Proveedor savedSupplier = repository.save(supplier);
        return SupplierDetailsResponse.fromDomain(savedSupplier);
    }

    private Proveedor buildSupplier(CreateSupplierRequest supplierRequest) {
        LocalDateTime now = LocalDateTime.now(clock);
        Proveedor supplier = new Proveedor();
        supplier.setKiosqueroId(supplierRequest.kioskOwnerId());
        supplier.setNombre(supplierRequest.name().trim());
        supplier.setTelefono(supplierRequest.phone());
        supplier.setEmail(supplierRequest.email());
        supplier.setDiasVisita(supplierRequest.visitDays());
        supplier.setNotas(supplierRequest.notes());
        supplier.setCreadoEn(now);
        supplier.setActualizadoEn(now);
        return supplier;
    }
}
