package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.CategoriaProducto;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String descripcion,
        Boolean activo
) {
    public static CategoryResponse fromDomain(CategoriaProducto category) {
        return new CategoryResponse(
                category.getId(),
                category.getDescripcion(),
                category.getActivo()
        );
    }
}
