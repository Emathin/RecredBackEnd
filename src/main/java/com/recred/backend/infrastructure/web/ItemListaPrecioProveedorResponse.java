package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import java.math.BigDecimal;
import java.util.UUID;

public record ItemListaPrecioProveedorResponse(
        UUID id,
        UUID listaPrecioId,
        String nombreProductoProveedor,
        UUID productoInventarioId,
        String nombreProductoInventario,
        BigDecimal precio,
        String unidad,
        String notas,
        boolean mappingConfirmado
) {
    public static ItemListaPrecioProveedorResponse fromDomain(ItemListaPrecioProveedor domain) {
        if (domain == null) {
            return null;
        }
        return new ItemListaPrecioProveedorResponse(
                domain.getId(),
                domain.getListaPrecioId(),
                domain.getNombreProductoProveedor(),
                domain.getProductoInventarioId(),
                domain.getNombreProductoInventario(),
                domain.getPrecio(),
                domain.getUnidad(),
                domain.getNotas(),
                domain.isMappingConfirmado()
        );
    }
}
