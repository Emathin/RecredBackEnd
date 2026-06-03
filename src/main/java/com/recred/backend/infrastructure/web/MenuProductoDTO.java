package com.recred.backend.infrastructure.web;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuProductoDTO(
        UUID id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Boolean bloqueado,
        String motivoBloqueo
) {
    public static MenuProductoDTO permitido(UUID id, String nombre,
                                            String descripcion, BigDecimal precio) {
        return new MenuProductoDTO(id, nombre, descripcion,
                precio, false, null);
    }

    public static MenuProductoDTO bloqueado(UUID id, String nombre, String descripcion,
                                            BigDecimal precio, String motivo) {
        return new MenuProductoDTO(id, nombre, descripcion,
                precio, true, motivo);
    }
}