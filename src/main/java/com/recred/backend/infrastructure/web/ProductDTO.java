package com.recred.backend.infrastructure.web;

import java.util.Set;
import java.util.UUID;

public class ProductDTO {
    private final UUID id;
    private final String name;
    private final Set<HealthClassificationDTO> healthClassifications;

    public ProductDTO(UUID id, String name, Set<HealthClassificationDTO> healthClassifications) {
        this.id = id;
        this.name = name;
        this.healthClassifications = healthClassifications;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public Set<HealthClassificationDTO> getHealthClassifications() {
        return healthClassifications;
    }
}
