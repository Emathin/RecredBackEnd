package com.recred.backend.application;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;

public class UpdateSupplier {

    private final ProveedorRepository repository;

    public UpdateSupplier(ProveedorRepository repository) {
        this.repository = repository;
    }

    public Proveedor execute(UpdateSupplierCommand command) {
        if (command.nombre() == null || command.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }

        Proveedor proveedor = repository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        if (!proveedor.getKiosqueroId().equals(command.kiosqueroId())) {
            throw new AccessDeniedException("No tiene permisos para modificar este proveedor");
        }

        proveedor.setNombre(command.nombre().trim());
        proveedor.setTelefono(command.telefono());
        proveedor.setEmail(command.email());
        proveedor.setDiasVisita(command.diasVisita());
        proveedor.setNotas(command.notas());
        proveedor.setActualizadoEn(LocalDateTime.now());

        return repository.save(proveedor);
    }
}
