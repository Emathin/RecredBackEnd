package com.recred.backend.dominio.models;

import java.util.UUID;

public class ClasificacionSalud {
    private final UUID id;
    private final String descripcion;

    public ClasificacionSalud(UUID id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    public UUID getId() { return id; }
    public String getDescripcion() { return descripcion; }
}