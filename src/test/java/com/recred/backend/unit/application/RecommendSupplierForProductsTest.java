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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendSupplierForProductsTest {

    @Mock
    private ItemListaPrecioProveedorRepository itemRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private RecommendSupplierForProducts recommendSupplierForProducts;

    private UUID kioskOwnerId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        kioskOwnerId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    void executeWhenMultipleSuppliersExistRecommendsCheapestAndGroupsAlternatives() {
        UUID expensiveSupplierId = UUID.randomUUID();
        Proveedor expensiveSupplier = supplier(expensiveSupplierId, "Proveedor A (Caro)");
        ListaPrecioProveedor expensivePriceList = priceList(expensiveSupplierId);
        expensiveSupplier.setListasPrecios(List.of(expensivePriceList));

        UUID cheapSupplierId = UUID.randomUUID();
        Proveedor cheapSupplier = supplier(cheapSupplierId, "Proveedor B (Barato)");
        ListaPrecioProveedor cheapPriceList = priceList(cheapSupplierId);
        cheapSupplier.setListasPrecios(List.of(cheapPriceList));

        when(proveedorRepository.findByKiosqueroId(kioskOwnerId))
                .thenReturn(List.of(expensiveSupplier, cheapSupplier));
        when(itemRepository.findItemsActivosByKiosqueroIdAndProductoIds(
                kioskOwnerId,
                List.of(productId)
        )).thenReturn(List.of(
                item(expensivePriceList.getId(), new BigDecimal("600.00"), "unidad"),
                item(cheapPriceList.getId(), new BigDecimal("450.00"), "unidad")
        ));

        RecommendSupplierForProductsResponse result =
                recommendSupplierForProducts.execute(request(productId));

        assertNotNull(result);
        assertEquals(1, result.recommendations().size());

        RecommendSupplierForProductsResponse.Recommendation recommendation =
                result.recommendations().get(0);
        assertEquals(productId, recommendation.inventoryProductId());
        assertEquals(cheapSupplierId, recommendation.recommendedSupplierId());
        assertEquals("Proveedor B (Barato)", recommendation.recommendedSupplierName());
        assertEquals(new BigDecimal("450.00"), recommendation.bestPrice());
        assertEquals(new BigDecimal("450.00"), recommendation.bestUnitPrice());
        assertEquals(1, recommendation.alternatives().size());
        assertEquals(expensiveSupplierId, recommendation.alternatives().get(0).supplierId());
        assertEquals(
                "Proveedor A (Caro)",
                recommendation.alternatives().get(0).supplierName()
        );
        assertEquals(new BigDecimal("600.00"), recommendation.alternatives().get(0).price());
        assertEquals(
                new BigDecimal("600.00"),
                recommendation.alternatives().get(0).unitPrice()
        );

        verify(proveedorRepository).findByKiosqueroId(kioskOwnerId);
        verify(itemRepository).findItemsActivosByKiosqueroIdAndProductoIds(
                kioskOwnerId,
                List.of(productId)
        );
    }

    @Test
    void executeWhenPackHasLowerUnitPriceRecommendsPackSupplier() {
        UUID packSupplierId = UUID.randomUUID();
        Proveedor packSupplier = supplier(packSupplierId, "Proveedor A (Pack x6)");
        ListaPrecioProveedor packPriceList = priceList(packSupplierId);
        packSupplier.setListasPrecios(List.of(packPriceList));

        UUID unitSupplierId = UUID.randomUUID();
        Proveedor unitSupplier = supplier(unitSupplierId, "Proveedor B (Unitario)");
        ListaPrecioProveedor unitPriceList = priceList(unitSupplierId);
        unitSupplier.setListasPrecios(List.of(unitPriceList));

        when(proveedorRepository.findByKiosqueroId(kioskOwnerId))
                .thenReturn(List.of(packSupplier, unitSupplier));
        when(itemRepository.findItemsActivosByKiosqueroIdAndProductoIds(
                kioskOwnerId,
                List.of(productId)
        )).thenReturn(List.of(
                item(unitPriceList.getId(), new BigDecimal("600.00"), "unidad"),
                item(packPriceList.getId(), new BigDecimal("3000.00"), "Pack de 6")
        ));

        RecommendSupplierForProductsResponse result =
                recommendSupplierForProducts.execute(request(productId));

        assertNotNull(result);
        assertEquals(1, result.recommendations().size());

        RecommendSupplierForProductsResponse.Recommendation recommendation =
                result.recommendations().get(0);
        assertEquals(productId, recommendation.inventoryProductId());
        assertEquals(packSupplierId, recommendation.recommendedSupplierId());
        assertEquals("Proveedor A (Pack x6)", recommendation.recommendedSupplierName());
        assertEquals(new BigDecimal("3000.00"), recommendation.bestPrice());
        assertEquals(new BigDecimal("500.00"), recommendation.bestUnitPrice());
        assertEquals(1, recommendation.alternatives().size());
        assertEquals(unitSupplierId, recommendation.alternatives().get(0).supplierId());
        assertEquals(new BigDecimal("600.00"), recommendation.alternatives().get(0).price());
        assertEquals(
                new BigDecimal("600.00"),
                recommendation.alternatives().get(0).unitPrice()
        );
    }

    @Test
    void executeWhenProductHasNoQuotesReturnsEmptyRecommendationWithProductName() {
        when(proveedorRepository.findByKiosqueroId(kioskOwnerId)).thenReturn(List.of());
        when(itemRepository.findItemsActivosByKiosqueroIdAndProductoIds(
                kioskOwnerId,
                List.of(productId)
        )).thenReturn(List.of());

        Producto product = new Producto();
        product.setId(productId);
        product.setNombre("Alfajor Triple");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        RecommendSupplierForProductsResponse result =
                recommendSupplierForProducts.execute(request(productId));

        assertNotNull(result);
        assertEquals(1, result.recommendations().size());

        RecommendSupplierForProductsResponse.Recommendation recommendation =
                result.recommendations().get(0);
        assertEquals(productId, recommendation.inventoryProductId());
        assertEquals("Alfajor Triple", recommendation.productName());
        assertNull(recommendation.recommendedSupplierId());
        assertEquals(0, recommendation.alternatives().size());

        verify(proveedorRepository).findByKiosqueroId(kioskOwnerId);
        verify(itemRepository).findItemsActivosByKiosqueroIdAndProductoIds(
                kioskOwnerId,
                List.of(productId)
        );
        verify(productRepository).findById(productId);
    }

    private RecommendSupplierForProductsRequest request(UUID productId) {
        return new RecommendSupplierForProductsRequest(kioskOwnerId, List.of(productId));
    }

    private Proveedor supplier(UUID supplierId, String name) {
        Proveedor supplier = new Proveedor();
        supplier.setId(supplierId);
        supplier.setNombre(name);
        return supplier;
    }

    private ListaPrecioProveedor priceList(UUID supplierId) {
        ListaPrecioProveedor priceList = new ListaPrecioProveedor();
        priceList.setId(UUID.randomUUID());
        priceList.setProveedorId(supplierId);
        return priceList;
    }

    private ItemListaPrecioProveedor item(
            UUID priceListId,
            BigDecimal price,
            String unit
    ) {
        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setId(UUID.randomUUID());
        item.setListaPrecioId(priceListId);
        item.setProductoInventarioId(productId);
        item.setNombreProductoInventario("Alfajor Triple");
        item.setPrecio(price);
        item.setUnidad(unit);
        return item;
    }
}
