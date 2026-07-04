package com.recred.backend.unit.application;

import com.recred.backend.application.SupplierDetailsResponse;
import com.recred.backend.application.UpdateSupplier;
import com.recred.backend.application.UpdateSupplierRequest;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSupplierTest {

    @Mock
    private ProveedorRepository repository;

    private UpdateSupplier useCase;
    private UUID supplierId;
    private UUID kioskOwnerId;

    @BeforeEach
    void setUp() {
        useCase = new UpdateSupplier(repository);
        supplierId = UUID.randomUUID();
        kioskOwnerId = UUID.randomUUID();
    }

    @Test
    void executeShouldUpdateSupplierWhenOwnerMatches() {
        Proveedor supplier = supplier(kioskOwnerId);
        when(repository.findById(supplierId)).thenReturn(Optional.of(supplier));
        when(repository.save(any(Proveedor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupplierDetailsResponse response = useCase.execute(request(
                "  Nuevo proveedor  ",
                kioskOwnerId
        ));

        ArgumentCaptor<Proveedor> captor = ArgumentCaptor.forClass(Proveedor.class);
        verify(repository).save(captor.capture());
        assertEquals("Nuevo proveedor", captor.getValue().getNombre());
        assertEquals("1155551111", response.phone());
        assertEquals("nuevo@supplier.com", response.email());
        assertNotNull(response.updatedAt());
    }

    @Test
    void executeShouldRejectMissingSupplierId() {
        assertEquals("supplierId is required",
                assertThrows(IllegalArgumentException.class,
                        () -> useCase.execute(new UpdateSupplierRequest(
                                null,
                                kioskOwnerId,
                                "Proveedor",
                                null,
                                null,
                                null,
                                null
                        ))).getMessage());
    }

    @Test
    void executeShouldRejectMissingSupplier() {
        when(repository.findById(supplierId)).thenReturn(Optional.empty());

        assertEquals("Supplier not found",
                assertThrows(IllegalArgumentException.class,
                        () -> useCase.execute(request(
                                "Proveedor",
                                kioskOwnerId
                        ))).getMessage());
    }

    @Test
    void executeShouldRejectSupplierFromAnotherOwner() {
        when(repository.findById(supplierId)).thenReturn(Optional.of(supplier(UUID.randomUUID())));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(request("Proveedor", kioskOwnerId)));

        verify(repository, never()).save(any(Proveedor.class));
    }

    private UpdateSupplierRequest request(String name, UUID ownerId) {
        return new UpdateSupplierRequest(
                supplierId,
                ownerId,
                name,
                "1155551111",
                "nuevo@supplier.com",
                "Lunes",
                "Notas nuevas"
        );
    }

    private Proveedor supplier(UUID ownerId) {
        Proveedor supplier = new Proveedor();
        supplier.setId(supplierId);
        supplier.setKiosqueroId(ownerId);
        supplier.setNombre("Proveedor viejo");
        supplier.setTelefono("1100000000");
        supplier.setEmail("viejo@supplier.com");
        supplier.setDiasVisita("Martes");
        supplier.setNotas("Notas viejas");
        supplier.setCreadoEn(LocalDateTime.of(2026, 7, 1, 10, 0));
        return supplier;
    }
}
