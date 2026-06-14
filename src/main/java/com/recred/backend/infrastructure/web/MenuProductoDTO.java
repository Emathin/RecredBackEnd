package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Producto;
import java.math.BigDecimal;
import java.util.UUID;

public record MenuProductoDTO(
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
        String motivoBloqueo
) {
    public static MenuProductoDTO permitido(UUID id, String nombre,
                                            String descripcion, BigDecimal precio) {
        return new MenuProductoDTO(id, nombre, descripcion,
                precio, null, null, null, null, null, null, null, null, false, null);
    }

    public static MenuProductoDTO bloqueado(UUID id, String nombre, String descripcion,
                                            BigDecimal precio, String motivo) {
        return new MenuProductoDTO(id, nombre, descripcion,
                precio, null, null, null, null, null, null, null, null, true, motivo);
    }

    public static MenuProductoDTO permitido(Producto producto) {
        return fromProduct(producto, false, null);
    }

    public static MenuProductoDTO bloqueado(Producto producto, String motivo) {
        return fromProduct(producto, true, motivo);
    }

    private static MenuProductoDTO fromProduct(
            Producto producto,
            Boolean bloqueado,
            String motivo
    ) {
        return new MenuProductoDTO(
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
                motivo
        );
    }
}
