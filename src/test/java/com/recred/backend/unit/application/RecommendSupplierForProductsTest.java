package com.recred.backend.unit.application;

import com.recred.backend.application.RecommendSupplierForProducts;
import com.recred.backend.application.RecommendSupplierForProductsRequest;
import com.recred.backend.application.RecommendSupplierForProductsResponse;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendSupplierForProductsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 7, 10, 0);

    @Mock
    private ItemListaPrecioProveedorRepository itemRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductRepository productRepository;

    private RecommendSupplierForProducts useCase;

    private UUID kioskOwnerId;
    private UUID productId;
    private UUID firstSupplierId;
    private UUID secondSupplierId;
    private ListaPrecioProveedor firstPriceList;
    private ListaPrecioProveedor secondPriceList;
    private List<Proveedor> suppliers;
    private RecommendSupplierForProductsResponse response;
    private RecommendSupplierForProductsResponse.Recommendation recommendation;

    @BeforeEach
    void setUp() {
        useCase = new RecommendSupplierForProducts(
                itemRepository,
                proveedorRepository,
                productRepository
        );
    }

    @Test
    void givenMultipleSuppliers_whenRecommendingSupplier_thenCheapestSupplierIsRecommended() {
        givenSupplierRecommendationScenario();
        givenTwoSuppliers(
                "Proveedor A (Caro)",
                "Proveedor B (Barato)"
        );
        givenSupplierItems(
                item(firstPriceList.getId(), "600.00", "unidad"),
                item(secondPriceList.getId(), "450.00", "unidad")
        );

        whenRecommendingSupplier();

        thenOneRecommendationIsReturned();
        thenSupplierIsRecommended(secondSupplierId, "Proveedor B (Barato)");
        thenBestPriceIs("450.00");
        thenBestUnitPriceIs("450.00");
        thenAlternativeSupplierIs(firstSupplierId, "Proveedor A (Caro)");
        thenAlternativePriceIs("600.00");
        thenSuppliersAndItemsWereRequested();
    }

    @Test
    void givenPackHasLowerUnitPrice_whenRecommendingSupplier_thenPackSupplierIsRecommended() {
        givenSupplierRecommendationScenario();
        givenTwoSuppliers(
                "Proveedor A (Pack x6)",
                "Proveedor B (Unitario)"
        );
        givenSupplierItems(
                item(secondPriceList.getId(), "600.00", "unidad"),
                item(firstPriceList.getId(), "3000.00", "Pack de 6")
        );

        whenRecommendingSupplier();

        thenOneRecommendationIsReturned();
        thenSupplierIsRecommended(firstSupplierId, "Proveedor A (Pack x6)");
        thenBestPriceIs("3000.00");
        thenBestUnitPriceIs("500.00");
        thenAlternativeSupplierIs(secondSupplierId, "Proveedor B (Unitario)");
        thenAlternativePriceIs("600.00");
    }

    @Test
    void givenProductHasNoQuotes_whenRecommendingSupplier_thenReturnsEmptyRecommendation() {
        givenSupplierRecommendationScenario();
        givenNoSuppliers();
        givenSupplierItems();
        givenProductExists("Alfajor Triple");

        whenRecommendingSupplier();

        thenOneRecommendationIsReturned();
        thenRecommendationHasNoSupplier();
        thenRecommendationProductNameIs("Alfajor Triple");
        thenProductWasRequested();
    }

    @Test
    void givenRepeatedSupplierQuotes_whenRecommending_thenKeepsOneAlternativePerSupplier() {
        givenSupplierRecommendationScenario();
        givenTwoSuppliers(
                "Proveedor A (Barato)",
                "Proveedor B (Alternativa)"
        );
        ListaPrecioProveedor firstOldPriceList = olderPriceList(firstSupplierId);
        ListaPrecioProveedor secondOldPriceList = olderPriceList(secondSupplierId);
        givenSupplierHasPriceLists(
                firstSupplierId,
                "Proveedor A (Barato)",
                firstPriceList,
                firstOldPriceList
        );
        givenSupplierHasPriceLists(
                secondSupplierId,
                "Proveedor B (Alternativa)",
                secondPriceList,
                secondOldPriceList
        );
        givenSupplierItems(
                item(firstPriceList.getId(), "100.00", "unidad"),
                item(firstOldPriceList.getId(), "120.00", "unidad"),
                item(secondPriceList.getId(), "200.00", "unidad"),
                item(secondOldPriceList.getId(), "250.00", "unidad")
        );

        whenRecommendingSupplier();

        thenSupplierIsRecommended(firstSupplierId, "Proveedor A (Barato)");
        thenBestPriceIs("100.00");
        thenAlternativeSupplierIs(secondSupplierId, "Proveedor B (Alternativa)");
        thenAlternativePriceIs("200.00");
    }

    @Test
    void givenSupplierHasOldAndNewPriceList_whenRecommending_thenUsesLatestPriceList() {
        givenSupplierRecommendationScenario();
        firstSupplierId = UUID.randomUUID();
        firstPriceList = latestPriceList(firstSupplierId);
        ListaPrecioProveedor oldPriceList = olderPriceList(firstSupplierId);
        givenSupplierHasPriceLists(firstSupplierId, oldPriceList, firstPriceList);
        givenSupplierItems(
                item(oldPriceList.getId(), "100.00", "unidad"),
                item(firstPriceList.getId(), "150.00", "unidad")
        );

        whenRecommendingSupplier();

        thenSupplierIsRecommended(firstSupplierId, "Proveedor A");
        thenBestPriceIs("150.00");
        thenNoAlternativeSuppliersAreReturned();
    }

    private void givenSupplierRecommendationScenario() {
        kioskOwnerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        suppliers = new ArrayList<>();
    }

    private void givenTwoSuppliers(String firstSupplierName, String secondSupplierName) {
        firstSupplierId = UUID.randomUUID();
        secondSupplierId = UUID.randomUUID();
        firstPriceList = latestPriceList(firstSupplierId);
        secondPriceList = latestPriceList(secondSupplierId);
        givenSupplierHasPriceLists(firstSupplierId, firstSupplierName, firstPriceList);
        givenSupplierHasPriceLists(secondSupplierId, secondSupplierName, secondPriceList);
    }

    private void givenSupplierHasPriceLists(
            UUID supplierId,
            ListaPrecioProveedor... priceLists
    ) {
        givenSupplierHasPriceLists(supplierId, "Proveedor A", priceLists);
    }

    private void givenSupplierHasPriceLists(
            UUID supplierId,
            String supplierName,
            ListaPrecioProveedor... priceLists
    ) {
        Proveedor supplier = supplier(supplierId, supplierName);
        supplier.setListasPrecios(List.of(priceLists));
        suppliers.removeIf(existingSupplier -> existingSupplier.getId().equals(supplierId));
        suppliers.add(supplier);
    }

    private void givenNoSuppliers() {
        suppliers = List.of();
    }

    private void givenSupplierItems(ItemListaPrecioProveedor... items) {
        when(proveedorRepository.findByKiosqueroId(kioskOwnerId)).thenReturn(suppliers);
        when(itemRepository.findItemsActivosByKiosqueroIdAndProductoIds(
                kioskOwnerId,
                List.of(productId)
        )).thenReturn(List.of(items));
    }

    private void givenProductExists(String productName) {
        Producto product = new Producto();
        product.setId(productId);
        product.setNombre(productName);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    }

    private void whenRecommendingSupplier() {
        response = useCase.execute(request());
        recommendation = response.recommendations().get(0);
    }

    private void thenOneRecommendationIsReturned() {
        assertEquals(1, response.recommendations().size());
        assertEquals(productId, recommendation.inventoryProductId());
    }

    private void thenSupplierIsRecommended(UUID supplierId, String supplierName) {
        assertEquals(supplierId, recommendation.recommendedSupplierId());
        assertEquals(supplierName, recommendation.recommendedSupplierName());
    }

    private void thenBestPriceIs(String price) {
        assertEquals(new BigDecimal(price), recommendation.bestPrice());
    }

    private void thenBestUnitPriceIs(String price) {
        assertEquals(new BigDecimal(price), recommendation.bestUnitPrice());
    }

    private void thenAlternativeSupplierIs(UUID supplierId, String supplierName) {
        assertEquals(1, recommendation.alternatives().size());
        assertEquals(supplierId, recommendation.alternatives().get(0).supplierId());
        assertEquals(supplierName, recommendation.alternatives().get(0).supplierName());
    }

    private void thenAlternativePriceIs(String price) {
        assertEquals(new BigDecimal(price), recommendation.alternatives().get(0).price());
        assertEquals(new BigDecimal(price), recommendation.alternatives().get(0).unitPrice());
    }

    private void thenRecommendationHasNoSupplier() {
        assertNull(recommendation.recommendedSupplierId());
        assertEquals(0, recommendation.alternatives().size());
    }

    private void thenRecommendationProductNameIs(String productName) {
        assertEquals(productName, recommendation.productName());
    }

    private void thenNoAlternativeSuppliersAreReturned() {
        assertEquals(0, recommendation.alternatives().size());
    }

    private void thenSuppliersAndItemsWereRequested() {
        verify(proveedorRepository).findByKiosqueroId(kioskOwnerId);
        verify(itemRepository).findItemsActivosByKiosqueroIdAndProductoIds(
                kioskOwnerId,
                List.of(productId)
        );
    }

    private void thenProductWasRequested() {
        verify(productRepository).findById(productId);
    }

    private RecommendSupplierForProductsRequest request() {
        return new RecommendSupplierForProductsRequest(kioskOwnerId, List.of(productId));
    }

    private Proveedor supplier(UUID supplierId, String name) {
        Proveedor supplier = new Proveedor();
        supplier.setId(supplierId);
        supplier.setNombre(name);
        return supplier;
    }

    private ListaPrecioProveedor latestPriceList(UUID supplierId) {
        return priceList(supplierId, NOW);
    }

    private ListaPrecioProveedor olderPriceList(UUID supplierId) {
        return priceList(supplierId, NOW.minusDays(1));
    }

    private ListaPrecioProveedor priceList(UUID supplierId, LocalDateTime createdAt) {
        ListaPrecioProveedor priceList = new ListaPrecioProveedor();
        priceList.setId(UUID.randomUUID());
        priceList.setProveedorId(supplierId);
        priceList.setCreadoEn(createdAt);
        return priceList;
    }

    private ItemListaPrecioProveedor item(UUID priceListId, String price, String unit) {
        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setId(UUID.randomUUID());
        item.setListaPrecioId(priceListId);
        item.setProductoInventarioId(productId);
        item.setNombreProductoInventario("Alfajor Triple");
        item.setPrecio(new BigDecimal(price));
        item.setUnidad(unit);
        return item;
    }

}
