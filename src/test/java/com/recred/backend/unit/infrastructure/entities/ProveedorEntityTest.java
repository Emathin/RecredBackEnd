package com.recred.backend.unit.infrastructure.entities;

import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.infrastructure.entities.ItemListaPrecioProveedorEntity;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import com.recred.backend.infrastructure.entities.ListaPrecioProveedorEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.ProveedorEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProveedorEntityTest {

    @Test
    void fromDomainShouldMapProviderPriceListsItemsAndBackReferences() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID proveedorId = UUID.randomUUID();
        UUID listaId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 8, 30);
        Proveedor proveedor = proveedorDomain(
                proveedorId,
                kiosqueroId,
                createdAt,
                priceListDomain(listaId, proveedorId, createdAt,
                        itemDomain(itemId, listaId, productoId))
        );

        ProveedorEntity result = ProveedorEntity.fromDomain(proveedor);

        assertEquals(proveedorId, result.getId());
        assertEquals(kiosqueroId, result.getKiosquero().getId());
        assertEquals("Distribuidora Norte", result.getNombre());
        assertEquals("lunes y miercoles", result.getDiasVisita());
        assertEquals(1, result.getListasPrecios().size());

        ListaPrecioProveedorEntity listEntity = result.getListasPrecios().get(0);
        assertEquals(listaId, listEntity.getId());
        assertEquals("lista.pdf", listEntity.getNombreOriginal());
        assertSame(result, listEntity.getProveedor());
        assertEquals(1, listEntity.getItems().size());

        ItemListaPrecioProveedorEntity itemEntity = listEntity.getItems().get(0);
        assertEquals(itemId, itemEntity.getId());
        assertEquals(productoId, itemEntity.getProductoInventario().getId());
        assertSame(listEntity, itemEntity.getListaPrecio());
    }

    @Test
    void toDomainShouldMapProviderPriceListsItemsAndInventoryProductName() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID proveedorId = UUID.randomUUID();
        UUID listaId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 8, 30);
        ProveedorEntity entity = providerEntity(proveedorId, kiosqueroId, createdAt);
        ListaPrecioProveedorEntity listEntity = priceListEntity(listaId, entity, createdAt);
        ItemListaPrecioProveedorEntity itemEntity = itemEntity(
                itemId,
                listEntity,
                inventoryProduct(productoId)
        );
        listEntity.setItems(List.of(itemEntity));
        entity.setListasPrecios(List.of(listEntity));

        Proveedor result = entity.toDomain();

        assertEquals(proveedorId, result.getId());
        assertEquals(kiosqueroId, result.getKiosqueroId());
        assertEquals("Distribuidora Norte", result.getNombre());
        assertEquals(1, result.getListasPrecios().size());

        ListaPrecioProveedor list = result.getListasPrecios().get(0);
        assertEquals(listaId, list.getId());
        assertEquals(proveedorId, list.getProveedorId());
        assertEquals(1, list.getItems().size());

        ItemListaPrecioProveedor item = list.getItems().get(0);
        assertEquals(itemId, item.getId());
        assertEquals(listaId, item.getListaPrecioId());
        assertEquals(productoId, item.getProductoInventarioId());
        assertEquals("Alfajor", item.getNombreProductoInventario());
    }

    @Test
    void mappersShouldHandleNullDomainAndMissingRelations() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(UUID.randomUUID());
        proveedor.setListasPrecios(null);

        ProveedorEntity proveedorEntity = ProveedorEntity.fromDomain(proveedor);
        proveedorEntity.setListasPrecios(null);

        ListaPrecioProveedor lista = new ListaPrecioProveedor();
        lista.setId(UUID.randomUUID());
        lista.setItems(null);

        ListaPrecioProveedorEntity listaEntity = ListaPrecioProveedorEntity.fromDomain(lista);
        listaEntity.setItems(null);

        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setId(UUID.randomUUID());

        ItemListaPrecioProveedorEntity itemEntity =
                ItemListaPrecioProveedorEntity.fromDomain(item);

        assertNull(ProveedorEntity.fromDomain(null));
        assertNull(ListaPrecioProveedorEntity.fromDomain(null));
        assertNull(ItemListaPrecioProveedorEntity.fromDomain(null));
        assertNull(proveedorEntity.getKiosquero());
        assertTrue(proveedorEntity.toDomain().getListasPrecios().isEmpty());
        assertNull(listaEntity.getProveedor());
        assertTrue(listaEntity.toDomain().getItems().isEmpty());
        assertNull(itemEntity.getListaPrecio());
        assertNull(itemEntity.getProductoInventario());
        assertNull(itemEntity.toDomain().getListaPrecioId());
        assertNull(itemEntity.toDomain().getProductoInventarioId());
    }

    private Proveedor proveedorDomain(
            UUID proveedorId,
            UUID kiosqueroId,
            LocalDateTime createdAt,
            ListaPrecioProveedor lista
    ) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorId);
        proveedor.setKiosqueroId(kiosqueroId);
        proveedor.setNombre("Distribuidora Norte");
        proveedor.setTelefono("1122334455");
        proveedor.setEmail("proveedor@example.com");
        proveedor.setDiasVisita("lunes y miercoles");
        proveedor.setNotas("entrega por la manana");
        proveedor.setCreadoEn(createdAt);
        proveedor.setActualizadoEn(createdAt.plusHours(2));
        proveedor.setListasPrecios(List.of(lista));
        return proveedor;
    }

    private ListaPrecioProveedor priceListDomain(
            UUID listaId,
            UUID proveedorId,
            LocalDateTime createdAt,
            ItemListaPrecioProveedor item
    ) {
        ListaPrecioProveedor lista = new ListaPrecioProveedor();
        lista.setId(listaId);
        lista.setProveedorId(proveedorId);
        lista.setUrlArchivo("https://example.com/lista.pdf");
        lista.setNombreOriginal("lista.pdf");
        lista.setActiva(false);
        lista.setCreadoEn(createdAt);
        lista.setItems(List.of(item));
        return lista;
    }

    private ItemListaPrecioProveedor itemDomain(
            UUID itemId,
            UUID listaId,
            UUID productoId
    ) {
        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setId(itemId);
        item.setListaPrecioId(listaId);
        item.setNombreProductoProveedor("Alfajor proveedor");
        item.setProductoInventarioId(productoId);
        item.setPrecio(new BigDecimal("350.00"));
        item.setUnidad("caja");
        item.setNotas("pack x12");
        item.setMappingConfirmado(true);
        return item;
    }

    private ProveedorEntity providerEntity(
            UUID proveedorId,
            UUID kiosqueroId,
            LocalDateTime createdAt
    ) {
        ProveedorEntity entity = new ProveedorEntity();
        entity.setId(proveedorId);
        entity.setKiosquero(kiosqueroEntity(kiosqueroId));
        entity.setNombre("Distribuidora Norte");
        entity.setTelefono("1122334455");
        entity.setEmail("proveedor@example.com");
        entity.setDiasVisita("lunes y miercoles");
        entity.setNotas("entrega por la manana");
        entity.setCreadoEn(createdAt);
        entity.setActualizadoEn(createdAt.plusHours(2));
        return entity;
    }

    private KiosqueroEntity kiosqueroEntity(UUID kiosqueroId) {
        KiosqueroEntity entity = new KiosqueroEntity();
        entity.setId(kiosqueroId);
        return entity;
    }

    private ListaPrecioProveedorEntity priceListEntity(
            UUID listaId,
            ProveedorEntity proveedor,
            LocalDateTime createdAt
    ) {
        ListaPrecioProveedorEntity entity = new ListaPrecioProveedorEntity();
        entity.setId(listaId);
        entity.setProveedor(proveedor);
        entity.setUrlArchivo("https://example.com/lista.pdf");
        entity.setNombreOriginal("lista.pdf");
        entity.setActiva(false);
        entity.setFechaCarga(createdAt);
        return entity;
    }

    private ItemListaPrecioProveedorEntity itemEntity(
            UUID itemId,
            ListaPrecioProveedorEntity lista,
            ProductoEntity producto
    ) {
        ItemListaPrecioProveedorEntity entity = new ItemListaPrecioProveedorEntity();
        entity.setId(itemId);
        entity.setListaPrecio(lista);
        entity.setNombreProductoProveedor("Alfajor proveedor");
        entity.setProductoInventario(producto);
        entity.setPrecio(new BigDecimal("350.00"));
        entity.setUnidad("caja");
        entity.setNotas("pack x12");
        entity.setMappingConfirmado(true);
        return entity;
    }

    private ProductoEntity inventoryProduct(UUID productoId) {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(productoId);
        entity.setNombre("Alfajor");
        return entity;
    }
}
