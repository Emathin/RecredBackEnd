package com.recred.backend.unit.application;

import com.recred.backend.application.UpdatePriceListItemMapping;
import com.recred.backend.application.UpdatePriceListItemMappingRequest;
import com.recred.backend.application.UpdatePriceListItemMappingResponse;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePriceListItemMappingTest {

    @Mock
    private ItemListaPrecioProveedorRepository itemRepository;

    @Mock
    private ListaPrecioProveedorRepository listaRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    private UpdatePriceListItemMapping useCase;
    private UUID kioskOwnerId;
    private UUID itemId;
    private UUID priceListId;
    private UUID supplierId;
    private UUID inventoryProductId;

    @BeforeEach
    void setUp() {
        useCase = new UpdatePriceListItemMapping(
                itemRepository,
                listaRepository,
                proveedorRepository
        );
        kioskOwnerId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        priceListId = UUID.randomUUID();
        supplierId = UUID.randomUUID();
        inventoryProductId = UUID.randomUUID();
    }

    @Test
    void executeShouldConfirmMappingWhenSupplierBelongsToKioskOwner() {
        givenItemPriceListAndSupplier(kioskOwnerId);
        when(itemRepository.save(any(ItemListaPrecioProveedor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePriceListItemMappingResponse response = useCase.execute(
                new UpdatePriceListItemMappingRequest(
                        kioskOwnerId,
                        itemId,
                        inventoryProductId
                ));

        assertEquals(itemId, response.id());
        assertEquals(priceListId, response.priceListId());
        assertEquals(inventoryProductId, response.inventoryProductId());
        assertTrue(response.mappingConfirmed());
    }

    @Test
    void executeShouldRejectWhenSupplierDoesNotBelongToKioskOwner() {
        givenItemPriceListAndSupplier(UUID.randomUUID());

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(new UpdatePriceListItemMappingRequest(
                        kioskOwnerId,
                        itemId,
                        inventoryProductId
                )));
    }

    @Test
    void executeShouldValidateRequestAndMissingAssociations() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new UpdatePriceListItemMappingRequest(
                        null,
                        itemId,
                        inventoryProductId
                )));

        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertEquals("Price list item not found",
                assertThrows(IllegalArgumentException.class,
                        () -> useCase.execute(new UpdatePriceListItemMappingRequest(
                                kioskOwnerId,
                                itemId,
                                inventoryProductId
                        ))).getMessage());
    }

    @Test
    void executeShouldFailWhenPriceListOrSupplierDoNotExist() {
        ItemListaPrecioProveedor item = item();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(listaRepository.findById(priceListId)).thenReturn(Optional.empty());

        assertEquals("Associated price list not found",
                assertThrows(IllegalArgumentException.class,
                        () -> useCase.execute(new UpdatePriceListItemMappingRequest(
                                kioskOwnerId,
                                itemId,
                                inventoryProductId
                        ))).getMessage());

        ListaPrecioProveedor priceList = priceList();
        when(listaRepository.findById(priceListId)).thenReturn(Optional.of(priceList));
        when(proveedorRepository.findById(supplierId)).thenReturn(Optional.empty());

        assertEquals("Associated supplier not found",
                assertThrows(IllegalArgumentException.class,
                        () -> useCase.execute(new UpdatePriceListItemMappingRequest(
                                kioskOwnerId,
                                itemId,
                                inventoryProductId
                        ))).getMessage());
    }

    private void givenItemPriceListAndSupplier(UUID ownerId) {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        when(listaRepository.findById(priceListId)).thenReturn(Optional.of(priceList()));
        when(proveedorRepository.findById(supplierId)).thenReturn(Optional.of(supplier(ownerId)));
    }

    private ItemListaPrecioProveedor item() {
        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setId(itemId);
        item.setListaPrecioId(priceListId);
        item.setNombreProductoProveedor("Alfajor proveedor");
        item.setPrecio(new BigDecimal("350.00"));
        item.setUnidad("caja");
        item.setNotas("pack x12");
        return item;
    }

    private ListaPrecioProveedor priceList() {
        ListaPrecioProveedor priceList = new ListaPrecioProveedor();
        priceList.setId(priceListId);
        priceList.setProveedorId(supplierId);
        return priceList;
    }

    private Proveedor supplier(UUID ownerId) {
        Proveedor supplier = new Proveedor();
        supplier.setId(supplierId);
        supplier.setKiosqueroId(ownerId);
        return supplier;
    }
}
