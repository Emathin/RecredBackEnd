package com.recred.backend.integration.persistence;

import com.recred.backend.integration.AbstractIntegrationTest;

import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.persistence.InventoryRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataInventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class InventoryRepositoryImplTest extends AbstractIntegrationTest {

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
        BuffetEntity buffet = generateBuffetEntity();
        ProductoEntity product = generateProductEntity();
        Inventario inventory = createInventoryDomain(buffet.getId(), product.getId());
        inventory.setStockActual(10);
        inventory.setEstado(EstadoInventario.DISPONIBLE);

        inventoryRepository.save(inventory);
        entityManager.flush();
        entityManager.clear();

        Optional<InventarioBuffetEntity> saved = springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(buffet.getId(), product.getId());
        
        assertTrue(saved.isPresent());
        assertEquals(10, saved.get().getStockActual());
        assertEquals(EstadoInventario.DISPONIBLE, saved.get().getEstado());
    }

    @Test
    void save_ShouldUpdateEntity_WhenAlreadyExists() {
        BuffetEntity buffet = generateBuffetEntity();
        ProductoEntity product = generateProductEntity();
        generateInventoryEntity(buffet, product, 10);
        
        Inventario inventory = createInventoryDomain(buffet.getId(), product.getId());
        inventory.setStockActual(50);
        inventory.setEstado(EstadoInventario.DISPONIBLE);

        inventoryRepository.save(inventory);
        entityManager.flush();
        entityManager.clear();

        Optional<InventarioBuffetEntity> updated = springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(buffet.getId(), product.getId());
        
        assertTrue(updated.isPresent());
        assertEquals(50, updated.get().getStockActual());
    }

    @Test
    void save_WhenSavedStateDoesNotMatchStock_ShouldPersistEffectiveState() {
        BuffetEntity buffet = generateBuffetEntity();
        ProductoEntity product = generateProductEntity();
        Inventario inventory = createInventoryDomain(buffet.getId(), product.getId());
        inventory.setStockActual(4); // Below minimum which is 5
        inventory.setStockReservado(0);
        inventory.setStockMinimo(5);
        inventory.setEstado(EstadoInventario.DISPONIBLE); // Trying to force available

        inventoryRepository.save(inventory);
        entityManager.flush();
        entityManager.clear();

        Optional<InventarioBuffetEntity> saved = springDataInventoryRepository
                .findByBuffetEntityIdAndProductoEntityId(buffet.getId(), product.getId());
        
        assertTrue(saved.isPresent());
        assertEquals(EstadoInventario.BAJO_STOCK, saved.get().getEstado(), "Should recalculate state effectively");
    }

    @Test
    void findLockedByBuffetAndProduct_ShouldReturnMappedInventory() {
        BuffetEntity buffet = generateBuffetEntity();
        ProductoEntity product = generateProductEntity();
        generateInventoryEntity(buffet, product, 20);

        Optional<Inventario> result = inventoryRepository.findLockedByBuffetAndProduct(buffet.getId(), product.getId());

        assertTrue(result.isPresent());
        assertEquals(20, result.get().getStockActual());
    }

    @Test
    void checkIfProductIsInStock_ShouldReturnTrueIfAvailable() {
        BuffetEntity buffet = generateBuffetEntity();
        ProductoEntity product = generateProductEntity();
        generateInventoryEntity(buffet, product, 15);

        boolean result = inventoryRepository.checkIfProductIsInStock(product.getId(), buffet.getId());

        assertTrue(result);
    }

    @Test
    void findAvailableProductsByBuffet_ShouldMapAvailableProductsToDomain() {
        BuffetEntity buffet = generateBuffetEntity();
        ProductoEntity product = generateProductEntity();
        generateInventoryEntity(buffet, product, 15);

        List<Producto> result = inventoryRepository.findAvailableProductsByBuffet(buffet.getId());

        assertFalse(result.isEmpty());
        assertEquals(product.getNombre(), result.get(0).getNombre());
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
            user.setRol(RolUsuario.VENDEDOR);
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
        cat.setDescripcion("Snacks-" + java.util.UUID.randomUUID().toString());
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
        entity.setEstado(EstadoInventario.DISPONIBLE);
        entity.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        return springDataInventoryRepository.save(entity);
    }

    private Inventario createInventoryDomain(java.util.UUID buffetId, java.util.UUID productId) {
        Inventario inventory = new Inventario();
        inventory.setBuffetId(buffetId);
        inventory.setProductoId(productId);
        inventory.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        return inventory;
    }
}
