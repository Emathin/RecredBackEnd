package com.recred.backend.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record GetFavoritosByAlumnoResponse(
        UUID id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        BigDecimal peso,
        Boolean requierePreparacion,
        Boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn,
        Integer stockActual,
        Integer stockReservado,
        Integer stockMinimo,
        Integer stockDisponible,
        String estadoInventario,
        String tipoManejoInventario,
        Integer cupoMaximoDiario,
        Integer cupoDisponibleDia,
        String urlImagen,
        CategoriaResponse categoria,
        Set<ClasificacionResponse> clasificacionesSalud,
        Set<ClasificacionResponse> tipos
) {
    public record CategoriaResponse(UUID id, String descripcion) {}
    public record ClasificacionResponse(UUID id, String descripcion) {}
}
