package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;

import java.util.UUID;

public record CategoryProductResponse(
        UUID id,
        String descripcion,
        Boolean activo
) {
    public static CategoryProductResponse fromDomain(CategoriaProducto categoria) {
        return new CategoryProductResponse(
                categoria.getId(),
                categoria.getDescripcion(),
                categoria.getActivo()
        );
    }
}
