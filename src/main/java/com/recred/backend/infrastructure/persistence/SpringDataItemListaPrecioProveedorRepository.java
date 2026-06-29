package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ItemListaPrecioProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataItemListaPrecioProveedorRepository 
        extends JpaRepository<ItemListaPrecioProveedorEntity, UUID> {
    List<ItemListaPrecioProveedorEntity> findByListaPrecioId(UUID listaPrecioId);

    @Query("""
        SELECT i FROM ItemListaPrecioProveedorEntity i
        JOIN i.listaPrecio l
        JOIN l.proveedor p
        WHERE p.kiosquero.id = :kiosqueroId
          AND l.activa = true
          AND i.productoInventario.id IN :productoIds
        ORDER BY i.productoInventario.id, i.precio ASC
    """)
    List<ItemListaPrecioProveedorEntity> findItemsActivosByKiosqueroIdAndProductoIds(
            @Param("kiosqueroId") UUID kiosqueroId,
            @Param("productoIds") List<UUID> productoIds);
}
