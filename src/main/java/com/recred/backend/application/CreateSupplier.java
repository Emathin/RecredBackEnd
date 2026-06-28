package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;

import java.time.LocalDateTime;

public class CreateSupplier {

    private final ProveedorRepository repository;

    public CreateSupplier(ProveedorRepository repository) {
        this.repository = repository;
    }

    public Proveedor execute(CreateSupplierCommand command) {
        if (command.nombre() == null || command.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setKiosqueroId(command.kiosqueroId());
        proveedor.setNombre(command.nombre().trim());
        proveedor.setTelefono(command.telefono());
        proveedor.setEmail(command.email());
        proveedor.setDiasVisita(command.diasVisita());
        proveedor.setNotas(command.notas());
        proveedor.setCreadoEn(LocalDateTime.now());
        proveedor.setActualizadoEn(LocalDateTime.now());

        return repository.save(proveedor);
    }
}
