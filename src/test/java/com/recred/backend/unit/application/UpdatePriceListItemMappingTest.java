package com.recred.backend.unit.application;

import com.recred.backend.application.UpdatePriceListItemMapping;
import com.recred.backend.application.UpdatePriceListItemMappingRequest;
import com.recred.backend.application.UpdatePriceListItemMappingResponse;
import com.recred.backend.domain.exceptions.OperationNotAllowedException;
import com.recred.backend.domain.models.ItemListaPrecioProveedor;
import com.recred.backend.domain.models.ListaPrecioProveedor;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ItemListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ListaPrecioProveedorRepository;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePriceListItemMappingTest {

    private static final UUID KIOSK_OWNER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID PRICE_LIST_ID = UUID.randomUUID();
    private static final UUID SUPPLIER_ID = UUID.randomUUID();
    private static final UUID INVENTORY_PRODUCT_ID = UUID.randomUUID();

    @Mock
    private ItemListaPrecioProveedorRepository itemRepository;

    @Mock
    private ListaPrecioProveedorRepository listaRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    private UpdatePriceListItemMapping useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdatePriceListItemMapping(
                itemRepository,
                listaRepository,
                proveedorRepository
        );
    }

    @Test
    void executeShouldConfirmMappingWhenSupplierBelongsToKioskOwner() {
        givenItemPriceListAndSupplierExist(KIOSK_OWNER_ID);
        givenSavedItemReturnsItself();

        UpdatePriceListItemMappingResponse response = whenUpdatingMapping(request());

        thenMappingIsConfirmed(response);
    }

    @Test
    void executeShouldRejectWhenSupplierDoesNotBelongToKioskOwner() {
        givenItemPriceListAndSupplierExist(UUID.randomUUID());

        Executable execution = () -> whenUpdatingMapping(request());

        thenThrowsOperationNotAllowedException(execution);
    }

    @Test
    void executeShouldValidateKioskOwnerId() {
        UpdatePriceListItemMappingRequest request = new UpdatePriceListItemMappingRequest(
                null, ITEM_ID, INVENTORY_PRODUCT_ID);

        Executable execution = () -> whenUpdatingMapping(request);

        thenThrowsIllegalArgumentException(execution);
    }

    @Test
    void executeShouldFailWhenItemDoesNotExist() {
        givenItemNotFound();

        Executable execution = () -> whenUpdatingMapping(request());

        thenThrowsIllegalArgumentExceptionWithMessage(execution, "Price list item not found");
    }

    @Test
    void executeShouldFailWhenPriceListDoesNotExist() {
        givenItemExists();
        givenPriceListNotFound();

        Executable execution = () -> whenUpdatingMapping(request());

        thenThrowsIllegalArgumentExceptionWithMessage(execution, "Associated price list not found");
    }

    @Test
    void executeShouldFailWhenSupplierDoesNotExist() {
        givenItemExists();
        givenPriceListExists();
        givenSupplierNotFound();

        Executable execution = () -> whenUpdatingMapping(request());

        thenThrowsIllegalArgumentExceptionWithMessage(execution, "Associated supplier not found");
    }


    private UpdatePriceListItemMappingRequest request() {
        return new UpdatePriceListItemMappingRequest(
                KIOSK_OWNER_ID, ITEM_ID, INVENTORY_PRODUCT_ID);
    }

    private void givenItemPriceListAndSupplierExist(UUID ownerId) {
        givenItemExists();
        givenPriceListExists();
        when(proveedorRepository.findById(SUPPLIER_ID))
                .thenReturn(Optional.of(supplier(ownerId)));
    }

    private void givenItemExists() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item()));
    }

    private void givenItemNotFound() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());
    }

    private void givenPriceListExists() {
        when(listaRepository.findById(PRICE_LIST_ID)).thenReturn(Optional.of(priceList()));
    }

    private void givenPriceListNotFound() {
        when(listaRepository.findById(PRICE_LIST_ID)).thenReturn(Optional.empty());
    }

    private void givenSupplierNotFound() {
        when(proveedorRepository.findById(SUPPLIER_ID)).thenReturn(Optional.empty());
    }

    private void givenSavedItemReturnsItself() {
        when(itemRepository.save(any(ItemListaPrecioProveedor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ItemListaPrecioProveedor item() {
        ItemListaPrecioProveedor item = new ItemListaPrecioProveedor();
        item.setId(ITEM_ID);
        item.setListaPrecioId(PRICE_LIST_ID);
        item.setNombreProductoProveedor("Alfajor proveedor");
        item.setPrecio(new BigDecimal("350.00"));
        item.setUnidad("caja");
        item.setNotas("pack x12");
        return item;
    }

    private ListaPrecioProveedor priceList() {
        ListaPrecioProveedor priceList = new ListaPrecioProveedor();
        priceList.setId(PRICE_LIST_ID);
        priceList.setProveedorId(SUPPLIER_ID);
        return priceList;
    }

    private Proveedor supplier(UUID ownerId) {
        Proveedor supplier = new Proveedor();
        supplier.setId(SUPPLIER_ID);
        supplier.setKiosqueroId(ownerId);
        return supplier;
    }

    private UpdatePriceListItemMappingResponse whenUpdatingMapping(
            UpdatePriceListItemMappingRequest request) {
        return useCase.execute(request);
    }

    private void thenMappingIsConfirmed(UpdatePriceListItemMappingResponse response) {
        assertEquals(ITEM_ID, response.id());
        assertEquals(PRICE_LIST_ID, response.priceListId());
        assertEquals(INVENTORY_PRODUCT_ID, response.inventoryProductId());
        assertTrue(response.mappingConfirmed());
    }

    private void thenThrowsOperationNotAllowedException(Executable execution) {
        assertThrows(OperationNotAllowedException.class, execution);
    }

    private void thenThrowsIllegalArgumentException(Executable execution) {
        assertThrows(IllegalArgumentException.class, execution);
    }

    private void thenThrowsIllegalArgumentExceptionWithMessage(
            Executable execution, String message) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, execution);
        assertEquals(message, ex.getMessage());
    }
}
