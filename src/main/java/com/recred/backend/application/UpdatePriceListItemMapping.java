package com.recred.backend.application;

import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

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

    public ItemListaPrecioProveedor execute(
            UUID kiosqueroId,
            UUID itemId,
            UUID productoInventarioId
    ) {
        ItemListaPrecioProveedor item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ítem de lista de precios no encontrado"));

        ListaPrecioProveedor lista = listaRepository.findById(item.getListaPrecioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lista de precios asociada no encontrada"));

        Proveedor proveedor = proveedorRepository.findById(lista.getProveedorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proveedor asociado no encontrado"));

        if (!proveedor.getKiosqueroId().equals(kiosqueroId)) {
            throw new AccessDeniedException("No tiene permisos para modificar este ítem");
        }

        item.setProductoInventarioId(productoInventarioId);
        item.setMappingConfirmado(true);

        return itemRepository.save(item);
    }
}
