package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class InventoryRepositoryImpl implements InventoryRepository {

    private final SpringDataInventoryRepository springDataInventoryRepository;

    public InventoryRepositoryImpl(
            SpringDataInventoryRepository springDataInventoryRepository) {
        this.springDataInventoryRepository = springDataInventoryRepository;
    }

    @Override
    public boolean checkIfProductIsInStock(UUID productId, UUID buffetId) {
        return springDataInventoryRepository
                .existsByProductIdAndKiosqueroId(productId, buffetId);
    }

    @Override
    public Inventario save(Inventario inventario) {
        InventarioBuffetEntity entity = springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(
                        inventario.getBuffetId(), inventario.getProductoId())
                .orElse(new InventarioBuffetEntity());

        entity.setStockActual(inventario.getStockActual());
        entity.setStockMinimo(inventario.getStockMinimo());
        entity.setEstado(inventario.getEstado());

        if (entity.getBuffetEntity() == null) {
            BuffetEntity buffet = new BuffetEntity();
            buffet.setId(inventario.getBuffetId());
            entity.setBuffetEntity(buffet);
        }

        if (entity.getProductoEntity() == null) {
            ProductoEntity producto = new ProductoEntity();
            producto.setId(inventario.getProductoId());
            entity.setProductoEntity(producto);
        }

        return springDataInventoryRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Inventario> findByBuffetAndProduct(UUID buffetId, UUID productoId) {
        return springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(buffetId, productoId)
                .map(InventarioBuffetEntity::toDomain);
    }
}
