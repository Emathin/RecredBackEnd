package com.recred.backend.application;

import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdatePriceListItemMappingResponse(
        UUID id,
        UUID priceListId,
        String supplierProductName,
        UUID inventoryProductId,
        String inventoryProductName,
        BigDecimal price,
        String unit,
        String notes,
        boolean mappingConfirmed
) {
    static UpdatePriceListItemMappingResponse fromDomain(ItemListaPrecioProveedor item) {
        return new UpdatePriceListItemMappingResponse(
                item.getId(),
                item.getListaPrecioId(),
                item.getNombreProductoProveedor(),
                item.getProductoInventarioId(),
                item.getNombreProductoInventario(),
                item.getPrecio(),
                item.getUnidad(),
                item.getNotas(),
                item.isMappingConfirmado()
        );
    }
}
