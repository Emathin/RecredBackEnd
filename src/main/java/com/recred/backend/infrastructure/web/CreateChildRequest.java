package com.recred.backend.infrastructure.web;

import java.util.UUID;

public record CreateChildRequest(
        String username,
        String nombre,
        String apellido,
        String email,
        String dni,
        UUID gradoId
) {
}
