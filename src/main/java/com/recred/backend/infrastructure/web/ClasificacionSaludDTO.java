package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.ClasificacionSalud;

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

    public static ClasificacionSaludDTO fromDomain(ClasificacionSalud modelo){
        ClasificacionSaludDTO dto = new ClasificacionSaludDTO(
                modelo.getId(), modelo.getDescripcion());
        return dto;
    }
}