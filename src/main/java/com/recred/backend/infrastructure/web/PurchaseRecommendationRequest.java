package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record PurchaseRecommendationRequest(
        @JsonProperty("productosIds")
        @JsonAlias("productIds")
        @NotEmpty(message = "Product id list cannot be empty")
        List<UUID> productIds
) {}
