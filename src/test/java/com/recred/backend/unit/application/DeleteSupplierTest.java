package com.recred.backend.unit.application;

import com.recred.backend.application.DeleteSupplier;
import com.recred.backend.application.DeleteSupplierRequest;
import com.recred.backend.application.DeleteSupplierResponse;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
        DeleteSupplierRequest request = new DeleteSupplierRequest(supplierId, kioskOwnerId);

        when(repository.findById(supplierId)).thenReturn(Optional.of(supplier));

        DeleteSupplierResponse response = useCase.execute(request);

        assertEquals(supplierId, response.id());
        verify(repository).delete(supplier);
    }

    @Test
    void executeShouldFailWhenSupplierDoesNotExist() {
        UUID supplierId = UUID.randomUUID();
        UUID kioskOwnerId = UUID.randomUUID();
        DeleteSupplierRequest request = new DeleteSupplierRequest(supplierId, kioskOwnerId);

        when(repository.findById(supplierId)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException notFound = assertThrows(
                IllegalArgumentException.class,
                execution
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
        DeleteSupplierRequest request = new DeleteSupplierRequest(supplierId, kioskOwnerId);

        when(repository.findById(supplierId))
                .thenReturn(Optional.of(supplier(supplierId, otherOwnerId)));

        Executable execution = () -> useCase.execute(request);

        AccessDeniedException denied = assertThrows(
                AccessDeniedException.class,
                execution
        );

        assertEquals("You do not have access to delete this supplier", denied.getMessage());
        verify(repository).findById(supplierId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void executeShouldValidateSupplierId() {
        DeleteSupplierRequest request = new DeleteSupplierRequest(null, UUID.randomUUID());

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException missingSupplier = assertThrows(
                IllegalArgumentException.class,
                execution
        );

        assertEquals("supplierId is required", missingSupplier.getMessage());
    }

    @Test
    void executeShouldValidateKioskOwnerId() {
        DeleteSupplierRequest request = new DeleteSupplierRequest(UUID.randomUUID(), null);

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException missingOwner = assertThrows(
                IllegalArgumentException.class,
                execution
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
