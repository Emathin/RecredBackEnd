package com.recred.backend.application;

import java.util.UUID;

public record CreateSupplierCommand(
        UUID kiosqueroId,
        String nombre,
        String telefono,
        String email,
        String diasVisita,
        String notas
) {}
