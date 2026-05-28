package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.ConsumptionCategory;
import java.math.BigDecimal;

public record ConsumptionCategoryResponse(
        @JsonProperty("descripcion")
        String description,
        @JsonProperty("montoTotal")
        BigDecimal totalAmount
) {

    public static ConsumptionCategoryResponse fromDomain(ConsumptionCategory consumptionCategory) {
        return new ConsumptionCategoryResponse(
                consumptionCategory.description(),
                consumptionCategory.totalAmount()
        );
    }
}
