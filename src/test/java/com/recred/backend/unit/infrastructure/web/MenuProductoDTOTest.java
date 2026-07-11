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

    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    void permitidoShouldMapFullProduct() {
        Producto product = givenFullProduct();
        
        MenuProductoDTO dto = whenMappingPermitido(product);
        
        thenFullProductIsMapped(dto);
    }

    @Test
    void bloqueadoShouldSetMotivo() {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);

        MenuProductoDTO dto = whenMappingBloqueado(product, "Salud");
        
        thenMotivoIsSet(dto);
    }

    @Test
    void staticMethodsShouldWork() {
        MenuProductoDTO.CategoryDTO catDto = givenCategoryDto();
        
        MenuProductoDTO perm = whenCallingPermitido(catDto);
        MenuProductoDTO bloq = whenCallingBloqueado(catDto);
        
        thenStaticMethodsSetExpectedFlags(perm, bloq);
    }

    @Test
    void fromProductShouldHandleNullFields() {
        Producto product = givenProductWithNullFields();
        
        MenuProductoDTO dto = whenMappingPermitido(product);
        
        thenNullFieldsAreHandled(dto);
    }

    private Producto givenFullProduct() {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        product.setNombre("Product");
        product.setPrecio(new BigDecimal("10.00"));
        product.setStockActual(10);
        product.setEstadoInventario(EstadoInventario.DISPONIBLE);
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);

        CategoriaProducto category = new CategoriaProducto();
        category.setId(CATEGORY_ID);
        category.setDescripcion("Cat");
        product.setCategoria(category);
        return product;
    }

    private MenuProductoDTO.CategoryDTO givenCategoryDto() {
        return new MenuProductoDTO.CategoryDTO(CATEGORY_ID, "Desc");
    }

    private Producto givenProductWithNullFields() {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        product.setCategoria(null);
        product.setEstadoInventario(null);
        product.setTipoManejoInventario(null);
        return product;
    }

    private MenuProductoDTO whenMappingPermitido(Producto product) {
        return MenuProductoDTO.permitido(product);
    }

    private MenuProductoDTO whenMappingBloqueado(Producto product, String motivo) {
        return MenuProductoDTO.bloqueado(product, motivo);
    }

    private MenuProductoDTO whenCallingPermitido(MenuProductoDTO.CategoryDTO catDto) {
        return MenuProductoDTO.permitido(PRODUCT_ID, "N", "D", BigDecimal.ONE, catDto);
    }

    private MenuProductoDTO whenCallingBloqueado(MenuProductoDTO.CategoryDTO catDto) {
        return MenuProductoDTO.bloqueado(PRODUCT_ID, "N", "D", BigDecimal.ONE, "M", catDto);
    }

    private void thenFullProductIsMapped(MenuProductoDTO dto) {
        assertEquals(PRODUCT_ID, dto.id());
        assertEquals("Product", dto.nombre());
        assertEquals(new BigDecimal("10.00"), dto.precio());
        assertEquals(10, dto.stockActual());
        assertEquals("DISPONIBLE", dto.estadoInventario());
        assertEquals("STOCK_EXACTO", dto.tipoManejoInventario());
        assertFalse(dto.bloqueado());
        assertNull(dto.motivoBloqueo());
        assertEquals(CATEGORY_ID, dto.categoria().id());
    }

    private void thenMotivoIsSet(MenuProductoDTO dto) {
        assertTrue(dto.bloqueado());
        assertEquals("Salud", dto.motivoBloqueo());
    }

    private void thenStaticMethodsSetExpectedFlags(MenuProductoDTO perm, MenuProductoDTO bloq) {
        assertFalse(perm.bloqueado());
        assertTrue(bloq.bloqueado());
    }

    private void thenNullFieldsAreHandled(MenuProductoDTO dto) {
        assertNull(dto.categoria());
        assertNull(dto.estadoInventario());
        assertNull(dto.tipoManejoInventario());
    }
}
