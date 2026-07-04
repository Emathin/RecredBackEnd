package com.recred.backend.unit.application;

import com.recred.backend.application.DeleteSupplier;
import com.recred.backend.application.DeleteSupplierRequest;
import com.recred.backend.application.DeleteSupplierResponse;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DeleteSupplierTest {

    private final ProveedorRepository repository = mock(ProveedorRepository.class);
    private final DeleteSupplier useCase = new DeleteSupplier(repository);

    @Test
    void executeShouldDeleteOwnedSupplier() {
        UUID supplierId = UUID.randomUUID();
        UUID kioskOwnerId = UUID.randomUUID();
        Proveedor supplier = supplier(supplierId, kioskOwnerId);
        when(repository.findById(supplierId)).thenReturn(Optional.of(supplier));

        DeleteSupplierResponse response = useCase.execute(
                new DeleteSupplierRequest(supplierId, kioskOwnerId)
        );

        assertEquals(supplierId, response.id());
        verify(repository).delete(supplier);
    }

    @Test
    void executeShouldFailWhenSupplierDoesNotExist() {
        UUID supplierId = UUID.randomUUID();
        UUID kioskOwnerId = UUID.randomUUID();
        when(repository.findById(supplierId)).thenReturn(Optional.empty());

        IllegalArgumentException notFound = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(new DeleteSupplierRequest(supplierId, kioskOwnerId))
        );

        assertEquals("Supplier not found", notFound.getMessage());
        verify(repository).findById(supplierId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void executeShouldFailWhenSupplierBelongsToAnotherOwner() {
        UUID supplierId = UUID.randomUUID();
        UUID kioskOwnerId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();
        when(repository.findById(supplierId))
                .thenReturn(Optional.of(supplier(supplierId, otherOwnerId)));

        AccessDeniedException denied = assertThrows(
                AccessDeniedException.class,
                () -> useCase.execute(new DeleteSupplierRequest(supplierId, kioskOwnerId))
        );

        assertEquals("You do not have access to delete this supplier", denied.getMessage());
        verify(repository).findById(supplierId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void executeShouldValidateSupplierId() {
        IllegalArgumentException missingSupplier = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(new DeleteSupplierRequest(null, UUID.randomUUID()))
        );

        assertEquals("supplierId is required", missingSupplier.getMessage());
    }

    @Test
    void executeShouldValidateKioskOwnerId() {
        IllegalArgumentException missingOwner = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(new DeleteSupplierRequest(UUID.randomUUID(), null))
        );

        assertEquals("kioskOwnerId is required", missingOwner.getMessage());
    }

    private Proveedor supplier(UUID supplierId, UUID kioskOwnerId) {
        Proveedor supplier = new Proveedor();
        supplier.setId(supplierId);
        supplier.setKiosqueroId(kioskOwnerId);
        return supplier;
    }
}
