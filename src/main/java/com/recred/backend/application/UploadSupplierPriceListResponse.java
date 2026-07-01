package com.recred.backend.application;

import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UploadSupplierPriceListResponse(
        UUID id,
        UUID supplierId,
        String fileUrl,
        String originalFilename,
        boolean active,
        LocalDateTime createdAt,
        List<Item> items
) {
    static UploadSupplierPriceListResponse fromDomain(ListaPrecioProveedor priceList) {
        return new UploadSupplierPriceListResponse(
                priceList.getId(),
                priceList.getProveedorId(),
                priceList.getUrlArchivo(),
                priceList.getNombreOriginal(),
                priceList.isActiva(),
                priceList.getCreadoEn(),
                itemsFrom(priceList)
        );
    }

    private static List<Item> itemsFrom(ListaPrecioProveedor priceList) {
        if (priceList.getItems() == null) {
            return List.of();
        }

        return priceList.getItems().stream()
                .map(Item::fromDomain)
                .toList();
    }

    public record Item(
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
        static Item fromDomain(ItemListaPrecioProveedor item) {
            return new Item(
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
}
