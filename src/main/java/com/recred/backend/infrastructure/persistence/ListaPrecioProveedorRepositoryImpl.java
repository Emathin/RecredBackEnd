package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.repositories.ListaPrecioProveedorRepository;
import com.recred.backend.infrastructure.entities.ListaPrecioProveedorEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ListaPrecioProveedorRepositoryImpl implements ListaPrecioProveedorRepository {

    private final SpringDataListaPrecioProveedorRepository repository;

    public ListaPrecioProveedorRepositoryImpl(SpringDataListaPrecioProveedorRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ListaPrecioProveedor save(ListaPrecioProveedor listaPrecio) {
        ListaPrecioProveedorEntity entity = ListaPrecioProveedorEntity.fromDomain(listaPrecio);
        ListaPrecioProveedorEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListaPrecioProveedor> findById(UUID id) {
        return repository.findById(id)
                .map(ListaPrecioProveedorEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListaPrecioProveedor> findByProveedorId(UUID proveedorId) {
        return repository.findByProveedorId(proveedorId).stream()
                .map(ListaPrecioProveedorEntity::toDomain)
                .collect(Collectors.toList());
    }
}
