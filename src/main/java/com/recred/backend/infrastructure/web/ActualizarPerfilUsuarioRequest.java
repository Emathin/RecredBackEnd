package com.recred.backend.infrastructure.web;

public record ActualizarPerfilUsuarioRequest(
        String nombre,
        String apellido,
        String telefono,
        String dni
) {
}
