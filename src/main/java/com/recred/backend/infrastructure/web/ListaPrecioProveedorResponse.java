package com.recred.backend.infrastructure.web;

import com.recred.backend.application.UploadSupplierPriceListResponse;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
                        .toList() : List.of()
        );
    }

    public static ListaPrecioProveedorResponse fromApplication(
            UploadSupplierPriceListResponse response
    ) {
        if (response == null) {
            return null;
        }
        return new ListaPrecioProveedorResponse(
                response.id(),
                response.supplierId(),
                response.fileUrl(),
                response.originalFilename(),
                response.active(),
                response.createdAt(),
                response.items() != null ? response.items().stream()
                        .map(ItemListaPrecioProveedorResponse::fromApplication)
                        .toList() : List.of()
        );
    }
}
