package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.*;
import com.recred.backend.infrastructure.entities.*;
import com.recred.backend.infrastructure.persistence.SpringDataStockMovementRepository;
import com.recred.backend.infrastructure.persistence.StockMovementRepositoryImpl;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
class StockMovementRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private StockMovementRepositoryImpl repository;

    @Autowired
    private SpringDataStockMovementRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveShouldMapMovementAndUserReference() {
        UsuarioEntity user = generateUser();
        InventarioBuffetEntity inventory = generateInventory();
        UUID purchaseId = UUID.randomUUID();
        
        MovimientoStock movement = createStockMovement(
                inventory.getId(), TipoMovimientoStock.INGRESO, 5, 1, 6, "Load", user.getId(), purchaseId);

        MovimientoStock result = repository.save(movement);
        entityManager.flush();
        entityManager.clear();

        assertNotNull(result.id());
        MovimientoStockEntity saved = springDataRepository.findById(result.id()).orElseThrow();
        
        assertEquals(inventory.getId(), saved.getInventario().getId());
        assertEquals(TipoMovimientoStock.INGRESO, saved.getTipo());
        assertEquals(5, saved.getCantidad());
        assertEquals(1, saved.getCantidadAnterior());
        assertEquals(6, saved.getCantidadNueva());
        assertEquals("Load", saved.getMotivo());
        assertEquals(user.getId(), saved.getCreadoPorUsuarioEntity().getId());
        assertEquals(purchaseId, saved.getCompraId());
    }

    @Test
    void saveShouldHandleNullUserId() {
        InventarioBuffetEntity inventory = generateInventory();
        
        MovimientoStock movement = createStockMovement(
                inventory.getId(), TipoMovimientoStock.AJUSTE, 2, 4, 6, "Adjustment", null, null);

        MovimientoStock result = repository.save(movement);
        entityManager.flush();
        entityManager.clear();

        MovimientoStockEntity saved = springDataRepository.findById(result.id()).orElseThrow();
        assertNull(saved.getCreadoPorUsuarioEntity());
        assertNull(result.usuarioId());
    }

    @Test
    void findByInventoryIdShouldMapEntitiesToDomain() {
        UsuarioEntity user = generateUser();
        InventarioBuffetEntity inventory = generateInventory();
        generateMovementEntity(inventory, user, TipoMovimientoStock.VENTA);

        List<MovimientoStock> result = repository.findByInventoryId(inventory.getId());

        assertEquals(1, result.size());
        assertEquals(inventory.getId(), result.get(0).inventarioId());
        assertEquals(user.getId(), result.get(0).usuarioId());
    }

    @Test
    void findByBuffetAndDateRangeShouldMapEntitiesToDomain() {
        InventarioBuffetEntity inventory = generateInventory();
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        generateMovementEntity(inventory, null, TipoMovimientoStock.AJUSTE);

        List<MovimientoStock> result = repository.findByBuffetAndDateRange(
                inventory.getBuffetEntity().getId(),
                start,
                end
        );

        assertEquals(1, result.size());
        assertNull(result.get(0).usuarioId());
    }

    private UsuarioEntity cachedAdmin;
    private ColegioEntity cachedSchool;
    private KiosqueroEntity cachedVendor;
    private BuffetEntity cachedBuffet;
    private CategoriaProductoEntity cachedCategory;
    private ProductoEntity cachedProduct;


    private MovimientoStock createStockMovement(
            UUID inventoryId, TipoMovimientoStock type, int quantity,
            int previousQty, int newQty, String reason, UUID userId, UUID purchaseId) {
        return new MovimientoStock(
                null,
                inventoryId,
                type,
                quantity,
                previousQty,
                newQty,
                reason,
                userId,
                purchaseId,
                LocalDateTime.now()
        );
    }

    private UsuarioEntity generateUser() {
        if (this.cachedAdmin == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-admin-stock");
            user.setNombre("Test");
            user.setApellido("User");
            user.setEmail("admin.stock@test.com");
            user.setRol(RolUsuario.ADMIN);
            user.setActivo(true);
            entityManager.persist(user);
            this.cachedAdmin = user;
        }
        return this.cachedAdmin;
    }

    private ColegioEntity getCachedSchool() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("School Stock");
            school.setCue("CUE-STK");
            school.setActivo(true);
            entityManager.persist(school);
            this.cachedSchool = school;
        }
        return this.cachedSchool;
    }

    private BuffetEntity getCachedBuffet() {
        if (this.cachedBuffet == null) {
            BuffetEntity buffet = new BuffetEntity();
            buffet.setNombre("Stock Buffet");
            buffet.setColegioEntity(getCachedSchool());
            buffet.setActivo(true);
            entityManager.persist(buffet);
            
            KiosqueroEntity vendor = getCachedVendor(buffet);
            buffet.setVendedor(vendor);
            entityManager.merge(buffet);
            
            this.cachedBuffet = buffet;
        }
        return this.cachedBuffet;
    }

    private KiosqueroEntity getCachedVendor(BuffetEntity buffet) {
        if (this.cachedVendor == null) {
            UsuarioEntity vendorUser = new UsuarioEntity();
            vendorUser.setIdCognito("sub-vendor-stock");
            vendorUser.setNombre("Vendor");
            vendorUser.setApellido("User");
            vendorUser.setEmail("vendor.stock@test.com");
            vendorUser.setRol(RolUsuario.VENDEDOR);
            vendorUser.setActivo(true);
            entityManager.persist(vendorUser);

            KiosqueroEntity vendor = new KiosqueroEntity();
            vendor.setUsuario(vendorUser);
            vendor.setBuffetEntity(buffet);
            vendor.setCuit("123456789");
            entityManager.persist(vendor);
            this.cachedVendor = vendor;
        }
        return this.cachedVendor;
    }

    private ProductoEntity getCachedProduct() {
        if (this.cachedProduct == null) {
            CategoriaProductoEntity category = new CategoriaProductoEntity();
            category.setDescripcion("Cat Stock");
            category.setActivo(true);
            entityManager.persist(category);
            this.cachedCategory = category;

            ProductoEntity product = new ProductoEntity();
            product.setCategoria(category);
            product.setNombre("Product Stock");
            product.setDescripcion("Desc");
            product.setPrecio(new BigDecimal("100"));
            product.setActivo(true);
            product.setRequierePreparacion(false);
            entityManager.persist(product);
            this.cachedProduct = product;
        }
        return this.cachedProduct;
    }

    private InventarioBuffetEntity generateInventory() {
        InventarioBuffetEntity inventory = new InventarioBuffetEntity();
        inventory.setBuffetEntity(getCachedBuffet());
        inventory.setProductoEntity(getCachedProduct());
        inventory.setStockActual(10);
        inventory.setStockMinimo(2);
        inventory.setEstado(EstadoInventario.DISPONIBLE);
        inventory.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        entityManager.persist(inventory);
        return inventory;
    }

    private MovimientoStockEntity generateMovementEntity(InventarioBuffetEntity inventory, UsuarioEntity user, TipoMovimientoStock type) {
        MovimientoStockEntity entity = new MovimientoStockEntity();
        entity.setInventario(inventory);
        entity.setTipo(type);
        entity.setCantidad(2);
        entity.setCantidadAnterior(4);
        entity.setCantidadNueva(2);
        entity.setMotivo("Reason");
        if (user != null) {
            entity.setCreadoPorUsuarioEntity(user);
        }
        springDataRepository.save(entity);
        return entity;
    }
}
