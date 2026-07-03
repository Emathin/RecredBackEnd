package com.recred.backend.infrastructure.web;

import com.recred.backend.application.MenuProductCategoryResponse;
import com.recred.backend.application.MenuProductResponse;
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
        String motivoBloqueo,
        CategoryDTO categoria,
        String urlImagen
) {
    public record CategoryDTO(UUID id, String descripcion) {}

    public static MenuProductoDTO permitido(
            UUID id, String nombre, String descripcion,
            BigDecimal precio, CategoryDTO categoria) {
        return new MenuProductoDTO(id, nombre, descripcion,
                precio, null, null, null, null, null, null, null, null, false, null, categoria,
                null);
    }

    public static MenuProductoDTO bloqueado(
            UUID id, String nombre, String descripcion,
            BigDecimal precio, String motivo, CategoryDTO categoria) {
        return new MenuProductoDTO(id, nombre, descripcion,
                precio, null, null, null, null, null, null, null, null, true, motivo, categoria,
                null);
    }

    public static MenuProductoDTO permitido(Producto producto) {
        return fromProduct(producto, false, null);
    }

    public static MenuProductoDTO bloqueado(Producto producto, String motivo) {
        return fromProduct(producto, true, motivo);
    }

    public static MenuProductoDTO fromApplication(MenuProductResponse product) {
        return new MenuProductoDTO(
                product.id(),
                product.nombre(),
                product.descripcion(),
                product.precio(),
                product.stockActual(),
                product.stockReservado(),
                product.stockMinimo(),
                product.stockDisponible(),
                product.estadoInventario(),
                product.tipoManejoInventario(),
                product.cupoMaximoDiario(),
                product.cupoDisponibleDia(),
                product.bloqueado(),
                product.motivoBloqueo(),
                category(product.categoria()),
                product.urlImagen()
        );
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
                motivo,
                category(producto),
                producto.getUrlImagen()
        );
    }

    private static CategoryDTO category(Producto producto) {
        if (producto.getCategoria() == null) {
            return null;
        }

        return new CategoryDTO(
                producto.getCategoria().getId(),
                producto.getCategoria().getDescripcion()
        );
    }

    private static CategoryDTO category(MenuProductCategoryResponse category) {
        if (category == null) {
            return null;
        }

        return new CategoryDTO(category.id(), category.descripcion());
    }
}
