package com.recred.backend.application;

import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.springframework.security.access.AccessDeniedException;

public class UpdatePriceListItemMapping {

    private final ItemListaPrecioProveedorRepository itemRepository;
    private final ListaPrecioProveedorRepository listaRepository;
    private final ProveedorRepository proveedorRepository;

    public UpdatePriceListItemMapping(
            ItemListaPrecioProveedorRepository itemRepository,
            ListaPrecioProveedorRepository listaRepository,
            ProveedorRepository proveedorRepository
    ) {
        this.itemRepository = itemRepository;
        this.listaRepository = listaRepository;
        this.proveedorRepository = proveedorRepository;
    }

    public UpdatePriceListItemMappingResponse execute(
            UpdatePriceListItemMappingRequest request
    ) {
        request.validate();
        ItemListaPrecioProveedor item = item(request);
        ListaPrecioProveedor priceList = priceList(item);
        Proveedor supplier = supplier(priceList);
        requireSupplierOwnership(supplier, request);
        ItemListaPrecioProveedor savedItem = saveMapping(item, request);
        return UpdatePriceListItemMappingResponse.fromDomain(savedItem);
    }

    private ItemListaPrecioProveedor item(UpdatePriceListItemMappingRequest request) {
        return itemRepository.findById(request.itemId())
                .orElseThrow(() -> new IllegalArgumentException("Price list item not found"));
    }

    private ListaPrecioProveedor priceList(ItemListaPrecioProveedor item) {
        return listaRepository.findById(item.getListaPrecioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Associated price list not found"));
    }

    private Proveedor supplier(ListaPrecioProveedor priceList) {
        return proveedorRepository.findById(priceList.getProveedorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Associated supplier not found"));
    }

    private void requireSupplierOwnership(
            Proveedor supplier,
            UpdatePriceListItemMappingRequest request
    ) {
        if (!supplier.belongsToKiosquero(request.kioskOwnerId())) {
            throw new AccessDeniedException(
                    "You do not have access to modify this price list item");
        }
    }

    private ItemListaPrecioProveedor saveMapping(
            ItemListaPrecioProveedor item,
            UpdatePriceListItemMappingRequest request
    ) {
        item.setProductoInventarioId(request.inventoryProductId());
        item.setMappingConfirmado(true);
        return itemRepository.save(item);
    }
}
