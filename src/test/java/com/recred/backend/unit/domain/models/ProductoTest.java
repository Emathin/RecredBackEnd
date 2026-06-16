package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.ProductoMedida;
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
}
