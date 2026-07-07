package com.recred.backend.unit.application;

import com.recred.backend.application.GetSupplier;
import com.recred.backend.application.GetSupplierRequest;
import com.recred.backend.application.SupplierDetailsResponse;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSupplierTest {

    @Mock
    private ProveedorRepository repository;

    private GetSupplier useCase;
    private UUID supplierId;
    private UUID kioskOwnerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @BeforeEach
    void setUp() {
        useCase = new GetSupplier(repository);
    }

    @Test
    void givenOwnedSupplierExists_whenGettingSupplier_thenReturnsSupplierDetails() {
        givenOwnedSupplierExists();

        SupplierDetailsResponse response = whenGetSupplier();

        thenSupplierDetailsWereReturned(response);
    }

    @Test
    void givenSupplierDoesNotExist_whenGettingSupplier_thenThrowsException() {
        givenSupplierDoesNotExist();

        IllegalArgumentException exception = whenGetSupplierFails(
                IllegalArgumentException.class);

        thenSupplierNotFoundWasThrown(exception);
    }

    @Test
    void givenSupplierBelongsToAnotherKiosquero_whenGettingSupplier_thenThrowsException() {
        givenSupplierBelongsToAnotherKiosquero();

        SecurityException exception = whenGetSupplierFails(SecurityException.class);

        thenAccessDeniedWasThrown(exception);
    }

    private void givenOwnedSupplierExists() {
        supplierId = randomUUID();
        kioskOwnerId = randomUUID();
        createdAt = LocalDateTime.of(2026, 7, 4, 10, 0);
        updatedAt = LocalDateTime.of(2026, 7, 4, 11, 0);

        Proveedor supplier = supplier(supplierId, kioskOwnerId);
        supplier.setTelefono("1133334444");
        supplier.setEmail("ventas@supplier.com");
        supplier.setDiasVisita("Lunes");
        supplier.setNotas("Entrega semanal");
        supplier.setCreadoEn(createdAt);
        supplier.setActualizadoEn(updatedAt);
        when(repository.findById(supplierId)).thenReturn(Optional.of(supplier));
    }

    private void givenSupplierDoesNotExist() {
        supplierId = randomUUID();
        kioskOwnerId = randomUUID();
        when(repository.findById(supplierId)).thenReturn(Optional.empty());
    }

    private void givenSupplierBelongsToAnotherKiosquero() {
        supplierId = randomUUID();
        kioskOwnerId = randomUUID();
        when(repository.findById(supplierId))
                .thenReturn(Optional.of(supplier(supplierId, randomUUID())));
    }

    private SupplierDetailsResponse whenGetSupplier() {
        return useCase.execute(new GetSupplierRequest(supplierId, kioskOwnerId));
    }

    private <T extends RuntimeException> T whenGetSupplierFails(Class<T> exceptionType) {
        return assertThrows(exceptionType, executeUseCase());
    }

    private void thenSupplierDetailsWereReturned(SupplierDetailsResponse response) {
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

    private void thenSupplierNotFoundWasThrown(IllegalArgumentException exception) {
        assertEquals("Supplier not found", exception.getMessage());
    }

    private void thenAccessDeniedWasThrown(SecurityException exception) {
        assertEquals("You do not have access to this supplier", exception.getMessage());
    }

    private Executable executeUseCase() {
        return () -> useCase.execute(new GetSupplierRequest(supplierId, kioskOwnerId));
    }

    private Proveedor supplier(UUID requestedSupplierId, UUID requestedKioskOwnerId) {
        Proveedor supplier = new Proveedor();
        supplier.setId(requestedSupplierId);
        supplier.setKiosqueroId(requestedKioskOwnerId);
        supplier.setNombre("Proveedor");
        return supplier;
    }
}
