package com.recred.backend.infrastructure.web;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuProductoDTO(
        UUID id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Boolean bloqueado,
        String motivoBloqueo,
        CategoryDTO categoria
) {
    public record CategoryDTO(UUID id, String descripcion) {}

    public static MenuProductoDTO permitido(
            UUID id, String nombre, String descripcion,
            BigDecimal precio, CategoryDTO categoria) {
        return new MenuProductoDTO(id, nombre, descripcion,
                precio, false, null, categoria);
    }

    public static MenuProductoDTO bloqueado(
            UUID id, String nombre, String descripcion,
            BigDecimal precio, String motivo, CategoryDTO categoria) {
        return new MenuProductoDTO(id, nombre, descripcion,
                precio, true, motivo, categoria);
    }
}