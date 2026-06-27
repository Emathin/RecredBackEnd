package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetFavoritosByAlumnoResponse;
import com.recred.backend.domain.models.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ProductResponseDTO(
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
        String image,
        CategoryDTO categoria,
        Set<ClasificacionDTO> clasificacionesSalud,
        Set<ClasificacionDTO> tipos
) {

    public static ProductResponseDTO fromUseCaseResponse(GetFavoritosByAlumnoResponse response) {
        CategoryDTO categoriaDTO = null;
        if (response.categoria() != null) {
            categoriaDTO = new CategoryDTO(
                    response.categoria().id(),
                    response.categoria().descripcion()
            );
        }

        Set<ClasificacionDTO> saludDTOs = response.clasificacionesSalud() == null
                ? Set.of()
                : response.clasificacionesSalud().stream()
                        .map(cs -> new ClasificacionDTO(cs.id(), cs.descripcion()))
                        .collect(Collectors.toSet());

        Set<ClasificacionDTO> tiposDTOs = response.tipos() == null
                ? Set.of()
                : response.tipos().stream()
                        .map(ct -> new ClasificacionDTO(ct.id(), ct.descripcion()))
                        .collect(Collectors.toSet());

        return new ProductResponseDTO(
                response.id(),
                response.nombre(),
                response.descripcion(),
                response.precio(),
                response.peso(),
                response.requierePreparacion(),
                response.activo(),
                response.creadoEn(),
                response.actualizadoEn(),
                response.stockActual(),
                response.stockReservado(),
                response.stockMinimo(),
                response.stockDisponible(),
                response.estadoInventario(),
                response.tipoManejoInventario(),
                response.cupoMaximoDiario(),
                response.cupoDisponibleDia(),
                response.urlImagen(),
                response.urlImagen(),
                categoriaDTO,
                saludDTOs,
                tiposDTOs
        );
    }

    public static ProductResponseDTO fromDomain(Producto producto) {
        CategoryDTO categoriaDTO = null;
        if (producto.getCategoria() != null) {
            categoriaDTO = new CategoryDTO(
                    producto.getCategoria().getId(),
                    producto.getCategoria().getDescripcion()
            );
        }

        Set<ClasificacionDTO> saludDTOs = mapClasificacionesSalud(producto);
        Set<ClasificacionDTO> tiposDTOs = mapTipos(producto);

        return new ProductResponseDTO(
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
                producto.getUrlImagen(),
                categoriaDTO,
                saludDTOs,
                tiposDTOs
        );
    }

    private static Set<ClasificacionDTO> mapClasificacionesSalud(Producto producto) {
        if (producto.getClasificacionesSalud() == null) {
            return Set.of();
        }

        return producto.getClasificacionesSalud().stream()
                .map(cs -> new ClasificacionDTO(cs.getId(), cs.getDescripcion()))
                .collect(Collectors.toSet());
    }

    private static Set<ClasificacionDTO> mapTipos(Producto producto) {
        if (producto.getTipos() == null) {
            return Set.of();
        }

        return producto.getTipos().stream()
                .map(ct -> new ClasificacionDTO(ct.getId(), ct.getDescripcion()))
                .collect(Collectors.toSet());
    }

    public record CategoryDTO(UUID id, String descripcion) {
    }

    public record ClasificacionDTO(UUID id, String descripcion) {
    }
}
