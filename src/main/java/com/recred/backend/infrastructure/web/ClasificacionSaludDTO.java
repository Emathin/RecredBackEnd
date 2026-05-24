package com.recred.backend.infrastructure.web;

import java.util.UUID;

public class ClasificacionSaludDTO {
    private final UUID id;
    private final String descripcion;

    public ClasificacionSaludDTO(UUID id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    public UUID getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }
}