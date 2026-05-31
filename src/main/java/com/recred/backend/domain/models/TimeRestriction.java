package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TimeRestriction {

    private UUID id;
    private UUID studentId;
    private TimeBadge timeBadge;
    private ProductCategory category;
    private TypeClassification classification;
    private boolean active = true;

    public TimeRestriction(UUID id, UUID studentId, TimeBadge timeBadge,
                           ProductCategory category, TypeClassification classification) {
        this.id = id;
        this.studentId = studentId;
        this.timeBadge = timeBadge;
        this.category = category;
        this.classification = classification;
    }

    public boolean blocks(Product product, LocalDateTime momentoCompra) {
        if (!this.active) {
            return false;
        }

        if (!this.timeBadge.isWithinTheBadge(momentoCompra)) {
            return false;
        }

        if (this.category == null && this.classification == null) {
            return true;
        }

        boolean matchesCategory = false;
        if (this.category != null && product.getCategory() != null) {
            matchesCategory = product.getCategory().getId().equals(this.category.getId());
        }

        boolean matchesClassification = false;
        if (this.classification != null && product.getTypes() != null) {
            matchesClassification = product.getTypes().stream()
                    .anyMatch(type -> type.getId().equals(this.classification.getId()));
        }

        return matchesCategory || matchesClassification;
    }
}