package com.recred.backend.presentacion;

import java.util.Set;
import java.util.UUID;

public class AlumnoDTO {
    private final UUID id;
    private Set<ClasificacionSaludDTO> restriccionesSalud;

    public AlumnoDTO(UUID id, Set<ClasificacionSaludDTO> restriccionesSalud) {
        this.id = id;
        this.restriccionesSalud = restriccionesSalud;
    }

    public UUID getId() {
        return id;
    }

    public Set<ClasificacionSaludDTO> getRestriccionesSalud() {
        return restriccionesSalud;
    }

    public void actualizarRestricciones(Set<ClasificacionSaludDTO> nuevasRestricciones) {
        this.restriccionesSalud = nuevasRestricciones;
    }
}