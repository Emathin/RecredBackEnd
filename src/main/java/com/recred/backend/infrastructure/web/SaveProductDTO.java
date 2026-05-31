package com.recred.backend.infrastructure.web;

import com.recred.backend.application.SaveProductCommand;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record SaveProductDTO(
        String name,
        String description,
        BigDecimal price,
        BigDecimal weight,
        boolean requiresPreparation,
        UUID categoryID,
        String newCategoryName,
        UUID buffetId,
        Integer currentStock,
        Set<UUID> healthClassificationIds,
        Set<UUID> idsTypes
) {
    public SaveProductCommand toCommand() {
        return new SaveProductCommand(
                name,
                description,
                price,
                weight,
                requiresPreparation,
                categoryID,
                newCategoryName,
                buffetId,
                currentStock,
                healthClassificationIds,
                idsTypes
        );
    }
}
