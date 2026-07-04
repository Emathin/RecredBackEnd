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
        when(repository.findById(supplierId)).thenReturn(Optional.of(supplier));

        SupplierDetailsResponse response = useCase.execute(
                new GetSupplierRequest(supplierId, kioskOwnerId)
        );

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
    void executeShouldFailWhenMissingOrOwnedByAnotherKiosquero() {
        UUID supplierId = UUID.randomUUID();
        UUID kioskOwnerId = UUID.randomUUID();
        when(repository.findById(supplierId))
                .thenReturn(Optional.empty(), Optional.of(supplier(supplierId, UUID.randomUUID())));

        IllegalArgumentException notFound = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(new GetSupplierRequest(supplierId, kioskOwnerId))
        );
        AccessDeniedException denied = assertThrows(
                AccessDeniedException.class,
                () -> useCase.execute(new GetSupplierRequest(supplierId, kioskOwnerId))
        );

        assertEquals("Supplier not found", notFound.getMessage());
        assertEquals("You do not have access to this supplier", denied.getMessage());
    }

    @Test
    void executeShouldValidateRequestBeforeRepository() {
        IllegalArgumentException missingSupplier = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(new GetSupplierRequest(null, UUID.randomUUID()))
        );
        IllegalArgumentException missingOwner = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(new GetSupplierRequest(UUID.randomUUID(), null))
        );

        assertEquals("supplierId is required", missingSupplier.getMessage());
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
