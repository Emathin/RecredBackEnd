package com.recred.backend.unit.application;

import com.recred.backend.application.GetSupplier;
import com.recred.backend.application.GetSupplierRequest;
import com.recred.backend.application.SupplierDetailsResponse;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetSupplierTest {

    private final ProveedorRepository repository = mock(ProveedorRepository.class);
    private final GetSupplier useCase = new GetSupplier(repository);

    @Test
    void executeShouldReturnOwnedSupplierDetails() {
        UUID supplierId = UUID.randomUUID();
        UUID kioskOwnerId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 4, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 4, 11, 0);
        Proveedor supplier = supplier(supplierId, kioskOwnerId);
        supplier.setTelefono("1133334444");
        supplier.setEmail("ventas@supplier.com");
        supplier.setDiasVisita("Lunes");
        supplier.setNotas("Entrega semanal");
        supplier.setCreadoEn(createdAt);
        supplier.setActualizadoEn(updatedAt);
        GetSupplierRequest request = new GetSupplierRequest(supplierId, kioskOwnerId);

        when(repository.findById(supplierId)).thenReturn(Optional.of(supplier));

        SupplierDetailsResponse response = useCase.execute(request);

        assertEquals(supplierId, response.id());
        assertEquals(kioskOwnerId, response.kioskOwnerId());
        assertEquals("Proveedor", response.name());
        assertEquals("1133334444", response.phone());
        assertEquals("ventas@supplier.com", response.email());
        assertEquals("Lunes", response.visitDays());
        assertEquals("Entrega semanal", response.notes());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertTrue(response.priceLists().isEmpty());
    }

    @Test
    void executeShouldFailWhenSupplierDoesNotExist() {
        UUID supplierId = UUID.randomUUID();
        UUID kioskOwnerId = UUID.randomUUID();
        GetSupplierRequest request = new GetSupplierRequest(supplierId, kioskOwnerId);

        when(repository.findById(supplierId)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException notFound = assertThrows(
                IllegalArgumentException.class,
                execution
        );

        assertEquals("Supplier not found", notFound.getMessage());
    }

    @Test
    void executeShouldFailWhenSupplierBelongsToAnotherKiosquero() {
        UUID supplierId = UUID.randomUUID();
        UUID kioskOwnerId = UUID.randomUUID();
        GetSupplierRequest request = new GetSupplierRequest(supplierId, kioskOwnerId);

        when(repository.findById(supplierId))
                .thenReturn(Optional.of(supplier(supplierId, UUID.randomUUID())));

        Executable execution = () -> useCase.execute(request);

        AccessDeniedException denied = assertThrows(
                AccessDeniedException.class,
                execution
        );

        assertEquals("You do not have access to this supplier", denied.getMessage());
    }

    @Test
    void executeShouldValidateSupplierIdBeforeRepository() {
        GetSupplierRequest request = new GetSupplierRequest(null, UUID.randomUUID());

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException missingSupplier = assertThrows(
                IllegalArgumentException.class,
                execution
        );

        assertEquals("supplierId is required", missingSupplier.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void executeShouldValidateKioskOwnerIdBeforeRepository() {
        GetSupplierRequest request = new GetSupplierRequest(UUID.randomUUID(), null);

        Executable execution = () -> useCase.execute(request);

        IllegalArgumentException missingOwner = assertThrows(
                IllegalArgumentException.class,
                execution
        );

        assertEquals("kioskOwnerId is required", missingOwner.getMessage());
        verifyNoInteractions(repository);
    }

    private Proveedor supplier(UUID supplierId, UUID kioskOwnerId) {
        Proveedor supplier = new Proveedor();
        supplier.setId(supplierId);
        supplier.setKiosqueroId(kioskOwnerId);
        supplier.setNombre("Proveedor");
        return supplier;
    }
}
