package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.ProductoMedida;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.entities.ClasificacionTipoEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.ProductoMedidaEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.ProductRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataProductRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
public class ProductRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private ProductRepositoryImpl productRepository;

    @Autowired
    private SpringDataProductRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByIdReturnsDomainObjectIfExists() {
        ProductoEntity entity = generateProduct("Test Product", true);
        
        Optional<Producto> result = productRepository.findById(entity.getId());
        
        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals("Test Product", result.get().getNombre());
    }

    @Test
    void findByIdShouldReturnEmptyWhenNotExists() {
        Optional<Producto> result = productRepository.findById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void saveShouldCreateNewEntityWhenProductHasNoId() {
        CategoriaProductoEntity category = generateCategory("Category 1");
        
        Producto product = createDomainProduct(null, "New Product", "100", true, category.getId());
        
        Producto result = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();
        
        assertNotNull(result.getId());
        assertTrue(springDataRepository.findById(result.getId()).isPresent());
    }

    @Test
    void saveShouldUpdateExistingEntityWhenProductHasId() {
        ProductoEntity existing = generateProduct("Old Product", true);
        CategoriaProductoEntity newCategory = generateCategory("Category 2");
        
        Producto product = createDomainProduct(existing.getId(), "Updated Product", "200", true, newCategory.getId());
        
        productRepository.save(product);
        entityManager.flush();
        entityManager.clear();
        
        ProductoEntity updated = springDataRepository.findById(existing.getId()).get();
        assertEquals("Updated Product", updated.getNombre());
        assertEquals(newCategory.getId(), updated.getCategoria().getId());
    }

    @Test
    void saveShouldMapEverythingIncludingMeasurementsTypesAndClassifications() {
        CategoriaProductoEntity category = generateCategory("Full Category");
        ProductoEntity dummyProduct = generateProduct("Dummy", true);
        ProductoMedidaEntity measurement = generateMeasurement(dummyProduct);
        ClasificacionTipoEntity type = generateType("Drink");
        ClasificacionSaludEntity health = generateHealth("Gluten Free");
        
        Producto product = createDomainProduct(null, "Complete Product", "150", true, category.getId());
        
        ProductoMedida pm = new ProductoMedida();
        pm.setCantidadUnidad(new BigDecimal("2.5"));
        pm.setUnidad(com.recred.backend.domain.models.UnidadMedida.KG);
        product.addMedida(pm);
        
        ClasificacionTipo ct = new ClasificacionTipo();
        ct.setId(type.getId());
        product.setTipos(Set.of(ct));
        
        ClasificacionSalud cs = new ClasificacionSalud();
        cs.setId(health.getId());
        product.setClasificacionesSalud(Set.of(cs));
        
        Producto result = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();
        
        ProductoEntity saved = springDataRepository.findById(result.getId()).get();
        assertNotNull(saved.getCategoria());
        assertFalse(saved.getMedidas().isEmpty());
        assertFalse(saved.getTipos().isEmpty());
        assertFalse(saved.getClasificacionesSalud().isEmpty());
    }

    @Test
    void saveShouldClearCollectionsWhenEmpty() {
        ProductoEntity existing = generateProduct("Old", true);
        existing.getMedidas().add(generateMeasurement(existing));
        existing.getTipos().add(generateType("Snack"));
        existing.getClasificacionesSalud().add(generateHealth("Vegan"));
        entityManager.merge(existing);
        entityManager.flush();
        
        Producto product = createDomainProduct(existing.getId(), "Clean Product", "50", true, existing.getCategoria().getId());
        product.setMedidas(new HashSet<>());
        product.setTipos(new HashSet<>());
        product.setClasificacionesSalud(new HashSet<>());
        
        productRepository.save(product);
        entityManager.flush();
        entityManager.clear();
        
        ProductoEntity saved = springDataRepository.findById(existing.getId()).get();
        assertTrue(saved.getMedidas().isEmpty());
        assertTrue(saved.getTipos().isEmpty());
        assertTrue(saved.getClasificacionesSalud().isEmpty());
    }

    @Test
    void findBuffetLeastSoldProductsReturnsAProduct() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product1 = generateProduct("Water", true);
        ProductoEntity product2 = generateProduct("Soda", true);
        
        generatePurchase(buffet, product1, 10);
        generatePurchase(buffet, product2, 1);
        
        ProductChangeRecommendation result = productRepository.findBuffetLeastSoldProducts(buffet.getId());
        
        assertNotNull(result);
        assertEquals(product2.getId(), result.getId());
        assertEquals(buffet.getId(), result.getIdBuffet());
    }

    @Test
    void saveShouldNotReassignCategoryWhenIdIsEqual() {
        CategoriaProductoEntity category = generateCategory("Unique Category");
        ProductoEntity entity = generateProduct("Prod", true);
        entity.setCategoria(category);
        entityManager.persist(entity);
        
        Producto product = createDomainProduct(entity.getId(), "Mod", "10", true, category.getId());
        
        productRepository.save(product);
        entityManager.flush();
        entityManager.clear();
        
        ProductoEntity saved = springDataRepository.findById(entity.getId()).get();
        assertEquals(category.getId(), saved.getCategoria().getId());
    }

    @Test
    void whenFindAllActiveWithStockIsCalledAndStockExistsThenMapsCorrectly() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct("Cookie", true);
        generateInventory(buffet, product, 50, 5, 3, com.recred.backend.domain.models.EstadoInventario.DISPONIBLE, com.recred.backend.domain.models.TipoManejoInventario.STOCK_EXACTO, 20);
        
        List<Producto> result = productRepository.findAllActiveWithStock(buffet.getId());
        
        assertFalse(result.isEmpty());
        Producto p = result.stream().filter(prod -> prod.getId().equals(product.getId())).findFirst().get();
        assertEquals(50, p.getStockActual());
        assertEquals(5, p.getStockReservado());
        assertEquals(3, p.getStockMinimo());
        assertEquals(20, p.getCupoMaximoDiario());
        assertEquals(45, p.getStockDisponible());
    }

    @Test
    void findAllActiveWithStockWhenNoMinimumAndFourRemainingShouldMapLowStock() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct("Gummy", true);
        generateInventory(buffet, product, 4, 0, null, com.recred.backend.domain.models.EstadoInventario.BAJO_STOCK, com.recred.backend.domain.models.TipoManejoInventario.STOCK_EXACTO, null);
        
        List<Producto> result = productRepository.findAllActiveWithStock(buffet.getId());
        
        Producto p = result.stream().filter(prod -> prod.getId().equals(product.getId())).findFirst().get();
        assertEquals(EstadoInventario.BAJO_STOCK, p.getEstadoInventario());
    }

    @Test
    void findAllActiveWithStockWhenNoStockShouldMapOutOfStock() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct("Chocolate", true);
        generateInventory(buffet, product, 0, 0, null, com.recred.backend.domain.models.EstadoInventario.SIN_STOCK, com.recred.backend.domain.models.TipoManejoInventario.STOCK_EXACTO, null);
        
        List<Producto> result = productRepository.findAllActiveWithStock(buffet.getId());
        
        Producto p = result.stream().filter(prod -> prod.getId().equals(product.getId())).findFirst().get();
        assertEquals(EstadoInventario.SIN_STOCK, p.getEstadoInventario());
    }

    @Test
    void findActiveByIdWithStockWhenThereIsNoInventoryDataShouldReturnEmpty() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct("No Inventory", true);
        
        Optional<Producto> result = productRepository.findActiveByIdWithStock(product.getId(), buffet.getId());
        
        assertTrue(result.isEmpty());
    }

    @Test
    void whenFindActiveByIdWithStockIsCalledAndExistsThenMapsCorrectly() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct("Candy", true);
        generateInventory(buffet, product, 10, 2, 1, com.recred.backend.domain.models.EstadoInventario.DISPONIBLE, com.recred.backend.domain.models.TipoManejoInventario.STOCK_EXACTO, null);
        
        Optional<Producto> result = productRepository.findActiveByIdWithStock(product.getId(), buffet.getId());
        
        assertTrue(result.isPresent());
        assertEquals(10, result.get().getStockActual());
        assertEquals(8, result.get().getStockDisponible());
    }

    @Test
    void findAllActiveReturnsMappedDomains() {
        ProductoEntity active = generateProduct("Active 1", true);
        generateProduct("Inactive 1", false);
        
        List<Producto> result = productRepository.findAllActive();
        
        assertTrue(result.size() >= 1);
        assertTrue(result.stream().anyMatch(p -> p.getId().equals(active.getId())));
        assertFalse(result.stream().anyMatch(p -> p.getNombre().equals("Inactive 1")));
    }

    @Test
    void findActiveByIdReturnsMappedDomain() {
        ProductoEntity active = generateProduct("Active 2", true);
        
        Optional<Producto> result = productRepository.findActiveById(active.getId());
        
        assertTrue(result.isPresent());
        assertEquals(active.getId(), result.get().getId());
    }

    @Test
    void findBuffetTop5LeastSoldProductsMapsRecommendations() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product1 = generateProduct("Bread", true);
        ProductoEntity product2 = generateProduct("Water", true);
        ProductoEntity product3 = generateProduct("Apple", true);
        
        generatePurchase(buffet, product1, 100);
        generatePurchase(buffet, product2, 50);
        generatePurchase(buffet, product3, 10);
        
        List<ProductChangeRecommendation> result = productRepository.findBuffetTop5LeastSoldProducts(buffet.getId());
        
        assertFalse(result.isEmpty());
        assertEquals(product3.getId(), result.get(0).getId());
    }

    @Test
    void findBestSoldProductsNotInStockMapsDomains() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product1 = generateProduct("Top 1", true);
        ProductoEntity product2 = generateProduct("Top 2", true);
        
        generatePurchase(buffet, product1, 500);
        generatePurchase(buffet, product2, 10);
        
        List<Producto> result = productRepository
                .findBestSoldProductsNotInStock(buffet.getId());
        
        assertFalse(result.isEmpty());
        assertEquals(product1.getId(), result.get(0).getId());
    }

    @Test
    void updatePriceDelegatesToSpringDataRepository() {
        ProductoEntity product = generateProduct("To Update", true);
        BigDecimal newPrice = new BigDecimal("999.99");
        
        productRepository.updatePrice(product.getId(), newPrice);
        entityManager.flush();
        entityManager.clear();
        
        ProductoEntity updated = springDataRepository.findById(product.getId()).get();
        assertEquals(newPrice.compareTo(updated.getPrecio()), 0);
    }

    @Test
    void findBuffetTop3MostSoldShouldMapToDomain() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product1 = generateProduct("Best Seller 1", true);
        ProductoEntity product2 = generateProduct("Best Seller 2", true);
        
        generateInventory(buffet, product1, 100, 0, 10, com.recred.backend.domain.models.EstadoInventario.DISPONIBLE, com.recred.backend.domain.models.TipoManejoInventario.STOCK_EXACTO, null);
        generateInventory(buffet, product2, 100, 0, 10, com.recred.backend.domain.models.EstadoInventario.DISPONIBLE, com.recred.backend.domain.models.TipoManejoInventario.STOCK_EXACTO, null);
        
        generatePurchase(buffet, product1, 100);
        generatePurchase(buffet, product2, 10);
        
        List<Producto> result = productRepository.findBuffetTop3MostSold(buffet.getId(), UUID.randomUUID());
        
        assertFalse(result.isEmpty());
        assertEquals(product1.getId(), result.get(0).getId());
    }
    
    private CategoriaProductoEntity generateCategory(String name) {
        CategoriaProductoEntity category = new CategoriaProductoEntity();
        category.setDescripcion(name);
        category.setActivo(true);
        entityManager.persist(category);
        return category;
    }
    
    private ProductoEntity generateProduct(String name, boolean active) {
        CategoriaProductoEntity category = generateCategory("General " + UUID.randomUUID().toString().substring(0, 5));
        ProductoEntity p = new ProductoEntity();
        p.setCategoria(category);
        p.setNombre(name);
        p.setDescripcion("Desc");
        p.setPrecio(new BigDecimal("100"));
        p.setActivo(active);
        p.setRequierePreparacion(false);
        entityManager.persist(p);
        return p;
    }
    
    private Producto createDomainProduct(UUID id, String name, String price, boolean active, UUID categoryId) {
        Producto product = new Producto();
        product.setId(id);
        product.setNombre(name);
        product.setDescripcion(name);
        product.setPrecio(new BigDecimal(price));
        product.setActivo(active);
        product.setRequierePreparacion(false);
        if (categoryId != null) {
            CategoriaProducto domainCategory = new CategoriaProducto();
            domainCategory.setId(categoryId);
            product.setCategoria(domainCategory);
        }
        return product;
    }
    
    private ProductoMedidaEntity generateMeasurement(ProductoEntity product) {
        ProductoMedidaEntity m = new ProductoMedidaEntity();
        m.setProducto(product);
        m.setCantidadUnidad(new BigDecimal("1.00"));
        m.setUnidad(com.recred.backend.domain.models.UnidadMedida.L);
        entityManager.persist(m);
        return m;
    }
    
    private ClasificacionTipoEntity generateType(String name) {
        ClasificacionTipoEntity t = new ClasificacionTipoEntity();
        t.setDescripcion(name);
        entityManager.persist(t);
        return t;
    }
    
    private ClasificacionSaludEntity generateHealth(String name) {
        ClasificacionSaludEntity s = new ClasificacionSaludEntity();
        s.setDescripcion(name);
        entityManager.persist(s);
        return s;
    }
    
    private BuffetEntity generateBuffet() {
        ColegioEntity school = new ColegioEntity();
        school.setNombre("School " + UUID.randomUUID().toString().substring(0, 5));
        school.setCue("CUE-" + UUID.randomUUID().toString().substring(0, 5));
        school.setActivo(true);
        entityManager.persist(school);
        
        UsuarioEntity vendorUser = new UsuarioEntity();
        vendorUser.setIdCognito("sub-" + UUID.randomUUID().toString().substring(0, 8));
        vendorUser.setNombre("Vendor");
        vendorUser.setApellido("Seller");
        vendorUser.setEmail("k@" + UUID.randomUUID().toString().substring(0, 5) + ".com");
        vendorUser.setRol(RolUsuario.VENDEDOR);
        vendorUser.setActivo(true);
        entityManager.persist(vendorUser);
        
        BuffetEntity buffet = new BuffetEntity();
        buffet.setNombre("Central Buffet");
        buffet.setColegioEntity(school);
        buffet.setActivo(true);
        entityManager.persist(buffet);
        
        KiosqueroEntity vendor = new KiosqueroEntity();
        vendor.setCuit("123456789");
        vendor.setUsuario(vendorUser);
        vendor.setBuffetEntity(buffet);
        entityManager.persist(vendor);

        buffet.setVendedor(vendor);
        entityManager.merge(buffet);
        return buffet;
    }
    
    private InventarioBuffetEntity generateInventory(BuffetEntity buffet, ProductoEntity product, 
            int currentStock, int reservedStock, Integer minStock, EstadoInventario status, TipoManejoInventario managementType, Integer maxDailyQuota) {
        InventarioBuffetEntity inv = new InventarioBuffetEntity();
        inv.setBuffetEntity(buffet);
        inv.setProductoEntity(product);
        inv.setStockActual(currentStock);
        inv.setStockReservado(reservedStock);
        inv.setStockMinimo(minStock);
        inv.setEstado(status);
        inv.setTipoManejo(managementType);
        inv.setCupoMaximoDiario(maxDailyQuota);
        entityManager.persist(inv);
        return inv;
    }
    
    private CompraEntity generatePurchase(BuffetEntity buffet, ProductoEntity product, int quantity) {
        com.recred.backend.infrastructure.entities.CompraPresencialEntity purchase = new com.recred.backend.infrastructure.entities.CompraPresencialEntity();
        purchase.setBuffetEntity(buffet);
        purchase.setFechaHora(LocalDateTime.now());
        purchase.setEstado(com.recred.backend.domain.models.EstadoCompra.ENTREGADO);
        purchase.setMedioPago(com.recred.backend.domain.models.MedioPago.CREDITOS);
        purchase.setMontoTotal(product.getPrecio().multiply(new BigDecimal(quantity)));
        
        UsuarioEntity studentUser = new UsuarioEntity();
        studentUser.setIdCognito("alumn-" + UUID.randomUUID().toString().substring(0, 8));
        studentUser.setNombre("Student");
        studentUser.setApellido("Buyer");
        studentUser.setEmail("k@" + UUID.randomUUID().toString().substring(0, 5) + ".com");
        studentUser.setRol(RolUsuario.ALUMNO);
        studentUser.setActivo(true);
        entityManager.persist(studentUser);
        
        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(studentUser);
        student.setActivo(true);
        entityManager.persist(student);

        purchase.setAlumnoEntity(student);
        purchase.setTutorEntity(null);
        entityManager.persist(purchase);
        
        CompraDetalleEntity detail = new CompraDetalleEntity();
        detail.setCompra(purchase);
        detail.setProductoEntity(product);
        detail.setCantidad(quantity);
        detail.setPrecioUnitario(product.getPrecio());
        detail.setSubtotal(purchase.getMontoTotal());
        entityManager.persist(detail);
        
        return purchase;
    }
}
