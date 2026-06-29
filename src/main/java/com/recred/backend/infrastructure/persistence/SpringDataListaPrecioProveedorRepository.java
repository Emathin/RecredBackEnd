package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ListaPrecioProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataListaPrecioProveedorRepository 
        extends JpaRepository<ListaPrecioProveedorEntity, UUID> {
    List<ListaPrecioProveedorEntity> findByProveedorId(UUID proveedorId);
}
