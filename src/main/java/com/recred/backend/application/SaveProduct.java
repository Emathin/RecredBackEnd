package com.recred.backend.application;

import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.TypeClassification;
import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.models.InventoryState;
import com.recred.backend.domain.models.Inventory;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SaveProduct {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;

    public SaveProduct(ProductRepository productRepository,
                       InventoryRepository inventoryRepository,
                       CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Product execute(SaveProductCommand command) {
        Product product = mapToModel(command);
        Product savedProduct = productRepository.save(product);

        if (command.buffetId() != null && command.stockActual() != null) {
            saveInventory(command, savedProduct);
            savedProduct.setCurrentStock(command.stockActual());
        }

        return savedProduct;
    }

    private void saveInventory(SaveProductCommand command, Product savedProduct) {
        Inventory inventory = inventoryRepository
                .findByBuffetAndProduct(command.buffetId(), savedProduct.getId())
                .orElseGet(() -> createNewInventory(command, savedProduct.getId()));

        inventory.setCurrentStock(command.stockActual());
        inventoryRepository.save(inventory);
    }

    private Inventory createNewInventory(SaveProductCommand command, UUID productoId) {
        Inventory inventory = new Inventory();
        inventory.setBuffetId(command.buffetId());
        inventory.setProductId(productoId);
        inventory.setMinimumStock(0);
        inventory.setStock(InventoryState.DISPONIBLE);
        return inventory;
    }

    private Product mapToModel(SaveProductCommand command) {
        Product product = new Product();
        product.setName(command.nombre());
        product.setDescription(command.descripcion());
        product.setPrice(command.precio());
        product.setWeight(command.peso());
        product.setRequiresPreparation(command.requierePreparacion());
        product.setActive(true);

        handleCategory(command, product);

        if (command.clasificacionesSaludIds() != null) {
            Set<HealthClassification> clasificaciones = command.clasificacionesSaludIds()
                    .stream().map(id -> {
                        HealthClassification cs = new HealthClassification();
                        cs.setId(id);
                        return cs;
                    }).collect(Collectors.toSet());
            product.setHealthClassifications(clasificaciones);
        }

        if (command.tiposIds() != null) {
            Set<TypeClassification> tipos = command.tiposIds().stream().map(id -> {
                TypeClassification ct = new TypeClassification();
                ct.setId(id);
                return ct;
            }).collect(Collectors.toSet());
            product.setTypes(tipos);
        }

        return product;
    }

    private void handleCategory(SaveProductCommand command, Product product) {
        if (command.nuevaCategoriaNombre() != null
                && !command.nuevaCategoriaNombre().isBlank()) {
            ProductCategory category = categoryRepository
                    .findByDescription(command.nuevaCategoriaNombre())
                    .orElseGet(() -> createAndSaveCategory(command.nuevaCategoriaNombre()));
            product.setCategory(category);
            return;
        }

        if (command.categoriaId() != null) {
            ProductCategory categoria = new ProductCategory();
            categoria.setId(command.categoriaId());
            product.setCategory(categoria);
        }
    }

    private ProductCategory createAndSaveCategory(String name) {
        ProductCategory category = new ProductCategory();
        category.setDescription(name);
        category.setActive(true);
        return categoryRepository.save(category);
    }
}
