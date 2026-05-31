package com.recred.backend.infrastructure.web;

import java.util.Set;
import java.util.UUID;

public class StudentDTO {
    private final UUID id;
    private Set<HealthClassificationDTO> healthRestrictions;

    public StudentDTO(UUID id, Set<HealthClassificationDTO> healthRestrictions) {
        this.id = id;
        this.healthRestrictions = healthRestrictions;
    }

    public UUID getId() {
        return id;
    }

    public Set<HealthClassificationDTO> getHealthRestrictions() {
        return healthRestrictions;
    }

    public void actualizarRestricciones(Set<HealthClassificationDTO> nuevasRestricciones) {
        this.healthRestrictions = nuevasRestricciones;
    }
}