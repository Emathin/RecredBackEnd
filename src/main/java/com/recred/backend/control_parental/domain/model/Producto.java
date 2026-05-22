package com.recred.backend.control_parental.domain.model;

import java.util.Set;
import java.util.UUID;

public class Producto {
    private final UUID id;
    private final String nombre;
    private final Set<ClasificacionSalud> clasificacionesSalud;

    public Producto(UUID id, String nombre, Set<ClasificacionSalud> clasificacionesSalud) {
        this.id = id;
        this.nombre = nombre;
        this.clasificacionesSalud = clasificacionesSalud;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public Set<ClasificacionSalud> getClasificacionesSalud() { return clasificacionesSalud; }
}