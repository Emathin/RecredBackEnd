package com.recred.backend.dominio.models;

import java.util.Set;
import java.util.UUID;

public class Alumno {
    private final UUID id;
    private Set<ClasificacionSalud> restriccionesSalud;

    public Alumno(UUID id, Set<ClasificacionSalud> restriccionesSalud) {
        this.id = id;
        this.restriccionesSalud = restriccionesSalud;
    }

    public UUID getId() { return id; }
    public Set<ClasificacionSalud> getRestriccionesSalud() { return restriccionesSalud; }

    public void actualizarRestricciones(Set<ClasificacionSalud> nuevasRestricciones) {
        this.restriccionesSalud = nuevasRestricciones;
    }
}