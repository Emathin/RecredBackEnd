package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Inventory;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.BuffetInventoryEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Inventory save(Inventory inventory) {
        BuffetInventoryEntity entity = springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(
                        inventory.getBuffetId(), inventory.getProductId())
                .orElse(new BuffetInventoryEntity());

        entity.setCurrentStock(inventory.getCurrentStock());
        entity.setMinimumStock(inventory.getMinimumStock());
        entity.setState(inventory.getStock());

        if (entity.getBuffetEntity() == null) {
            BuffetEntity buffet = new BuffetEntity();
            buffet.setId(inventory.getBuffetId());
            entity.setBuffetEntity(buffet);
        }

        if (entity.getProductEntity() == null) {
            ProductEntity producto = new ProductEntity();
            producto.setId(inventory.getProductId());
            entity.setProductEntity(producto);
        }

        return springDataInventoryRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Inventory> findByBuffetAndProduct(UUID buffetId, UUID productoId) {
        return springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(buffetId, productoId)
                .map(BuffetInventoryEntity::toDomain);
    }

    @Override
    public List<Product> findAvailableProductsByBuffet(UUID buffetId) {
        return springDataInventoryRepository.findAvailableByBuffetId(buffetId).stream()
                .map(BuffetInventoryEntity::getProductEntity)
                .map(this::toProductDomain)
                .collect(Collectors.toList());
    }

    private Product toProductDomain(ProductEntity entity) {
        Product product = new Product();
        product.setId(entity.getId());
        product.setName(entity.getName());

        if (entity.getCategory() != null) {
            ProductCategory category =
                    new ProductCategory();
            category.setId(entity.getCategory().getId());
            category.setDescription(entity.getCategory().getDescription());
            product.setCategory(category);
        }
        return product;
    }
}
