package com.recred.backend.application;

import java.util.UUID;

public record UpdateTimeRestrictionResponse(
        UUID id,
        UUID alumnoId,
        FranjaHorariaResponse franjaHoraria,
        CategoriaProductoResponse categoria,
        ClasificacionSaludResponse clasificacionSalud,
        boolean activa,
        UUID creadoPorUsuarioId
) {
    public record FranjaHorariaResponse(
            UUID id,
            UUID colegioId,
            String descripcion,
            java.time.LocalTime horaInicio,
            java.time.LocalTime horaFin,
            boolean activo,
            Integer cupoMaximo,
            Integer minutosCorte
    ) {}

    public record CategoriaProductoResponse(
            UUID id,
            String descripcion,
            Boolean activo
    ) {}

    public record ClasificacionSaludResponse(
            UUID id,
            String descripcion,
            Boolean activo
    ) {}
}
