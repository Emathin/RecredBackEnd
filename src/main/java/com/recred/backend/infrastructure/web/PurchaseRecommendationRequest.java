package com.recred.backend.infrastructure.web;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record PurchaseRecommendationRequest(
        @NotEmpty(message = "La lista de IDs de productos no puede estar vacía")
        List<UUID> productosIds
) {}
