package com.recred.backend.infrastructure.web;

import java.util.UUID;

public record UpdateItemMappingRequest(
        UUID productoInventarioId
) {}
