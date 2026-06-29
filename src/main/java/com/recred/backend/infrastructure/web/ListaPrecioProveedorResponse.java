package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.ListaPrecioProveedor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record ListaPrecioProveedorResponse(
        UUID id,
        UUID proveedorId,
        String urlArchivo,
        String nombreOriginal,
        boolean activa,
        LocalDateTime creadoEn,
        List<ItemListaPrecioProveedorResponse> items
) {
    public static ListaPrecioProveedorResponse fromDomain(ListaPrecioProveedor domain) {
        if (domain == null) {
            return null;
        }
        return new ListaPrecioProveedorResponse(
                domain.getId(),
                domain.getProveedorId(),
                domain.getUrlArchivo(),
                domain.getNombreOriginal(),
                domain.isActiva(),
                domain.getCreadoEn(),
                domain.getItems() != null ? domain.getItems().stream()
                        .map(ItemListaPrecioProveedorResponse::fromDomain)
                        .collect(Collectors.toList()) : List.of()
        );
    }
}
