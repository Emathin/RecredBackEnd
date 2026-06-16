package com.recred.backend.web;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductResponseDTOTest {

    @Test
    void fromDomain_ShouldMapFullProduct() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Product");
        product.setPrecio(new BigDecimal("10.00"));
        product.setActivo(true);
        product.setCreadoEn(LocalDateTime.now());
        product.setEstadoInventario(EstadoInventario.DISPONIBLE);
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);

        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Cat");
        product.setCategoria(category);

        ClasificacionSalud salud = new ClasificacionSalud();
        salud.setId(UUID.randomUUID());
        salud.setDescripcion("Salud");
        product.setClasificacionesSalud(Set.of(salud));

        ClasificacionTipo tipo = new ClasificacionTipo();
        tipo.setId(UUID.randomUUID());
        tipo.setDescripcion("Tipo");
        product.setTipos(Set.of(tipo));

        ProductResponseDTO dto = ProductResponseDTO.fromDomain(product);

        assertEquals(product.getId(), dto.id());
        assertEquals("Product", dto.nombre());
        assertEquals(new BigDecimal("10.00"), dto.precio());
        assertTrue(dto.activo());
        assertEquals("DISPONIBLE", dto.estadoInventario());
        assertEquals("STOCK_EXACTO", dto.tipoManejoInventario());
        assertEquals(category.getId(), dto.categoria().id());
        assertEquals(1, dto.clasificacionesSalud().size());
        assertEquals("Salud", dto.clasificacionesSalud().iterator().next().descripcion());
        assertEquals(1, dto.tipos().size());
        assertEquals("Tipo", dto.tipos().iterator().next().descripcion());
    }

    @Test
    void fromDomain_ShouldHandleNullFields() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setCategoria(null);
        product.setClasificacionesSalud(null);
        product.setTipos(null);
        product.setEstadoInventario(null);

        ProductResponseDTO dto = ProductResponseDTO.fromDomain(product);

        assertNull(dto.categoria());
        assertEquals(0, dto.clasificacionesSalud().size());
        assertEquals(0, dto.tipos().size());
        assertNull(dto.estadoInventario());
    }
}
