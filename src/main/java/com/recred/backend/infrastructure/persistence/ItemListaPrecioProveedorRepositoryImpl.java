package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.infrastructure.entities.ItemListaPrecioProveedorEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ItemListaPrecioProveedorRepositoryImpl implements ItemListaPrecioProveedorRepository {

    private final SpringDataItemListaPrecioProveedorRepository repository;

    public ItemListaPrecioProveedorRepositoryImpl(
            SpringDataItemListaPrecioProveedorRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ItemListaPrecioProveedor save(ItemListaPrecioProveedor item) {
        ItemListaPrecioProveedorEntity entity = ItemListaPrecioProveedorEntity.fromDomain(item);
        ItemListaPrecioProveedorEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    @Transactional
    public List<ItemListaPrecioProveedor> saveAll(List<ItemListaPrecioProveedor> items) {
        List<ItemListaPrecioProveedorEntity> entities = items.stream()
                .map(ItemListaPrecioProveedorEntity::fromDomain)
                .collect(Collectors.toList());
        List<ItemListaPrecioProveedorEntity> saved = repository.saveAll(entities);
        return saved.stream()
                .map(ItemListaPrecioProveedorEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemListaPrecioProveedor> findById(UUID id) {
        return repository.findById(id)
                .map(ItemListaPrecioProveedorEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemListaPrecioProveedor> findByListaPrecioId(UUID listaPrecioId) {
        return repository.findByListaPrecioId(listaPrecioId).stream()
                .map(ItemListaPrecioProveedorEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemListaPrecioProveedor> findItemsActivosByKiosqueroIdAndProductoIds(
            UUID kiosqueroId, List<UUID> productoIds) {
        return repository.findItemsActivosByKiosqueroIdAndProductoIds(
                kiosqueroId, productoIds).stream()
                .map(ItemListaPrecioProveedorEntity::toDomain)
                .collect(Collectors.toList());
    }
}
