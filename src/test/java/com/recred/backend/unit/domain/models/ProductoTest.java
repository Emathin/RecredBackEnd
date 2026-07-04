package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.ProductoMedida;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.TipoManejoInventario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductoTest {

    @Test
    void addMedida_DeberiaAgregarMedidaYVincularProducto() {
        Producto producto = new Producto();
        ProductoMedida medida = new ProductoMedida();

        producto.addMedida(medida);

        assertTrue(producto.getMedidas().contains(medida));
        assertEquals(producto, medida.getProducto());
    }

    @Test
    void addMedida_NoDeberiaHacerNada_CuandoMedidaEsNula() {
        Producto producto = new Producto();
        producto.addMedida(null);
        assertTrue(producto.getMedidas().isEmpty());
    }

    @Test
    void removeMedida_DeberiaQuitarMedidaYDesvincularProducto() {
        Producto producto = new Producto();
        ProductoMedida medida = new ProductoMedida();
        producto.addMedida(medida);

        producto.removeMedida(medida);

        assertFalse(producto.getMedidas().contains(medida));
        assertNull(medida.getProducto());
    }

    @Test
    void removeMedida_NoDeberiaHacerNada_CuandoMedidaEsNula() {
        Producto producto = new Producto();
        producto.removeMedida(null);
        assertTrue(producto.getMedidas().isEmpty());
    }

    @Test
    void getClasificaciones_DeberiaRetornarSetDeClasificaciones() {
        Producto producto = new Producto();
        Set<ClasificacionSalud> clasificaciones = new HashSet<>();
        ClasificacionSalud cs = new ClasificacionSalud();
        clasificaciones.add(cs);
        producto.setClasificacionesSalud(clasificaciones);

        assertEquals(clasificaciones, producto.getClasificaciones());
    }

    @Test
    void testGettersAndSetters() {
        Producto producto = new Producto();
        UUID id = UUID.randomUUID();
        CategoriaProducto cat = new CategoriaProducto();
        BigDecimal precio = new BigDecimal("100.00");
        BigDecimal peso = new BigDecimal("1.0");

        producto.setId(id);
        producto.setNombre("Test");
        producto.setDescripcion("Desc");
        producto.setPrecio(precio);
        producto.setPeso(peso);
        producto.setRequierePreparacion(true);
        producto.setActivo(false);
        producto.setStockActual(10);

        assertEquals(id, producto.getId());
        assertEquals("Test", producto.getNombre());
        assertEquals("Desc", producto.getDescripcion());
        assertEquals(precio, producto.getPrecio());
        assertEquals(peso, producto.getPeso());
        assertTrue(producto.getRequierePreparacion());
        assertFalse(producto.getActivo());
        assertEquals(10, producto.getStockActual());
    }

    @Test
    void hasInventoryData_ShouldReturnTrueWhenProductHasInventoryFields() {
        Producto producto = new Producto();

        assertFalse(producto.hasInventoryData());

        producto.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);

        assertTrue(producto.hasInventoryData());
    }

    @Test
    void needsRestock_ShouldReturnTrueForLowStockOrSoldOutProducts() {
        Producto lowStock = new Producto();
        lowStock.setStockActual(2);
        lowStock.setStockDisponible(1);
        lowStock.setStockMinimo(2);
        Producto soldOut = new Producto();
        soldOut.setEstadoInventario(EstadoInventario.SIN_STOCK);
        Producto available = new Producto();
        available.setStockActual(10);
        available.setStockDisponible(8);
        available.setStockMinimo(2);
        available.setEstadoInventario(EstadoInventario.DISPONIBLE);

        assertTrue(lowStock.hasLowStock());
        assertTrue(lowStock.needsRestock());
        assertTrue(soldOut.isSoldOut());
        assertTrue(soldOut.needsRestock());
        assertFalse(available.needsRestock());
    }

    @Test
    void isSoldOut_ShouldReturnTrueForDisabledOrWithoutAvailableStock() {
        Producto disabled = new Producto();
        disabled.setEstadoInventario(EstadoInventario.DESACTIVADO);
        Producto withoutAvailableStock = new Producto();
        withoutAvailableStock.setStockDisponible(0);
        withoutAvailableStock.setEstadoInventario(EstadoInventario.DISPONIBLE);

        assertTrue(disabled.isSoldOut());
        assertTrue(withoutAvailableStock.isSoldOut());
    }

    @Test
    void reservedStockOrZeroAndPriceOrZero_ShouldNormalizeNullableValues() {
        Producto producto = new Producto();

        assertEquals(0, producto.reservedStockOrZero());
        assertEquals(BigDecimal.ZERO, producto.priceOrZero());

        producto.setStockReservado(4);
        producto.setPrecio(new BigDecimal("15.00"));

        assertEquals(4, producto.reservedStockOrZero());
        assertEquals(new BigDecimal("15.00"), producto.priceOrZero());
    }

    @Test
    void inventoryMode_WhenModeIsMissing_ShouldDefaultToExactStock() {
        Producto producto = new Producto();

        assertEquals(TipoManejoInventario.STOCK_EXACTO, producto.inventoryMode());
    }

    @Test
    void dailyCapacity_WhenProductUsesDailyCapacity_ShouldExposeConfiguredValues() {
        Producto producto = new Producto();
        producto.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        producto.setStockActual(20);
        producto.setStockReservado(5);
        producto.setCupoMaximoDiario(null);

        assertEquals(20, producto.configuredDailyCapacity());
        assertEquals(15, producto.dailyCapacityAvailable());
        assertFalse(producto.hasDailyCapacityLimit());
        producto.setCupoMaximoDiario(20);

        assertTrue(producto.hasDailyCapacityLimit());
        assertTrue(producto.isAvailableForSale());
    }

    @Test
    void stockStatusHelpers_ShouldExposeSellerInventoryState() {
        Producto producto = new Producto();
        producto.setEstadoInventario(EstadoInventario.BAJO_STOCK);

        assertTrue(producto.hasLowStockStatus());
    }

    @Test
    void activeAndStockHelpers_ShouldExposeAvailabilityStates() {
        Producto producto = new Producto();

        assertTrue(producto.isActive());
        producto.deactivate();

        assertFalse(producto.isActive());
        assertNull(producto.getStockDisponible());
        assertFalse(producto.hasAvailableStock());

        producto.setStockActual(5);
        producto.setStockReservado(2);

        assertEquals(3, producto.getStockDisponible());
        assertTrue(producto.hasAvailableStock());

        producto.setStockReservado(10);

        assertEquals(0, producto.getStockDisponible());
        assertTrue(producto.isSoldOut());
        assertFalse(producto.isAvailableForSale());
    }

    @Test
    void dailyCapacity_ShouldApplyReservationsAndDetectExhaustion() {
        Producto exactStock = new Producto();
        exactStock.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);
        exactStock.setCupoMaximoDiario(10);

        assertNull(exactStock.configuredDailyCapacity());
        assertNull(exactStock.dailyCapacityAvailable());
        exactStock.applyDailyCapacity(3);
        assertNull(exactStock.getCupoDisponibleDia());

        Producto dailyCapacity = new Producto();
        dailyCapacity.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        dailyCapacity.setCupoMaximoDiario(10);

        dailyCapacity.applyDailyCapacity(4);

        assertEquals(6, dailyCapacity.getCupoDisponibleDia());
        assertFalse(dailyCapacity.isDailyCapacityExhausted());

        dailyCapacity.applyDailyCapacity(12);

        assertEquals(0, dailyCapacity.getCupoDisponibleDia());
        assertTrue(dailyCapacity.isDailyCapacityExhausted());
    }

    @Test
    void priceAndCategoryBudget_ShouldCompareAgainstAvailableAmounts() {
        UUID categoryId = UUID.randomUUID();
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        category.setDescripcion("Golosinas");
        Producto producto = new Producto();
        producto.setCategoria(category);
        producto.setPrecio(new BigDecimal("100.00"));

        assertFalse(producto.exceedsPrice(null));
        assertFalse(producto.exceedsPrice(new BigDecimal("150.00")));
        assertTrue(producto.exceedsPrice(new BigDecimal("50.00")));
        assertFalse(producto.exceedsCategoryBudget(null));
        assertFalse(new Producto().exceedsCategoryBudget(Map.of(categoryId,
                new BigDecimal("50.00"))));
        assertTrue(producto.exceedsCategoryBudget(Map.of(categoryId,
                new BigDecimal("50.00"))));
    }

    @Test
    void categoryDescriptionAndHealthClassification_ShouldHandleNullsAndMatches() {
        UUID classificationId = UUID.randomUUID();
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(classificationId);
        CategoriaProducto category = new CategoriaProducto();
        category.setDescripcion("Bebidas frias");
        Producto producto = new Producto();
        producto.setCategoria(category);
        producto.setClasificacionesSalud(Set.of(classification));

        assertFalse(producto.matchesAnyCategoryDescription(null));
        assertFalse(producto.matchesAnyCategoryDescription(List.of()));
        assertFalse(producto.matchesCategoryDescription(null));
        assertTrue(producto.matchesAnyCategoryDescription(List.of("frias")));
        assertTrue(producto.matchesCategoryDescription("BEBIDAS"));
        assertFalse(producto.hasHealthClassification(null));
        assertFalse(producto.hasHealthClassification(new ClasificacionSalud()));
        assertTrue(producto.hasHealthClassification(classification));

        producto.setClasificacionesSalud(null);

        assertFalse(producto.hasHealthClassification(classification));
    }

    @Test
    void purchaseIdentityCopyAndDiscount_ShouldUseProductPriceAndPromotion() {
        UUID productId = UUID.randomUUID();
        UUID promotionId = UUID.randomUUID();
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setName("Promo");
        promotion.setDiscountPercentage(new BigDecimal("25.00"));
        Producto producto = new Producto();
        producto.setId(productId);
        producto.setNombre("Alfajor");
        producto.setPrecio(new BigDecimal("100.00"));
        producto.setStockActual(10);
        producto.setStockReservado(1);
        producto.setCupoDisponibleDia(4);

        Producto discounted = producto.discountedFor(promotion);
        Producto copied = producto.copyForPurchase();

        assertEquals(productId.toString(), producto.idAsString());
        assertEquals(new BigDecimal("75.00"), discounted.getPrecio());
        assertEquals(promotionId, discounted.getPromocionId());
        assertEquals("Promo", discounted.getPromocionNombre());
        assertEquals(producto.getNombre(), copied.getNombre());
        assertEquals(producto.getStockActual(), copied.getStockActual());
        Producto samePurchaseProduct = new Producto();
        samePurchaseProduct.setId(productId);
        samePurchaseProduct.setPrecio(new BigDecimal("100.00"));
        assertTrue(producto.hasSamePurchaseIdentityAs(samePurchaseProduct));
        samePurchaseProduct.setPromocionId(UUID.randomUUID());
        assertFalse(producto.hasSamePurchaseIdentityAs(samePurchaseProduct));
        assertFalse(producto.hasSamePurchaseIdentityAs(null));
        producto.setId(null);
        assertNull(producto.idAsString());
    }
}
