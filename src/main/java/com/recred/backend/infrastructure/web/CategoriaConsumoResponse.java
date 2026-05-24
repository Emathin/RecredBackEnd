package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.CategoriaConsumo;
import java.math.BigDecimal;

public record CategoriaConsumoResponse(String descripcion, BigDecimal montoTotal) {

    public static CategoriaConsumoResponse fromDomain(CategoriaConsumo categoriaConsumo) {
        return new CategoriaConsumoResponse(
                categoriaConsumo.descripcion(),
                categoriaConsumo.montoTotal()
        );
    }
}
