package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.HealthClassification;

import java.util.UUID;

public class HealthClassificationDTO {
    private final UUID id;
    private final String description;

    public HealthClassificationDTO(UUID id, String description) {
        this.id = id;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static HealthClassificationDTO fromDomain(HealthClassification model){
        HealthClassificationDTO dto = new HealthClassificationDTO(
                model.getId(), model.getDescription());
        return dto;
    }
}