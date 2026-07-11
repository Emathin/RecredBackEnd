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
class ProductRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private ProductRepositoryImpl productRepository;

    @Autowired
    private SpringDataProductRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByIdReturnsDomainObjectIfExists() {
        ProductoEntity entity = givenActiveProduct();
        Optional<Producto> result = whenFindingProductById(entity.getId());
        thenProductIsFoundWithCorrectData(result, entity.getId());
    }

    @Test
    void findByIdShouldReturnEmptyWhenNotExists() {
        Optional<Producto> result = whenFindingProductById(UUID.randomUUID());
        thenProductIsNotFound(result);
    }

    @Test
    void saveShouldCreateNewEntityWhenProductHasNoId() {
        Producto product = givenNewProductDomain();
        Producto result = whenSavingProduct(product);
        thenProductIsPersisted(result);
    }

    @Test
    void saveShouldUpdateExistingEntityWhenProductHasId() {
        Producto product = givenExistingProductForUpdate();
        Producto result = whenSavingProduct(product);
        thenProductIsUpdated(result);
    }

    @Test
    void saveShouldMapEverythingIncludingMeasurementsTypesAndClassifications() {
        Producto product = givenComplexProductDomain();
        Producto result = whenSavingProduct(product);
        thenComplexProductIsPersisted(result);
    }

    @Test
    void saveShouldClearCollectionsWhenEmpty() {
        Producto product = givenExistingProductWithEmptyCollections();
        Producto result = whenSavingProduct(product);
        thenCollectionsAreCleared(result);
    }

    @Test
    void findBuffetLeastSoldProductsReturnsAProduct() {
        BuffetEntity buffet = givenBuffetWithPurchasedProducts();
        ProductChangeRecommendation result = whenFindingLeastSoldProduct(buffet.getId());
        thenLeastSoldProductIsReturned(result, buffet.getId());
    }

    @Test
    void saveShouldNotReassignCategoryWhenIdIsEqual() {
        Producto product = givenExistingProductWithSameCategory();
        Producto result = whenSavingProduct(product);
        thenCategoryRemainsTheSame(result);
    }

    @Test
    void whenFindAllActiveWithStockIsCalledAndStockExistsThenMapsCorrectly() {
        BuffetEntity buffet = givenBuffetWithAvailableStockProduct();
        List<Producto> result = whenFindingAllActiveWithStock(buffet.getId());
        thenStockDataIsMappedCorrectly(result);
    }

    @Test
    void findAllActiveWithStockWhenNoMinimumAndFourRemainingShouldMapLowStock() {
        BuffetEntity buffet = givenBuffetWithLowStockProduct();
        List<Producto> result = whenFindingAllActiveWithStock(buffet.getId());
        thenProductStatusIsLowStock(result);
    }

    @Test
    void findAllActiveWithStockWhenNoStockShouldMapOutOfStock() {
        BuffetEntity buffet = givenBuffetWithOutOfStockProduct();
        List<Producto> result = whenFindingAllActiveWithStock(buffet.getId());
        thenProductStatusIsOutOfStock(result);
    }

    @Test
    void findActiveByIdWithStockWhenThereIsNoInventoryDataShouldReturnEmpty() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = givenActiveProduct();
        Optional<Producto> result = whenFindingActiveByIdWithStock(product.getId(), buffet.getId());
        thenProductIsNotFound(result);
    }

    @Test
    void whenFindActiveByIdWithStockIsCalledAndExistsThenMapsCorrectly() {
        BuffetEntity buffet = givenBuffetWithProductStock();
        Optional<Producto> result = whenFindingActiveByIdWithStockFromContext(buffet);
        thenSpecificStockDataIsMappedCorrectly(result);
    }

    @Test
    void findAllActiveReturnsMappedDomains() {
        ProductoEntity active = givenActiveAndInactiveProducts();
        List<Producto> result = whenFindingAllActiveProducts();
        thenOnlyActiveProductsAreReturned(result, active.getId());
    }

    @Test
    void findActiveByIdReturnsMappedDomain() {
        ProductoEntity active = givenActiveProduct();
        Optional<Producto> result = whenFindingActiveProductById(active.getId());
        thenProductIsFoundWithCorrectData(result, active.getId());
    }

    @Test
    void findBuffetTop5LeastSoldProductsMapsRecommendations() {
        BuffetEntity buffet = givenBuffetWithMultiplePurchasedProducts();
        List<ProductChangeRecommendation> result = whenFindingTop5LeastSoldProducts(buffet.getId());
        thenLeastSoldProductIsRankedFirst(result);
    }

    @Test
    void findBestSoldProductsNotInStockMapsDomains() {
        BuffetEntity buffet = givenBuffetWithPurchasedProductsNotInStock();
        List<Producto> result = whenFindingBestSoldProductsNotInStock(buffet.getId());
        thenBestSoldProductIsRankedFirst(result);
    }

    @Test
    void updatePriceDelegatesToSpringDataRepository() {
        ProductoEntity product = givenActiveProduct();
        whenUpdatingProductPrice(product.getId());
        thenProductPriceIsUpdated(product.getId());
    }

    @Test
    void findBuffetTop3MostSoldShouldMapToDomain() {
        BuffetEntity buffet = givenBuffetWithMostSoldProducts();
        List<Producto> result = whenFindingTop3MostSoldProducts(buffet.getId());
        thenMostSoldProductIsRankedFirst(result);
    }

    // --- Given Helpers ---

    private ProductoEntity givenActiveProduct() {
        return generateProduct("Test Product", true);
    }

    private Producto givenNewProductDomain() {
        CategoriaProductoEntity category = generateCategory("Category 1");
        return createDomainProduct(null, "New Product", "100", true, category.getId());
    }

    private Producto givenExistingProductForUpdate() {
        ProductoEntity existing = generateProduct("Old Product", true);
        CategoriaProductoEntity newCategory = generateCategory("Category 2");
        return createDomainProduct(existing.getId(), "Updated Product", "200", true, newCategory.getId());
    }

    private Producto givenComplexProductDomain() {
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
        
        return product;
    }

    private Producto givenExistingProductWithEmptyCollections() {
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
        return product;
    }

    private BuffetEntity givenBuffetWithPurchasedProducts() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product1 = generateProduct("Water", true);
        ProductoEntity product2 = generateProduct("Soda", true);
        
        generatePurchase(buffet, product1, 10);
        generatePurchase(buffet, product2, 1); // Least sold
        
        return buffet;
    }

    private Producto givenExistingProductWithSameCategory() {
        CategoriaProductoEntity category = generateCategory("Unique Category");
        ProductoEntity entity = generateProduct("Prod", true);
        entity.setCategoria(category);
        entityManager.persist(entity);
        return createDomainProduct(entity.getId(), "Mod", "10", true, category.getId());
    }

    private BuffetEntity givenBuffetWithAvailableStockProduct() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct("Cookie", true);
        generateInventory(buffet, product, 50, 5, 3, EstadoInventario.DISPONIBLE, TipoManejoInventario.STOCK_EXACTO, 20);
        return buffet;
    }

    private BuffetEntity givenBuffetWithLowStockProduct() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct("Gummy", true);
        generateInventory(buffet, product, 4, 0, null, EstadoInventario.BAJO_STOCK, TipoManejoInventario.STOCK_EXACTO, null);
        return buffet;
    }

    private BuffetEntity givenBuffetWithOutOfStockProduct() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct("Chocolate", true);
        generateInventory(buffet, product, 0, 0, null, EstadoInventario.SIN_STOCK, TipoManejoInventario.STOCK_EXACTO, null);
        return buffet;
    }

    private UUID cachedProductIdForStock;

    private BuffetEntity givenBuffetWithProductStock() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product = generateProduct("Candy", true);
        generateInventory(buffet, product, 10, 2, 1, EstadoInventario.DISPONIBLE, TipoManejoInventario.STOCK_EXACTO, null);
        cachedProductIdForStock = product.getId();
        return buffet;
    }

    private ProductoEntity givenActiveAndInactiveProducts() {
        ProductoEntity active = generateProduct("Active 1", true);
        generateProduct("Inactive 1", false);
        return active;
    }

    private UUID cachedLeastSoldProductId;

    private BuffetEntity givenBuffetWithMultiplePurchasedProducts() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product1 = generateProduct("Bread", true);
        ProductoEntity product2 = generateProduct("Water", true);
        ProductoEntity product3 = generateProduct("Apple", true); // Least sold
        
        generatePurchase(buffet, product1, 100);
        generatePurchase(buffet, product2, 50);
        generatePurchase(buffet, product3, 10);
        
        cachedLeastSoldProductId = product3.getId();
        return buffet;
    }

    private UUID cachedBestSoldProductId;

    private BuffetEntity givenBuffetWithPurchasedProductsNotInStock() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product1 = generateProduct("Top 1", true); // Best sold
        ProductoEntity product2 = generateProduct("Top 2", true);
        
        generatePurchase(buffet, product1, 500);
        generatePurchase(buffet, product2, 10);
        
        cachedBestSoldProductId = product1.getId();
        return buffet;
    }

    private UUID cachedMostSoldProductId;

    private BuffetEntity givenBuffetWithMostSoldProducts() {
        BuffetEntity buffet = generateBuffet();
        ProductoEntity product1 = generateProduct("Best Seller 1", true);
        ProductoEntity product2 = generateProduct("Best Seller 2", true);
        
        generateInventory(buffet, product1, 100, 0, 10, EstadoInventario.DISPONIBLE, TipoManejoInventario.STOCK_EXACTO, null);
        generateInventory(buffet, product2, 100, 0, 10, EstadoInventario.DISPONIBLE, TipoManejoInventario.STOCK_EXACTO, null);
        
        generatePurchase(buffet, product1, 100);
        generatePurchase(buffet, product2, 10);
        
        cachedMostSoldProductId = product1.getId();
        return buffet;
    }

    // --- When Helpers ---

    private Optional<Producto> whenFindingProductById(UUID id) {
        return productRepository.findById(id);
    }

    private Producto whenSavingProduct(Producto product) {
        Producto result = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private ProductChangeRecommendation whenFindingLeastSoldProduct(UUID buffetId) {
        return productRepository.findBuffetLeastSoldProducts(buffetId);
    }

    private List<Producto> whenFindingAllActiveWithStock(UUID buffetId) {
        return productRepository.findAllActiveWithStock(buffetId);
    }

    private Optional<Producto> whenFindingActiveByIdWithStock(UUID productId, UUID buffetId) {
        return productRepository.findActiveByIdWithStock(productId, buffetId);
    }

    private Optional<Producto> whenFindingActiveByIdWithStockFromContext(BuffetEntity buffet) {
        return productRepository.findActiveByIdWithStock(cachedProductIdForStock, buffet.getId());
    }

    private List<Producto> whenFindingAllActiveProducts() {
        return productRepository.findAllActive();
    }

    private Optional<Producto> whenFindingActiveProductById(UUID productId) {
        return productRepository.findActiveById(productId);
    }

    private List<ProductChangeRecommendation> whenFindingTop5LeastSoldProducts(UUID buffetId) {
        return productRepository.findBuffetTop5LeastSoldProducts(buffetId);
    }

    private List<Producto> whenFindingBestSoldProductsNotInStock(UUID buffetId) {
        return productRepository.findBestSoldProductsNotInStock(buffetId);
    }

    private void whenUpdatingProductPrice(UUID productId) {
        productRepository.updatePrice(productId, new BigDecimal("999.99"));
        entityManager.flush();
        entityManager.clear();
    }

    private List<Producto> whenFindingTop3MostSoldProducts(UUID buffetId) {
        return productRepository.findBuffetTop3MostSold(buffetId, UUID.randomUUID());
    }

    // --- Then Helpers ---

    private void thenProductIsFoundWithCorrectData(Optional<Producto> result, UUID expectedId) {
        assertTrue(result.isPresent());
        assertEquals(expectedId, result.get().getId());
    }

    private void thenProductIsNotFound(Optional<Producto> result) {
        assertTrue(result.isEmpty());
    }

    private void thenProductIsPersisted(Producto result) {
        assertNotNull(result.getId());
        assertTrue(springDataRepository.findById(result.getId()).isPresent());
    }

    private void thenProductIsUpdated(Producto result) {
        ProductoEntity updated = springDataRepository.findById(result.getId()).get();
        assertEquals("Updated Product", updated.getNombre());
    }

    private void thenComplexProductIsPersisted(Producto result) {
        ProductoEntity saved = springDataRepository.findById(result.getId()).get();
        assertNotNull(saved.getCategoria());
        assertFalse(saved.getMedidas().isEmpty());
        assertFalse(saved.getTipos().isEmpty());
        assertFalse(saved.getClasificacionesSalud().isEmpty());
    }

    private void thenCollectionsAreCleared(Producto result) {
        ProductoEntity saved = springDataRepository.findById(result.getId()).get();
        assertTrue(saved.getMedidas().isEmpty());
        assertTrue(saved.getTipos().isEmpty());
        assertTrue(saved.getClasificacionesSalud().isEmpty());
    }

    private void thenLeastSoldProductIsReturned(ProductChangeRecommendation result, UUID buffetId) {
        assertNotNull(result);
        assertEquals(buffetId, result.getIdBuffet());
    }

    private void thenCategoryRemainsTheSame(Producto result) {
        ProductoEntity saved = springDataRepository.findById(result.getId()).get();
        assertNotNull(saved.getCategoria().getId());
    }

    private void thenStockDataIsMappedCorrectly(List<Producto> result) {
        assertFalse(result.isEmpty());
        Producto p = result.get(0);
        assertEquals(50, p.getStockActual());
        assertEquals(5, p.getStockReservado());
        assertEquals(3, p.getStockMinimo());
        assertEquals(20, p.getCupoMaximoDiario());
        assertEquals(45, p.getStockDisponible());
    }

    private void thenProductStatusIsLowStock(List<Producto> result) {
        Producto p = result.get(0);
        assertEquals(EstadoInventario.BAJO_STOCK, p.getEstadoInventario());
    }

    private void thenProductStatusIsOutOfStock(List<Producto> result) {
        Producto p = result.get(0);
        assertEquals(EstadoInventario.SIN_STOCK, p.getEstadoInventario());
    }

    private void thenSpecificStockDataIsMappedCorrectly(Optional<Producto> result) {
        assertTrue(result.isPresent());
        assertEquals(10, result.get().getStockActual());
        assertEquals(8, result.get().getStockDisponible());
    }

    private void thenOnlyActiveProductsAreReturned(List<Producto> result, UUID expectedActiveId) {
        assertTrue(result.size() >= 1);
        assertTrue(result.stream().anyMatch(p -> p.getId().equals(expectedActiveId)));
        assertFalse(result.stream().anyMatch(p -> p.getNombre().equals("Inactive 1")));
    }

    private void thenLeastSoldProductIsRankedFirst(List<ProductChangeRecommendation> result) {
        assertFalse(result.isEmpty());
        assertEquals(cachedLeastSoldProductId, result.get(0).getId());
    }

    private void thenBestSoldProductIsRankedFirst(List<Producto> result) {
        assertFalse(result.isEmpty());
        assertEquals(cachedBestSoldProductId, result.get(0).getId());
    }

    private void thenProductPriceIsUpdated(UUID productId) {
        ProductoEntity updated = springDataRepository.findById(productId).get();
        assertEquals(new BigDecimal("999.99").compareTo(updated.getPrecio()), 0);
    }

    private void thenMostSoldProductIsRankedFirst(List<Producto> result) {
        assertFalse(result.isEmpty());
        assertEquals(cachedMostSoldProductId, result.get(0).getId());
    }

    // --- Entity Generators ---
    
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
