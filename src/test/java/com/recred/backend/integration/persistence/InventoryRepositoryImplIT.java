package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.InventoryRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataInventoryRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.recred.backend.domain.models.EstadoInventario.BAJO_STOCK;
import static com.recred.backend.domain.models.EstadoInventario.DISPONIBLE;
import static com.recred.backend.domain.models.RolUsuario.VENDEDOR;
import static com.recred.backend.domain.models.TipoManejoInventario.STOCK_EXACTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class InventoryRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private InventoryRepositoryImpl inventoryRepository;

    @Autowired
    private SpringDataInventoryRepository springDataInventoryRepository;

    @Autowired
    private EntityManager entityManager;

    private ColegioEntity cachedSchool;
    private UsuarioEntity cachedUser;
    private BuffetEntity cachedBuffet;
    private ProductoEntity cachedProduct;

    @Test
    void save_ShouldCreateNewEntity_WhenNotExists() {
        Inventario inventory = givenNewInventoryDomain();

        whenSavingInventory(inventory);

        thenInventoryIsPersistedWithExpectedStockAndState(inventory, 10, DISPONIBLE);
    }

    @Test
    void save_ShouldUpdateEntity_WhenAlreadyExists() {
        Inventario inventory = givenExistingInventoryDomainForUpdate();

        whenSavingInventory(inventory);

        thenInventoryIsPersistedWithExpectedStock(inventory, 50);
    }

    @Test
    void save_WhenSavedStateDoesNotMatchStock_ShouldPersistEffectiveState() {
        Inventario inventory = givenInventoryDomainWithLowStock();

        whenSavingInventory(inventory);

        thenInventoryIsPersistedWithEffectiveLowStockState(inventory);
    }

    @Test
    void findLockedByBuffetAndProduct_ShouldReturnMappedInventory() {
        InventarioBuffetEntity entity = givenPersistedInventoryEntityWithStock();

        Optional<Inventario> result = whenFindingLockedInventory(entity.getBuffetEntity().getId(), entity.getProductoEntity().getId());

        thenInventoryIsReturnedWithExpectedStock(result, 20);
    }

    @Test
    void checkIfProductIsInStock_ShouldReturnTrueIfAvailable() {
        InventarioBuffetEntity entity = givenPersistedInventoryEntityWithStock();

        boolean result = whenCheckingIfProductIsInStock(entity.getProductoEntity().getId(), entity.getBuffetEntity().getId());

        thenProductIsInStock(result);
    }

    @Test
    void findAvailableProductsByBuffet_ShouldMapAvailableProductsToDomain() {
        InventarioBuffetEntity entity = givenPersistedInventoryEntityWithStock();

        List<Producto> result = whenFindingAvailableProducts(entity.getBuffetEntity().getId());

        thenAvailableProductsAreReturned(result, entity.getProductoEntity().getNombre());
    }

    private Inventario givenNewInventoryDomain() {
        BuffetEntity buffet = generateBuffetEntity();
        ProductoEntity product = generateProductEntity();
        Inventario inventory = createInventoryDomain(buffet.getId(), product.getId());
        inventory.setStockActual(10);
        inventory.setEstado(DISPONIBLE);
        return inventory;
    }

    private Inventario givenExistingInventoryDomainForUpdate() {
        BuffetEntity buffet = generateBuffetEntity();
        ProductoEntity product = generateProductEntity();
        generateInventoryEntity(buffet, product, 10);
        Inventario inventory = createInventoryDomain(buffet.getId(), product.getId());
        inventory.setStockActual(50);
        inventory.setEstado(DISPONIBLE);
        return inventory;
    }

    private Inventario givenInventoryDomainWithLowStock() {
        BuffetEntity buffet = generateBuffetEntity();
        ProductoEntity product = generateProductEntity();
        Inventario inventory = createInventoryDomain(buffet.getId(), product.getId());
        inventory.setStockActual(4);
        inventory.setStockReservado(0);
        inventory.setStockMinimo(5);
        inventory.setEstado(DISPONIBLE);
        return inventory;
    }

    private InventarioBuffetEntity givenPersistedInventoryEntityWithStock() {
        BuffetEntity buffet = generateBuffetEntity();
        ProductoEntity product = generateProductEntity();
        return generateInventoryEntity(buffet, product, 20);
    }

    private void whenSavingInventory(Inventario inventory) {
        inventoryRepository.save(inventory);
        entityManager.flush();
        entityManager.clear();
    }

    private Optional<Inventario> whenFindingLockedInventory(UUID buffetId, UUID productId) {
        return inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId);
    }

    private boolean whenCheckingIfProductIsInStock(UUID productId, UUID buffetId) {
        return inventoryRepository.checkIfProductIsInStock(productId, buffetId);
    }

    private List<Producto> whenFindingAvailableProducts(UUID buffetId) {
        return inventoryRepository.findAvailableProductsByBuffet(buffetId);
    }

    private void thenInventoryIsPersistedWithExpectedStockAndState(Inventario inventory, int expectedStock, com.recred.backend.domain.models.EstadoInventario expectedState) {
        Optional<InventarioBuffetEntity> saved = springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(inventory.getBuffetId(), inventory.getProductoId());
        assertTrue(saved.isPresent());
        assertEquals(expectedStock, saved.get().getStockActual());
        assertEquals(expectedState, saved.get().getEstado());
    }

    private void thenInventoryIsPersistedWithExpectedStock(Inventario inventory, int expectedStock) {
        Optional<InventarioBuffetEntity> updated = springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(inventory.getBuffetId(), inventory.getProductoId());
        assertTrue(updated.isPresent());
        assertEquals(expectedStock, updated.get().getStockActual());
    }

    private void thenInventoryIsPersistedWithEffectiveLowStockState(Inventario inventory) {
        Optional<InventarioBuffetEntity> saved = springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(inventory.getBuffetId(), inventory.getProductoId());
        assertTrue(saved.isPresent());
        assertEquals(BAJO_STOCK, saved.get().getEstado(), "Should recalculate state effectively");
    }

    private void thenInventoryIsReturnedWithExpectedStock(Optional<Inventario> result, int expectedStock) {
        assertTrue(result.isPresent());
        assertEquals(expectedStock, result.get().getStockActual());
    }

    private void thenProductIsInStock(boolean result) {
        assertTrue(result);
    }

    private void thenAvailableProductsAreReturned(List<Producto> result, String expectedProductName) {
        assertFalse(result.isEmpty());
        assertEquals(expectedProductName, result.get(0).getNombre());
    }

    private ColegioEntity generateSchoolEntity() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("Inventory School");
            school.setCue("CUE-INVENTORY");
            school.setActivo(true);
            entityManager.persist(school);
            this.cachedSchool = school;
        }
        return this.cachedSchool;
    }

    private UsuarioEntity generateUserEntity() {
        if (this.cachedUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-inv-close");
            user.setNombre("Vendor Name");
            user.setApellido("Vendor Surname");
            user.setEmail("vendor.inv@test.com");
            user.setRol(VENDEDOR);
            user.setActivo(true);
            entityManager.persist(user);
            this.cachedUser = user;
        }
        return this.cachedUser;
    }

    private BuffetEntity generateBuffetEntity() {
        if (this.cachedBuffet == null) {
            BuffetEntity buffet = new BuffetEntity();
            buffet.setColegioEntity(generateSchoolEntity());
            buffet.setNombre("Inventory Buffet");
            entityManager.persist(buffet);
            this.cachedBuffet = buffet;
        }
        return this.cachedBuffet;
    }

    private com.recred.backend.infrastructure.entities.CategoriaProductoEntity generateCategoryEntity() {
        com.recred.backend.infrastructure.entities.CategoriaProductoEntity cat =
                new com.recred.backend.infrastructure.entities.CategoriaProductoEntity();
        cat.setDescripcion("Snacks-" + UUID.randomUUID().toString());
        cat.setActivo(true);
        entityManager.persist(cat);
        return cat;
    }

    private ProductoEntity generateProductEntity() {
        if (this.cachedProduct == null) {
            ProductoEntity product = new ProductoEntity();
            product.setCategoria(generateCategoryEntity());
            product.setNombre("Snack");
            product.setDescripcion("Desc");
            product.setPrecio(BigDecimal.TEN);
            product.setRequierePreparacion(false);
            product.setActivo(true);
            entityManager.persist(product);
            this.cachedProduct = product;
        }
        return this.cachedProduct;
    }

    private InventarioBuffetEntity generateInventoryEntity(BuffetEntity buffet, ProductoEntity product, int stock) {
        InventarioBuffetEntity entity = new InventarioBuffetEntity();
        entity.setBuffetEntity(buffet);
        entity.setProductoEntity(product);
        entity.setStockActual(stock);
        entity.setStockReservado(0);
        entity.setStockMinimo(5);
        entity.setEstado(DISPONIBLE);
        entity.setTipoManejo(STOCK_EXACTO);
        return springDataInventoryRepository.save(entity);
    }

    private Inventario createInventoryDomain(UUID buffetId, UUID productId) {
        Inventario inventory = new Inventario();
        inventory.setBuffetId(buffetId);
        inventory.setProductoId(productId);
        inventory.setTipoManejo(STOCK_EXACTO);
        return inventory;
    }
}
