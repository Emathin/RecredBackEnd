package com.recred.backend.infrastructure.web;

import java.util.List;
import java.util.UUID;

public class ActualizarRestriccionesDTO {
    private List<UUID> clasificacionesIds;

    public List<UUID> getClasificacionesIds() {
        return clasificacionesIds;
    }

    public void setClasificacionesIds(List<UUID> clasificacionesIds) {
        this.clasificacionesIds = clasificacionesIds;
    }
}