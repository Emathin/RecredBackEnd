package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.infrastructure.entities.ItemListaPrecioProveedorEntity;
import com.recred.backend.infrastructure.entities.ListaPrecioProveedorEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.persistence.ItemListaPrecioProveedorRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataItemListaPrecioProveedorRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemListaPrecioProveedorRepositoryImplTest {

    @Mock
    private SpringDataItemListaPrecioProveedorRepository repository;

    private ItemListaPrecioProveedorRepositoryImpl adapter;

    @BeforeEach
    void setUp() {
        adapter = new ItemListaPrecioProveedorRepositoryImpl(repository);
    }

    @Test
    void saveShouldMapDomainToEntityAndBack() {
        UUID itemId = UUID.randomUUID();
        UUID priceListId = UUID.randomUUID();
        UUID inventoryProductId = UUID.randomUUID();
        when(repository.save(any(ItemListaPrecioProveedorEntity.class)))
                .thenReturn(entity(itemId, priceListId, inventoryProductId));

        ItemListaPrecioProveedor result = adapter.save(domain(
                itemId,
                priceListId,
                inventoryProductId
        ));

        ArgumentCaptor<ItemListaPrecioProveedorEntity> captor =
                ArgumentCaptor.forClass(ItemListaPrecioProveedorEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(priceListId, captor.getValue().getListaPrecio().getId());
        assertEquals(inventoryProductId, captor.getValue().getProductoInventario().getId());
        assertEquals(itemId, result.getId());
        assertEquals("Producto inventario", result.getNombreProductoInventario());
        assertTrue(result.isMappingConfirmado());
    }

    @Test
    void saveAllShouldMapEntitiesAndReturnDomains() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID priceListId = UUID.randomUUID();
        when(repository.saveAll(any()))
                .thenReturn(List.of(
                        entity(firstId, priceListId, null),
                        entity(secondId, priceListId, UUID.randomUUID())
                ));

        List<ItemListaPrecioProveedor> result = adapter.saveAll(List.of(
                domain(firstId, priceListId, null),
                domain(secondId, priceListId, UUID.randomUUID())
        ));

        assertEquals(2, result.size());
        assertEquals(firstId, result.get(0).getId());
        assertEquals(secondId, result.get(1).getId());
        verify(repository).saveAll(any());
    }

    @Test
    void findMethodsShouldMapRepositoryResults() {
        UUID itemId = UUID.randomUUID();
        UUID priceListId = UUID.randomUUID();
        UUID kiosqueroId = UUID.randomUUID();
        UUID inventoryProductId = UUID.randomUUID();
        ItemListaPrecioProveedorEntity entity = entity(
                itemId,
                priceListId,
                inventoryProductId
        );
        when(repository.findById(itemId)).thenReturn(Optional.of(entity));
        when(repository.findByListaPrecioId(priceListId)).thenReturn(List.of(entity));
        when(repository.findItemsActivosByKiosqueroIdAndProductoIds(
                kiosqueroId,
                List.of(inventoryProductId))).thenReturn(List.of(entity));

        Optional<ItemListaPrecioProveedor> found = adapter.findById(itemId);
        List<ItemListaPrecioProveedor> byPriceList = adapter.findByListaPrecioId(priceListId);
        List<ItemListaPrecioProveedor> activeItems =
                adapter.findItemsActivosByKiosqueroIdAndProductoIds(
                        kiosqueroId,
                        List.of(inventoryProductId)
                );

        assertTrue(found.isPresent());
        assertEquals(itemId, found.orElseThrow().getId());
        assertEquals(1, byPriceList.size());
        assertEquals(1, activeItems.size());
        assertEquals(inventoryProductId, activeItems.get(0).getProductoInventarioId());
    }

    private ItemListaPrecioProveedor domain(
            UUID itemId,
            UUID priceListId,
            UUID inventoryProductId
    ) {
        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setId(itemId);
        item.setListaPrecioId(priceListId);
        item.setProductoInventarioId(inventoryProductId);
        item.setNombreProductoProveedor("Producto proveedor");
        item.setPrecio(new BigDecimal("123.45"));
        item.setUnidad("unidad");
        item.setNotas("nota");
        item.setMappingConfirmado(true);
        return item;
    }

    private ItemListaPrecioProveedorEntity entity(
            UUID itemId,
            UUID priceListId,
            UUID inventoryProductId
    ) {
        ItemListaPrecioProveedorEntity entity = new ItemListaPrecioProveedorEntity();
        entity.setId(itemId);
        entity.setListaPrecio(priceList(priceListId));
        entity.setProductoInventario(product(inventoryProductId));
        entity.setNombreProductoProveedor("Producto proveedor");
        entity.setPrecio(new BigDecimal("123.45"));
        entity.setUnidad("unidad");
        entity.setNotas("nota");
        entity.setMappingConfirmado(true);
        return entity;
    }

    private ListaPrecioProveedorEntity priceList(UUID priceListId) {
        ListaPrecioProveedorEntity entity = new ListaPrecioProveedorEntity();
        entity.setId(priceListId);
        return entity;
    }

    private ProductoEntity product(UUID inventoryProductId) {
        if (inventoryProductId == null)
            return null;

        ProductoEntity entity = new ProductoEntity();
        entity.setId(inventoryProductId);
        entity.setNombre("Producto inventario");
        return entity;
    }
}
