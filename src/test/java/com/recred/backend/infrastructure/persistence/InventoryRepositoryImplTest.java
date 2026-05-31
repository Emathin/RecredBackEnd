package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.InventoryState;
import com.recred.backend.domain.models.Inventory;
import com.recred.backend.domain.models.Product;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ProductCategoryEntity;
import com.recred.backend.infrastructure.entities.BuffetInventoryEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryRepositoryImplTest {

    @Mock
    private SpringDataInventoryRepository springDataInventoryRepository;

    private InventoryRepositoryImpl inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository = new InventoryRepositoryImpl(springDataInventoryRepository);
    }

    @Test
    void save_DeberiaCrearNuevaEntidad_CuandoNoExiste() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Inventory inventory = new Inventory();
        inventory.setBuffetId(buffetId);
        inventory.setProductId(productId);
        inventory.setCurrentStock(10);
        inventory.setStock(InventoryState.DISPONIBLE);

        when(springDataInventoryRepository.findByBuffetEntityIdAndProductoEntityId(buffetId, productId))
                .thenReturn(Optional.empty());
        when(springDataInventoryRepository.save(any(BuffetInventoryEntity.class))).thenAnswer(i -> i.getArgument(0));

        inventoryRepository.save(inventory);

        verify(springDataInventoryRepository).save(any(BuffetInventoryEntity.class));
    }

    @Test
    void save_DeberiaActualizarEntidad_CuandoYaExiste() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Inventory inventory = new Inventory();
        inventory.setBuffetId(buffetId);
        inventory.setProductId(productId);
        inventory.setCurrentStock(50);

        BuffetInventoryEntity existingEntity = new BuffetInventoryEntity();
        existingEntity.setCurrentStock(10);
        existingEntity.setBuffetEntity(new BuffetEntity());
        existingEntity.setProductEntity(new ProductEntity());

        when(springDataInventoryRepository.findByBuffetEntityIdAndProductoEntityId(buffetId, productId))
                .thenReturn(Optional.of(existingEntity));
        when(springDataInventoryRepository.save(any(BuffetInventoryEntity.class))).thenAnswer(i -> i.getArgument(0));

        inventoryRepository.save(inventory);

        assertEquals(50, existingEntity.getCurrentStock());
    }

    @Test
    void findByBuffetAndProduct_DeberiaLlamarAlSpringData() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        inventoryRepository.findByBuffetAndProduct(buffetId, productId);

        verify(springDataInventoryRepository).findByBuffetEntityIdAndProductoEntityId(buffetId, productId);
    }

    @Test
    void checkIfProductIsInStock_DeberiaLlamarAlSpringDataYRetornarElResultado() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(springDataInventoryRepository.existsByProductIdAndKiosqueroId(productId, buffetId))
                .thenReturn(true);

        boolean result = inventoryRepository.checkIfProductIsInStock(productId, buffetId);

        assertTrue(result);
        verify(springDataInventoryRepository).existsByProductIdAndKiosqueroId(productId, buffetId);
    }

    @Test
    void findAvailableProductsByBuffet_ShouldMapAvailableProductsToDomain() {
        UUID buffetId = UUID.randomUUID();

        ProductEntity productEntity = new ProductEntity();
        productEntity.setId(UUID.randomUUID());
        productEntity.setName("Jugo");

        ProductCategoryEntity categoria = new ProductCategoryEntity();
        categoria.setDescription("Bebidas");
        productEntity.setCategory(categoria);

        BuffetInventoryEntity inventario = new BuffetInventoryEntity();
        inventario.setProductEntity(productEntity);

        when(springDataInventoryRepository.findAvailableByBuffetId(buffetId))
                .thenReturn(List.of(inventario));

        List<Product> result = inventoryRepository.findAvailableProductsByBuffet(buffetId);

        assertEquals(1, result.size());
        assertEquals("Jugo", result.get(0).getName());
        assertEquals("Bebidas", result.get(0).getCategory().getDescription());
    }

    @Test
    void findAvailableProductsByBuffet_ShouldHandleNullCategory() {
        UUID buffetId = UUID.randomUUID();

        ProductEntity productEntity = new ProductEntity();
        productEntity.setId(UUID.randomUUID());
        productEntity.setName("Fruta");
        productEntity.setCategory(null);

        BuffetInventoryEntity inventario = new BuffetInventoryEntity();
        inventario.setProductEntity(productEntity);

        when(springDataInventoryRepository.findAvailableByBuffetId(buffetId))
                .thenReturn(List.of(inventario));

        List<Product> result = inventoryRepository.findAvailableProductsByBuffet(buffetId);

        assertNull(result.get(0).getCategory());
    }
}
