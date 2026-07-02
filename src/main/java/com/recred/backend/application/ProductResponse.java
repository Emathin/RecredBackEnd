package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ProductResponse(
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
        CategoryResponse categoria,
        Set<ClassificationResponse> clasificacionesSalud,
        Set<ClassificationResponse> tipos
) {

    public static ProductResponse fromDomain(Producto producto) {
        CategoryResponse categoryResponse = null;
        if (producto.getCategoria() != null) {
            categoryResponse = new CategoryResponse(
                    producto.getCategoria().getId(),
                    producto.getCategoria().getDescripcion()
            );
        }

        Set<ClassificationResponse> saludResponses = producto.getClasificacionesSalud() == null
                ? Set.of()
                : producto.getClasificacionesSalud().stream()
                        .map(cs -> new ClassificationResponse(cs.getId(), cs.getDescripcion()))
                        .collect(Collectors.toSet());

        Set<ClassificationResponse> tiposResponses = producto.getTipos() == null
                ? Set.of()
                : producto.getTipos().stream()
                        .map(ct -> new ClassificationResponse(ct.getId(), ct.getDescripcion()))
                        .collect(Collectors.toSet());

        return new ProductResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getPeso(),
                producto.getRequierePreparacion(),
                producto.getActivo(),
                producto.getCreadoEn(),
                producto.getActualizadoEn(),
                producto.getStockActual(),
                producto.getStockReservado(),
                producto.getStockMinimo(),
                producto.getStockDisponible(),
                producto.getEstadoInventario() == null
                        ? null
                        : producto.getEstadoInventario().name(),
                producto.getTipoManejoInventario() == null
                        ? null
                        : producto.getTipoManejoInventario().name(),
                producto.getCupoMaximoDiario(),
                producto.getCupoDisponibleDia(),
                producto.getUrlImagen(),
                categoryResponse,
                saludResponses,
                tiposResponses
        );
    }

    public record CategoryResponse(UUID id, String descripcion) {
    }

    public record ClassificationResponse(UUID id, String descripcion) {
    }
}
