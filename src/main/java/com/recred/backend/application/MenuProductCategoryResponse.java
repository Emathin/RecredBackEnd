package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import java.util.UUID;

public record MenuProductCategoryResponse(
        UUID id,
        String descripcion
) {

    public static MenuProductCategoryResponse fromDomain(CategoriaProducto category) {
        if (category == null) {
            return null;
        }

        return new MenuProductCategoryResponse(
                category.getId(),
                category.getDescripcion()
        );
    }
}
