package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record UpdateItemMappingRequest(
        @JsonProperty("productoInventarioId")
        @JsonAlias("inventoryProductId")
        UUID inventoryProductId
) {}
