package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.UpdatePriceListItemMappingResponse;
import com.recred.backend.application.UploadSupplierPriceListResponse;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.infrastructure.web.ItemListaPrecioProveedorResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemListaPrecioProveedorResponseTest {

    private final UUID ITEM_ID = UUID.randomUUID();
    private final UUID PRICE_LIST_ID = UUID.randomUUID();
    private final String PROVEEDOR_NOMBRE = "Alfajor proveedor";
    private final String INVENTARIO_NOMBRE = "Alfajor";
    private final BigDecimal PRECIO = new BigDecimal("500.00");
    private final String UNIDAD = "unidad";
    private final String NOTA = "nota";
    private final boolean MAPPING = true;

    @Test
    void fromDomainShouldMapItemAndAcceptNull() {
        ItemListaPrecioProveedor item = givenDomainItem();
        
        ItemListaPrecioProveedorResponse response = whenMappingFromDomain(item);
        
        thenResponseIsMappedCorrectly(response);
        thenNullInputReturnsNull(whenMappingFromDomain(null));
    }

    @Test
    void fromUploadApplicationShouldMapItemAndAcceptNull() {
        UploadSupplierPriceListResponse.Item item = givenUploadResponseItem();
        
        ItemListaPrecioProveedorResponse response = whenMappingFromUpload(item);
        
        thenResponseIsMappedCorrectly(response);
        thenNullInputReturnsNull(whenMappingFromUpload(null));
    }

    @Test
    void fromMappingApplicationShouldMapItemAndAcceptNull() {
        UpdatePriceListItemMappingResponse item = givenUpdateMappingResponseItem();
        
        ItemListaPrecioProveedorResponse response = whenMappingFromUpdate(item);
        
        thenResponseIsMappedCorrectly(response);
        thenNullInputReturnsNull(whenMappingFromUpdate(null));
    }

    private ItemListaPrecioProveedor givenDomainItem() {
        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setId(ITEM_ID);
        item.setListaPrecioId(PRICE_LIST_ID);
        item.setNombreProductoProveedor(PROVEEDOR_NOMBRE);
        item.setProductoInventarioId(UUID.randomUUID());
        item.setNombreProductoInventario(INVENTARIO_NOMBRE);
        item.setPrecio(PRECIO);
        item.setUnidad(UNIDAD);
        item.setNotas(NOTA);
        item.setMappingConfirmado(MAPPING);
        return item;
    }

    private UploadSupplierPriceListResponse.Item givenUploadResponseItem() {
        return new UploadSupplierPriceListResponse.Item(
                ITEM_ID, PRICE_LIST_ID, PROVEEDOR_NOMBRE, UUID.randomUUID(),
                INVENTARIO_NOMBRE, PRECIO, UNIDAD, NOTA, MAPPING
        );
    }

    private UpdatePriceListItemMappingResponse givenUpdateMappingResponseItem() {
        return new UpdatePriceListItemMappingResponse(
                ITEM_ID, PRICE_LIST_ID, PROVEEDOR_NOMBRE, UUID.randomUUID(),
                INVENTARIO_NOMBRE, PRECIO, UNIDAD, NOTA, MAPPING
        );
    }

    private ItemListaPrecioProveedorResponse whenMappingFromDomain(ItemListaPrecioProveedor item) {
        return ItemListaPrecioProveedorResponse.fromDomain(item);
    }

    private ItemListaPrecioProveedorResponse whenMappingFromUpload(UploadSupplierPriceListResponse.Item item) {
        return ItemListaPrecioProveedorResponse.fromApplication(item);
    }

    private ItemListaPrecioProveedorResponse whenMappingFromUpdate(UpdatePriceListItemMappingResponse item) {
        return ItemListaPrecioProveedorResponse.fromApplication(item);
    }

    private void thenResponseIsMappedCorrectly(ItemListaPrecioProveedorResponse response) {
        assertEquals(ITEM_ID, response.id());
        assertEquals(PRICE_LIST_ID, response.listaPrecioId());
        assertEquals(PROVEEDOR_NOMBRE, response.nombreProductoProveedor());
        assertEquals(INVENTARIO_NOMBRE, response.nombreProductoInventario());
        assertEquals(PRECIO, response.precio());
        assertEquals(UNIDAD, response.unidad());
        assertEquals(NOTA, response.notas());
        assertEquals(MAPPING, response.mappingConfirmado());
    }

    private void thenNullInputReturnsNull(ItemListaPrecioProveedorResponse response) {
        assertNull(response);
    }
}
