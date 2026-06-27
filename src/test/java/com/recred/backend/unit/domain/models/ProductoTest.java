package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.ProductoMedida;
import com.recred.backend.domain.models.TipoManejoInventario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
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
}
