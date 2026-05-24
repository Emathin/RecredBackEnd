package com.recred.backend.infrastructure.web;

import java.util.Set;
import java.util.UUID;

public class ProductoDTO {
    private final UUID id;
    private final String nombre;
    private final Set<ClasificacionSaludDTO> clasificacionesSalud;

    public ProductoDTO(UUID id, String nombre, Set<ClasificacionSaludDTO> clasificacionesSalud) {
        this.id = id;
        this.nombre = nombre;
        this.clasificacionesSalud = clasificacionesSalud;
    }

    public UUID getId() {
        return id; 
    }

    public String getNombre() {
        return nombre;
    }
    public Set<ClasificacionSaludDTO> getClasificacionesSalud() {
        return clasificacionesSalud;
    }
}