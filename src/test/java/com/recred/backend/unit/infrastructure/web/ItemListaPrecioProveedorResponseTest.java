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

    @Test
    void fromDomainShouldMapItemAndAcceptNull() {
        ItemListaPrecioProveedor item = item();

        ItemListaPrecioProveedorResponse response =
                ItemListaPrecioProveedorResponse.fromDomain(item);

        assertResponse(response, item.getId(), item.getListaPrecioId());
        assertNull(ItemListaPrecioProveedorResponse.fromDomain(null));
    }

    @Test
    void fromUploadApplicationShouldMapItemAndAcceptNull() {
        UUID itemId = UUID.randomUUID();
        UUID priceListId = UUID.randomUUID();
        UploadSupplierPriceListResponse.Item item = new UploadSupplierPriceListResponse.Item(
                itemId,
                priceListId,
                "Alfajor proveedor",
                UUID.randomUUID(),
                "Alfajor",
                new BigDecimal("500.00"),
                "unidad",
                "nota",
                true
        );

        ItemListaPrecioProveedorResponse response =
                ItemListaPrecioProveedorResponse.fromApplication(item);

        assertResponse(response, itemId, priceListId);
        assertNull(ItemListaPrecioProveedorResponse.fromApplication(
                (UploadSupplierPriceListResponse.Item) null));
    }

    @Test
    void fromMappingApplicationShouldMapItemAndAcceptNull() {
        UUID itemId = UUID.randomUUID();
        UUID priceListId = UUID.randomUUID();
        UpdatePriceListItemMappingResponse item = new UpdatePriceListItemMappingResponse(
                itemId,
                priceListId,
                "Alfajor proveedor",
                UUID.randomUUID(),
                "Alfajor",
                new BigDecimal("500.00"),
                "unidad",
                "nota",
                true
        );

        ItemListaPrecioProveedorResponse response =
                ItemListaPrecioProveedorResponse.fromApplication(item);

        assertResponse(response, itemId, priceListId);
        assertNull(ItemListaPrecioProveedorResponse.fromApplication(
                (UpdatePriceListItemMappingResponse) null));
    }

    private void assertResponse(
            ItemListaPrecioProveedorResponse response,
            UUID itemId,
            UUID priceListId
    ) {
        assertEquals(itemId, response.id());
        assertEquals(priceListId, response.listaPrecioId());
        assertEquals("Alfajor proveedor", response.nombreProductoProveedor());
        assertEquals("Alfajor", response.nombreProductoInventario());
        assertEquals(new BigDecimal("500.00"), response.precio());
        assertEquals("unidad", response.unidad());
        assertEquals("nota", response.notas());
        assertEquals(true, response.mappingConfirmado());
    }

    private ItemListaPrecioProveedor item() {
        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setId(UUID.randomUUID());
        item.setListaPrecioId(UUID.randomUUID());
        item.setNombreProductoProveedor("Alfajor proveedor");
        item.setProductoInventarioId(UUID.randomUUID());
        item.setNombreProductoInventario("Alfajor");
        item.setPrecio(new BigDecimal("500.00"));
        item.setUnidad("unidad");
        item.setNotas("nota");
        item.setMappingConfirmado(true);
        return item;
    }
}
