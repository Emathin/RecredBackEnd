package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemListaPrecioProveedorRepository {
    ItemListaPrecioProveedor save(ItemListaPrecioProveedor item);
    List<ItemListaPrecioProveedor> saveAll(List<ItemListaPrecioProveedor> items);
    Optional<ItemListaPrecioProveedor> findById(UUID id);
    List<ItemListaPrecioProveedor> findByListaPrecioId(UUID listaPrecioId);
    List<ItemListaPrecioProveedor> findItemsActivosByKiosqueroIdAndProductoIds(
            UUID kiosqueroId, List<UUID> productoIds);
}
