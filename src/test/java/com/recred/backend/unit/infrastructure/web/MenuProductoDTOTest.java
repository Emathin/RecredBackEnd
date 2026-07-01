package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.infrastructure.web.MenuProductoDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MenuProductoDTOTest {

    @Test
    void permitido_ShouldMapFullProduct() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Product");
        product.setPrecio(new BigDecimal("10.00"));
        product.setStockActual(10);
        product.setEstadoInventario(EstadoInventario.DISPONIBLE);
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);

        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Cat");
        product.setCategoria(category);

        MenuProductoDTO dto = MenuProductoDTO.permitido(product);

        assertEquals(product.getId(), dto.id());
        assertEquals("Product", dto.nombre());
        assertEquals(new BigDecimal("10.00"), dto.precio());
        assertEquals(10, dto.stockActual());
        assertEquals("DISPONIBLE", dto.estadoInventario());
        assertEquals("STOCK_EXACTO", dto.tipoManejoInventario());
        assertFalse(dto.bloqueado());
        assertNull(dto.motivoBloqueo());
        assertEquals(category.getId(), dto.categoria().id());
    }

    @Test
    void bloqueado_ShouldSetMotivo() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());

        MenuProductoDTO dto = MenuProductoDTO.bloqueado(product, "Salud");

        assertTrue(dto.bloqueado());
        assertEquals("Salud", dto.motivoBloqueo());
    }

    @Test
    void staticMethods_ShouldWork() {
        MenuProductoDTO.CategoryDTO catDto = new MenuProductoDTO.CategoryDTO(UUID.randomUUID(), "Desc");
        
        MenuProductoDTO perm = MenuProductoDTO.permitido(UUID.randomUUID(), "N", "D", BigDecimal.ONE, catDto);
        assertFalse(perm.bloqueado());
        
        MenuProductoDTO bloq = MenuProductoDTO.bloqueado(UUID.randomUUID(), "N", "D", BigDecimal.ONE, "M", catDto);
        assertTrue(bloq.bloqueado());
    }

    @Test
    void fromProduct_ShouldHandleNullFields() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setCategoria(null);
        product.setEstadoInventario(null);
        product.setTipoManejoInventario(null);

        MenuProductoDTO dto = MenuProductoDTO.permitido(product);

        assertNull(dto.categoria());
        assertNull(dto.estadoInventario());
        assertNull(dto.tipoManejoInventario());
    }
}

