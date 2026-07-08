package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import java.math.BigDecimal;
import java.util.UUID;

public record MenuProductResponse(
        UUID id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stockActual,
        Integer stockReservado,
        Integer stockMinimo,
        Integer stockDisponible,
        String estadoInventario,
        String tipoManejoInventario,
        Integer cupoMaximoDiario,
        Integer cupoDisponibleDia,
        Boolean bloqueado,
        String motivoBloqueo,
        MenuProductCategoryResponse categoria,
        String urlImagen
) {

    public static MenuProductResponse permitido(Producto producto) {
        return fromProduct(producto, false, null);
    }

    public static MenuProductResponse bloqueado(Producto producto, String motivo) {
        return fromProduct(producto, true, motivo);
    }

    private static MenuProductResponse fromProduct(
            Producto producto,
            Boolean bloqueado,
            String motivo
    ) {
        return new MenuProductResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
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
                bloqueado,
                motivo,
                MenuProductCategoryResponse.fromDomain(producto.getCategoria()),
                producto.getUrlImagen()
        );
    }
}
