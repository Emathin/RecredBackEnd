package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ListaPrecioProveedor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListaPrecioProveedorRepository {
    ListaPrecioProveedor save(ListaPrecioProveedor listaPrecio);
    Optional<ListaPrecioProveedor> findById(UUID id);
    List<ListaPrecioProveedor> findByProveedorId(UUID proveedorId);
}
