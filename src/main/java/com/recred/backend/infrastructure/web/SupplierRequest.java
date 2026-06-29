package com.recred.backend.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank(message = "El nombre del proveedor es obligatorio")
        @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
        String nombre,

        @Size(max = 100, message = "El teléfono no puede superar los 100 caracteres")
        String telefono,

        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email,

        @Size(max = 200, message = "Los días de visita no pueden superar los 200 caracteres")
        String diasVisita,

        String notas
) {}
