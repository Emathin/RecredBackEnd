package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.SupplierDetailsResponse;
import com.recred.backend.application.UploadSupplierPriceListResponse;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.infrastructure.web.SupplierResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SupplierResponseMappingTest {

    private static final UUID SUPPLIER_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    private SupplierResponse result;

    @Test
    void givenDomainSupplierWhenMappingThenMapsSupplierWithPriceLists() {
        Proveedor supplier = givenDomainSupplier();

        result = whenMappingFromDomain(supplier);

        thenSupplierFieldsAreMapped(SUPPLIER_ID, OWNER_ID);
        thenPriceListCountIs(1);
        thenFirstPriceListFileNameIs("list.csv");
        thenFirstItemNameIs("Alfajor proveedor");
    }

    @Test
    void givenNullDomainSupplierWhenMappingThenReturnsNull() {
        result = whenMappingFromDomain(null);

        assertNull(result);
    }

    @Test
    void givenApplicationSupplierWhenMappingThenMapsSupplierWithPriceLists() {
        SupplierDetailsResponse supplier = givenApplicationSupplier();

        result = whenMappingFromApplication(supplier);

        thenSupplierFieldsAreMapped(SUPPLIER_ID, OWNER_ID);
        thenPriceListCountIs(1);
        thenFirstPriceListFileNameIs("list.csv");
        thenFirstItemNameIs("Alfajor proveedor");
    }

    @Test
    void givenNullApplicationSupplierWhenMappingThenReturnsNull() {
        result = whenMappingFromApplication(null);

        assertNull(result);
    }

    @Test
    void givenNullPriceListsWhenMappingThenReturnsEmptyPriceLists() {
        Proveedor domain = givenDomainSupplierWithNullLists();
        SupplierDetailsResponse application =
                givenApplicationSupplierWithNullLists();

        assertEquals(0,
                whenMappingFromDomain(domain).listasPrecios().size());
        assertEquals(0,
                whenMappingFromApplication(application)
                        .listasPrecios().size());
    }

    private Proveedor givenDomainSupplier() {
        Proveedor supplier = new Proveedor();
        supplier.setId(SUPPLIER_ID);
        supplier.setKiosqueroId(OWNER_ID);
        supplier.setNombre("Proveedor");
        supplier.setTelefono("111");
        supplier.setEmail("supplier@test.com");
        supplier.setDiasVisita("lunes");
        supplier.setNotas("nota");
        supplier.setCreadoEn(LocalDateTime.of(2026, 7, 1, 10, 0));
        supplier.setActualizadoEn(
                LocalDateTime.of(2026, 7, 2, 10, 0));
        supplier.setListasPrecios(
                List.of(givenPriceList(SUPPLIER_ID)));
        return supplier;
    }

    private Proveedor givenDomainSupplierWithNullLists() {
        Proveedor supplier = givenDomainSupplier();
        supplier.setListasPrecios(null);
        return supplier;
    }

    private SupplierDetailsResponse givenApplicationSupplier() {
        return new SupplierDetailsResponse(
                SUPPLIER_ID, OWNER_ID,
                "Proveedor", "111", "supplier@test.com",
                "lunes", "nota",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 2, 10, 0),
                List.of(givenPriceListResponse(SUPPLIER_ID)));
    }

    private SupplierDetailsResponse givenApplicationSupplierWithNullLists() {
        return new SupplierDetailsResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                "Proveedor", "111", "supplier@test.com",
                "lunes", "nota", null, null, null);
    }

    private SupplierResponse whenMappingFromDomain(Proveedor supplier) {
        return SupplierResponse.fromDomain(supplier);
    }

    private SupplierResponse whenMappingFromApplication(
            SupplierDetailsResponse supplier) {
        return SupplierResponse.fromApplication(supplier);
    }

    private void thenSupplierFieldsAreMapped(
            UUID supplierId, UUID ownerId) {
        assertEquals(supplierId, result.id());
        assertEquals(ownerId, result.kiosqueroId());
        assertEquals("Proveedor", result.nombre());
        assertEquals("111", result.telefono());
        assertEquals("supplier@test.com", result.email());
        assertEquals("lunes", result.diasVisita());
        assertEquals("nota", result.notas());
    }

    private void thenPriceListCountIs(int expected) {
        assertEquals(expected, result.listasPrecios().size());
    }

    private void thenFirstPriceListFileNameIs(String expected) {
        assertEquals(expected,
                result.listasPrecios().get(0).nombreOriginal());
    }

    private void thenFirstItemNameIs(String expected) {
        assertEquals(expected,
                result.listasPrecios().get(0).items().get(0)
                        .nombreProductoProveedor());
    }

    private ListaPrecioProveedor givenPriceList(UUID supplierId) {
        ListaPrecioProveedor priceList = new ListaPrecioProveedor();
        priceList.setId(UUID.randomUUID());
        priceList.setProveedorId(supplierId);
        priceList.setUrlArchivo("https://files.test/list.csv");
        priceList.setNombreOriginal("list.csv");
        priceList.setActiva(true);
        priceList.setCreadoEn(
                LocalDateTime.of(2026, 7, 1, 11, 0));
        priceList.setItems(List.of(givenItem(priceList.getId())));
        return priceList;
    }

    private ItemListaPrecioProveedor givenItem(UUID priceListId) {
        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setId(UUID.randomUUID());
        item.setListaPrecioId(priceListId);
        item.setNombreProductoProveedor("Alfajor proveedor");
        item.setPrecio(new BigDecimal("500.00"));
        item.setUnidad("unidad");
        item.setNotas("nota");
        item.setMappingConfirmado(true);
        return item;
    }

    private UploadSupplierPriceListResponse givenPriceListResponse(
            UUID supplierId) {
        UUID priceListId = UUID.randomUUID();
        return new UploadSupplierPriceListResponse(
                priceListId, supplierId,
                "https://files.test/list.csv", "list.csv", true,
                LocalDateTime.of(2026, 7, 1, 11, 0),
                List.of(new UploadSupplierPriceListResponse.Item(
                        UUID.randomUUID(), priceListId,
                        "Alfajor proveedor", null, null,
                        new BigDecimal("500.00"), "unidad",
                        "nota", true)));
    }
}
