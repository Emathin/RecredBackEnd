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

    @Test
    void fromDomainShouldMapSupplierWithPriceListsAndAcceptNull() {
        Proveedor supplier = supplier();

        SupplierResponse response = SupplierResponse.fromDomain(supplier);

        assertSupplier(response, supplier.getId(), supplier.getKiosqueroId());
        assertEquals(1, response.listasPrecios().size());
        assertEquals("list.csv", response.listasPrecios().get(0).nombreOriginal());
        assertEquals("Alfajor proveedor",
                response.listasPrecios().get(0).items().get(0).nombreProductoProveedor());
        assertNull(SupplierResponse.fromDomain(null));
    }

    @Test
    void fromApplicationShouldMapSupplierWithPriceListsAndAcceptNull() {
        UUID supplierId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        SupplierDetailsResponse supplier = new SupplierDetailsResponse(
                supplierId,
                ownerId,
                "Proveedor",
                "111",
                "supplier@test.com",
                "lunes",
                "nota",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 2, 10, 0),
                List.of(priceListResponse(supplierId))
        );

        SupplierResponse response = SupplierResponse.fromApplication(supplier);

        assertSupplier(response, supplierId, ownerId);
        assertEquals(1, response.listasPrecios().size());
        assertEquals("list.csv", response.listasPrecios().get(0).nombreOriginal());
        assertEquals("Alfajor proveedor",
                response.listasPrecios().get(0).items().get(0).nombreProductoProveedor());
        assertNull(SupplierResponse.fromApplication(null));
    }

    @Test
    void mappingShouldReturnEmptyPriceListsWhenSourceListsAreNull() {
        Proveedor domain = supplier();
        domain.setListasPrecios(null);
        SupplierDetailsResponse application = new SupplierDetailsResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Proveedor",
                "111",
                "supplier@test.com",
                "lunes",
                "nota",
                null,
                null,
                null
        );

        assertEquals(0, SupplierResponse.fromDomain(domain).listasPrecios().size());
        assertEquals(0, SupplierResponse.fromApplication(application).listasPrecios().size());
    }

    private void assertSupplier(SupplierResponse response, UUID supplierId, UUID ownerId) {
        assertEquals(supplierId, response.id());
        assertEquals(ownerId, response.kiosqueroId());
        assertEquals("Proveedor", response.nombre());
        assertEquals("111", response.telefono());
        assertEquals("supplier@test.com", response.email());
        assertEquals("lunes", response.diasVisita());
        assertEquals("nota", response.notas());
    }

    private Proveedor supplier() {
        UUID supplierId = UUID.randomUUID();
        Proveedor supplier = new Proveedor();
        supplier.setId(supplierId);
        supplier.setKiosqueroId(UUID.randomUUID());
        supplier.setNombre("Proveedor");
        supplier.setTelefono("111");
        supplier.setEmail("supplier@test.com");
        supplier.setDiasVisita("lunes");
        supplier.setNotas("nota");
        supplier.setCreadoEn(LocalDateTime.of(2026, 7, 1, 10, 0));
        supplier.setActualizadoEn(LocalDateTime.of(2026, 7, 2, 10, 0));
        supplier.setListasPrecios(List.of(priceList(supplierId)));
        return supplier;
    }

    private ListaPrecioProveedor priceList(UUID supplierId) {
        ListaPrecioProveedor priceList = new ListaPrecioProveedor();
        priceList.setId(UUID.randomUUID());
        priceList.setProveedorId(supplierId);
        priceList.setUrlArchivo("https://files.test/list.csv");
        priceList.setNombreOriginal("list.csv");
        priceList.setActiva(true);
        priceList.setCreadoEn(LocalDateTime.of(2026, 7, 1, 11, 0));
        priceList.setItems(List.of(item(priceList.getId())));
        return priceList;
    }

    private ItemListaPrecioProveedor item(UUID priceListId) {
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

    private UploadSupplierPriceListResponse priceListResponse(UUID supplierId) {
        UUID priceListId = UUID.randomUUID();
        return new UploadSupplierPriceListResponse(
                priceListId,
                supplierId,
                "https://files.test/list.csv",
                "list.csv",
                true,
                LocalDateTime.of(2026, 7, 1, 11, 0),
                List.of(new UploadSupplierPriceListResponse.Item(
                        UUID.randomUUID(),
                        priceListId,
                        "Alfajor proveedor",
                        null,
                        null,
                        new BigDecimal("500.00"),
                        "unidad",
                        "nota",
                        true
                ))
        );
    }
}
