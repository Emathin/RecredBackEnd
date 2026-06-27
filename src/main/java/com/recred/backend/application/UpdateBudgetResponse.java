package com.recred.backend.application;

import com.recred.backend.domain.models.PeriodoPresupuesto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateBudgetResponse(
        UUID id,
        UUID alumnoId,
        UUID creadoPorUsuarioId,
        BigDecimal montoLimiteGeneral,
        PeriodoPresupuesto periodo,
        LocalDate fechaInicio,
        boolean activo,
        List<ReglaPresupuestoCategoriaResponse> reglasCategoria,
        BigDecimal montoLimite
) {
    public record ReglaPresupuestoCategoriaResponse(
            UUID id,
            CategoriaProductoResponse categoria,
            BigDecimal porcentajeLimite,
            BigDecimal montoLimiteCalculado,
            boolean activo
    ) {}

    public record CategoriaProductoResponse(
            UUID id,
            String descripcion,
            Boolean activo
    ) {}
}
