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
    void dadoProductoConStockBajo_cuandoNeedsRestock_entoncesRetornaTrue() {
        Producto lowStock = new Producto();
        lowStock.setStockActual(2);
        lowStock.setStockDisponible(1);
        lowStock.setStockMinimo(2);

        assertTrue(lowStock.hasLowStock());
        assertTrue(lowStock.needsRestock());
    }

    @Test
    void dadoProductoSinStock_cuandoNeedsRestock_entoncesRetornaTrue() {
        Producto soldOut = new Producto();
        soldOut.setEstadoInventario(EstadoInventario.SIN_STOCK);

        assertTrue(soldOut.isSoldOut());
        assertTrue(soldOut.needsRestock());
    }

    @Test
    void dadoProductoDisponible_cuandoNeedsRestock_entoncesRetornaFalse() {
        Producto available = new Producto();
        available.setStockActual(10);
        available.setStockDisponible(8);
        available.setStockMinimo(2);
        available.setEstadoInventario(EstadoInventario.DISPONIBLE);

        assertFalse(available.needsRestock());
    }

    @Test
    void dadoProductoDesactivado_cuandoIsSoldOut_entoncesRetornaTrue() {
        Producto disabled = new Producto();
        disabled.setEstadoInventario(EstadoInventario.DESACTIVADO);

        assertTrue(disabled.isSoldOut());
    }

    @Test
    void dadoProductoSinStockDisponible_cuandoIsSoldOut_entoncesRetornaTrue() {
        Producto withoutAvailableStock = new Producto();
        withoutAvailableStock.setStockDisponible(0);
        withoutAvailableStock.setEstadoInventario(EstadoInventario.DISPONIBLE);

        assertTrue(withoutAvailableStock.isSoldOut());
    }

    @Test
    void dadoStockReservadoYPrecioNulos_cuandoNormalizaValores_entoncesRetornaCeros() {
        Producto producto = new Producto();

        assertEquals(0, producto.reservedStockOrZero());
        assertEquals(BigDecimal.ZERO, producto.priceOrZero());
    }

    @Test
    void dadoStockReservadoYPrecioConfigurados_cuandoNormalizaValores_entoncesRetornaValores() {
        Producto producto = new Producto();
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
    void dadoProductoConCupoDiarioSinLimite_cuandoConsultaCupo_entoncesUsaStockActual() {
        Producto producto = new Producto();
        producto.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        producto.setStockActual(20);
        producto.setStockReservado(5);
        producto.setCupoMaximoDiario(null);

        assertEquals(20, producto.configuredDailyCapacity());
        assertEquals(15, producto.dailyCapacityAvailable());
        assertFalse(producto.hasDailyCapacityLimit());
    }

    @Test
    void dadoProductoConCupoDiarioConLimite_cuandoConsultaCupo_entoncesTieneLimiteYEstaDisponible() {
        Producto producto = new Producto();
        producto.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
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
    void dadoProductoNuevo_cuandoIsActive_entoncesRetornaTrue() {
        Producto producto = new Producto();

        assertTrue(producto.isActive());
    }

    @Test
    void dadoProductoActivo_cuandoDeactivate_entoncesQuedaInactivo() {
        Producto producto = new Producto();

        producto.deactivate();

        assertFalse(producto.isActive());
    }

    @Test
    void dadoProductoSinStockActual_cuandoConsultaStockDisponible_entoncesNoTieneStockDisponible() {
        Producto producto = new Producto();

        assertNull(producto.getStockDisponible());
        assertFalse(producto.hasAvailableStock());
    }

    @Test
    void dadoProductoConStockYReservaMenor_cuandoConsultaStockDisponible_entoncesCalculaDisponible() {
        Producto producto = new Producto();
        producto.setStockActual(5);
        producto.setStockReservado(2);

        assertEquals(3, producto.getStockDisponible());
        assertTrue(producto.hasAvailableStock());
    }

    @Test
    void dadoProductoConReservaMayorAlStock_cuandoConsultaDisponibilidad_entoncesQuedaSinStock() {
        Producto producto = new Producto();
        producto.setStockActual(5);
        producto.setStockReservado(10);

        assertEquals(0, producto.getStockDisponible());
        assertTrue(producto.isSoldOut());
        assertFalse(producto.isAvailableForSale());
    }

    @Test
    void dadoProductoConStockExacto_cuandoAplicaCupoDiario_entoncesIgnoraCupo() {
        Producto exactStock = new Producto();
        exactStock.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);
        exactStock.setCupoMaximoDiario(10);

        assertNull(exactStock.configuredDailyCapacity());
        assertNull(exactStock.dailyCapacityAvailable());
        exactStock.applyDailyCapacity(3);
        assertNull(exactStock.getCupoDisponibleDia());
    }

    @Test
    void dadoProductoConCupoDiario_cuandoAplicaReservaMenorAlLimite_entoncesCalculaDisponible() {
        Producto dailyCapacity = new Producto();
        dailyCapacity.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        dailyCapacity.setCupoMaximoDiario(10);

        dailyCapacity.applyDailyCapacity(4);

        assertEquals(6, dailyCapacity.getCupoDisponibleDia());
        assertFalse(dailyCapacity.isDailyCapacityExhausted());
    }

    @Test
    void dadoProductoConCupoDiario_cuandoAplicaReservaMayorAlLimite_entoncesAgotaCupo() {
        Producto dailyCapacity = new Producto();
        dailyCapacity.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        dailyCapacity.setCupoMaximoDiario(10);

        dailyCapacity.applyDailyCapacity(12);

        assertEquals(0, dailyCapacity.getCupoDisponibleDia());
        assertTrue(dailyCapacity.isDailyCapacityExhausted());
    }

    @Test
    void dadoMontoDisponibleNulo_cuandoExceedsPrice_entoncesRetornaFalse() {
        Producto producto = productoConPrecio(new BigDecimal("100.00"));

        assertFalse(producto.exceedsPrice(null));
    }

    @Test
    void dadoMontoDisponibleMayorAlPrecio_cuandoExceedsPrice_entoncesRetornaFalse() {
        Producto producto = productoConPrecio(new BigDecimal("100.00"));

        assertFalse(producto.exceedsPrice(new BigDecimal("150.00")));
    }

    @Test
    void dadoMontoDisponibleMenorAlPrecio_cuandoExceedsPrice_entoncesRetornaTrue() {
        Producto producto = productoConPrecio(new BigDecimal("100.00"));

        assertTrue(producto.exceedsPrice(new BigDecimal("50.00")));
    }

    @Test
    void dadoPresupuestoPorCategoriaNulo_cuandoExceedsCategoryBudget_entoncesRetornaFalse() {
        Producto producto = productoConCategoriaYPrecio(UUID.randomUUID(), new BigDecimal("100.00"));

        assertFalse(producto.exceedsCategoryBudget(null));
    }

    @Test
    void dadoProductoSinCategoria_cuandoExceedsCategoryBudget_entoncesRetornaFalse() {
        UUID categoryId = UUID.randomUUID();

        assertFalse(new Producto().exceedsCategoryBudget(Map.of(categoryId, new BigDecimal("50.00"))));
    }

    @Test
    void dadoPresupuestoDeCategoriaMenorAlPrecio_cuandoExceedsCategoryBudget_entoncesRetornaTrue() {
        UUID categoryId = UUID.randomUUID();
        Producto producto = productoConCategoriaYPrecio(categoryId, new BigDecimal("100.00"));

        assertTrue(producto.exceedsCategoryBudget(Map.of(categoryId, new BigDecimal("50.00"))));
    }

    @Test
    void dadoDescripcionesNulas_cuandoMatchesAnyCategoryDescription_entoncesRetornaFalse() {
        Producto producto = productoConCategoria("Bebidas frias");

        assertFalse(producto.matchesAnyCategoryDescription(null));
    }

    @Test
    void dadoDescripcionesVacias_cuandoMatchesAnyCategoryDescription_entoncesRetornaFalse() {
        Producto producto = productoConCategoria("Bebidas frias");

        assertFalse(producto.matchesAnyCategoryDescription(List.of()));
    }

    @Test
    void dadoDescripcionNula_cuandoMatchesCategoryDescription_entoncesRetornaFalse() {
        Producto producto = productoConCategoria("Bebidas frias");

        assertFalse(producto.matchesCategoryDescription(null));
    }

    @Test
    void dadoDescripcionParcial_cuandoMatchesAnyCategoryDescription_entoncesRetornaTrue() {
        Producto producto = productoConCategoria("Bebidas frias");

        assertTrue(producto.matchesAnyCategoryDescription(List.of("frias")));
    }

    @Test
    void dadoDescripcionConDistintasMayusculas_cuandoMatchesCategoryDescription_entoncesRetornaTrue() {
        Producto producto = productoConCategoria("Bebidas frias");

        assertTrue(producto.matchesCategoryDescription("BEBIDAS"));
    }

    @Test
    void dadoClasificacionNula_cuandoHasHealthClassification_entoncesRetornaFalse() {
        Producto producto = productoConClasificacion(clasificacionConId(UUID.randomUUID()));

        assertFalse(producto.hasHealthClassification(null));
    }

    @Test
    void dadoClasificacionSinId_cuandoHasHealthClassification_entoncesRetornaFalse() {
        Producto producto = productoConClasificacion(clasificacionConId(UUID.randomUUID()));

        assertFalse(producto.hasHealthClassification(new ClasificacionSalud()));
    }

    @Test
    void dadoClasificacionExistente_cuandoHasHealthClassification_entoncesRetornaTrue() {
        ClasificacionSalud classification = clasificacionConId(UUID.randomUUID());
        Producto producto = productoConClasificacion(classification);

        assertTrue(producto.hasHealthClassification(classification));
    }

    @Test
    void dadoProductoSinClasificaciones_cuandoHasHealthClassification_entoncesRetornaFalse() {
        ClasificacionSalud classification = clasificacionConId(UUID.randomUUID());
        Producto producto = productoConClasificacion(classification);
        producto.setClasificacionesSalud(null);

        assertFalse(producto.hasHealthClassification(classification));
    }

    @Test
    void dadoProductoConId_cuandoIdAsString_entoncesRetornaIdComoTexto() {
        UUID productId = UUID.randomUUID();
        Producto producto = productoParaCompra(productId);

        assertEquals(productId.toString(), producto.idAsString());
    }

    @Test
    void dadoProductoSinId_cuandoIdAsString_entoncesRetornaNull() {
        Producto producto = new Producto();

        assertNull(producto.idAsString());
    }

    @Test
    void dadoProductoYPromocion_cuandoDiscountedFor_entoncesAplicaDescuentoYPromocion() {
        UUID promotionId = UUID.randomUUID();
        Producto producto = productoParaCompra(UUID.randomUUID());
        Promotion promotion = promotion(promotionId);

        Producto discounted = producto.discountedFor(promotion);

        assertEquals(new BigDecimal("75.00"), discounted.getPrecio());
        assertEquals(promotionId, discounted.getPromocionId());
        assertEquals("Promo", discounted.getPromocionNombre());
    }

    @Test
    void dadoProducto_cuandoCopyForPurchase_entoncesCopiaCamposDeCompra() {
        Producto producto = productoParaCompra(UUID.randomUUID());

        Producto copied = producto.copyForPurchase();

        assertEquals(producto.getNombre(), copied.getNombre());
        assertEquals(producto.getStockActual(), copied.getStockActual());
    }

    @Test
    void dadoProductoConMismaIdentidadDeCompra_cuandoHasSamePurchaseIdentityAs_entoncesRetornaTrue() {
        UUID productId = UUID.randomUUID();
        Producto producto = productoParaCompra(productId);
        Producto samePurchaseProduct = productoConPrecio(new BigDecimal("100.00"));
        samePurchaseProduct.setId(productId);

        assertTrue(producto.hasSamePurchaseIdentityAs(samePurchaseProduct));
    }

    @Test
    void dadoProductoConDistintaPromocion_cuandoHasSamePurchaseIdentityAs_entoncesRetornaFalse() {
        UUID productId = UUID.randomUUID();
        Producto producto = productoParaCompra(productId);
        Producto samePurchaseProduct = productoConPrecio(new BigDecimal("100.00"));
        samePurchaseProduct.setId(productId);
        samePurchaseProduct.setPromocionId(UUID.randomUUID());

        assertFalse(producto.hasSamePurchaseIdentityAs(samePurchaseProduct));
    }

    @Test
    void dadoProductoNulo_cuandoHasSamePurchaseIdentityAs_entoncesRetornaFalse() {
        Producto producto = productoParaCompra(UUID.randomUUID());

        assertFalse(producto.hasSamePurchaseIdentityAs(null));
    }

    private Producto productoConPrecio(BigDecimal precio) {
        Producto producto = new Producto();
        producto.setPrecio(precio);
        return producto;
    }

    private Producto productoConCategoriaYPrecio(UUID categoryId, BigDecimal precio) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        category.setDescripcion("Golosinas");
        Producto producto = new Producto();
        producto.setCategoria(category);
        producto.setPrecio(precio);
        return producto;
    }

    private Producto productoConCategoria(String descripcion) {
        CategoriaProducto category = new CategoriaProducto();
        category.setDescripcion(descripcion);
        Producto producto = new Producto();
        producto.setCategoria(category);
        return producto;
    }

    private Producto productoConClasificacion(ClasificacionSalud classification) {
        Producto producto = new Producto();
        producto.setClasificacionesSalud(Set.of(classification));
        return producto;
    }

    private ClasificacionSalud clasificacionConId(UUID classificationId) {
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(classificationId);
        return classification;
    }

    private Promotion promotion(UUID promotionId) {
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        promotion.setName("Promo");
        promotion.setDiscountPercentage(new BigDecimal("25.00"));
        return promotion;
    }

    private Producto productoParaCompra(UUID productId) {
        Producto producto = new Producto();
        producto.setId(productId);
        producto.setNombre("Alfajor");
        producto.setPrecio(new BigDecimal("100.00"));
        producto.setStockActual(10);
        producto.setStockReservado(1);
        producto.setCupoDisponibleDia(4);
        return producto;
    }
}
