package com.recred.backend.unit.application;

import com.recred.backend.application.DeleteSupplier;
import com.recred.backend.application.DeleteSupplierRequest;
import com.recred.backend.application.DeleteSupplierResponse;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DeleteSupplierTest {

    private static final UUID SUPPLIER_ID = UUID.randomUUID();
    private static final UUID KIOSK_OWNER_ID = UUID.randomUUID();
    private static final UUID OTHER_OWNER_ID = UUID.randomUUID();

    private final ProveedorRepository repository = mock(ProveedorRepository.class);
    private final DeleteSupplier useCase = new DeleteSupplier(repository);

    @Test
    void deleteOwnedSupplierSuccessfully() {
        Proveedor supplier = supplier(SUPPLIER_ID, KIOSK_OWNER_ID);
        givenAnExistingSupplier(supplier);

        DeleteSupplierResponse response = whenExecuteUseCase();

        thenSupplierShouldBeDeletedAndResponseIsReturned(response, supplier);
    }

    @Test
    void throwExceptionWhenSupplierDoesNotExist() {
        givenSupplierNotFound();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, this::whenExecuteUseCase);

        assertEquals("Supplier not found", exception.getMessage());
        thenNoMoreInteractionsWithRepository();
    }

    @Test
    void throwExceptionWhenSupplierBelongsToAnotherOwner() {
        Proveedor supplier = supplier(SUPPLIER_ID, OTHER_OWNER_ID);
        givenAnExistingSupplier(supplier);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, this::whenExecuteUseCase);

        assertEquals("You do not have access to delete this supplier", exception.getMessage());
        thenNoMoreInteractionsWithRepository();
    }

    private void givenAnExistingSupplier(Proveedor supplier) {
        when(repository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
    }

    private void givenSupplierNotFound() {
        when(repository.findById(SUPPLIER_ID)).thenReturn(Optional.empty());
    }

    private DeleteSupplierResponse whenExecuteUseCase() {
        return useCase.execute(new DeleteSupplierRequest(SUPPLIER_ID, KIOSK_OWNER_ID));
    }

    private void thenSupplierShouldBeDeletedAndResponseIsReturned(DeleteSupplierResponse response, Proveedor supplier) {
        assertEquals(SUPPLIER_ID, response.id());
        verify(repository).delete(supplier);
    }

    private void thenNoMoreInteractionsWithRepository() {
        verify(repository).findById(SUPPLIER_ID);
        verifyNoMoreInteractions(repository);
    }

    private Proveedor supplier(UUID supplierId, UUID kioskOwnerId) {
        Proveedor supplier = new Proveedor();
        supplier.setId(supplierId);
        supplier.setKiosqueroId(kioskOwnerId);
        return supplier;
    }
}
