package com.recred.backend.unit.application;

import com.recred.backend.application.CreateSupplier;
import com.recred.backend.application.CreateSupplierRequest;
import com.recred.backend.application.SupplierDetailsResponse;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateSupplierTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private CreateSupplier createSupplier;

    private UUID kioskOwnerId;

    @BeforeEach
    void setUp() {
        kioskOwnerId = UUID.randomUUID();
    }

    @Test
    void executeWhenRequestIsValidCreatesSupplierAndReturnsResponse() {
        CreateSupplierRequest request = request("Proveedor Test");
        Proveedor savedSupplier = savedSupplier();

        when(proveedorRepository.save(any(Proveedor.class))).thenReturn(savedSupplier);

        SupplierDetailsResponse result = createSupplier.execute(request);

        assertNotNull(result);
        assertEquals(savedSupplier.getId(), result.id());
        assertEquals("Proveedor Test", result.name());
        assertEquals(kioskOwnerId, result.kioskOwnerId());
        verify(proveedorRepository).save(any(Proveedor.class));
    }

    @Test
    void executeWhenNameIsNullThrowsIllegalArgumentException() {
        CreateSupplierRequest request = request(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createSupplier.execute(request)
        );

        assertEquals("Supplier name is required", exception.getMessage());
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    @Test
    void executeWhenNameIsBlankThrowsIllegalArgumentException() {
        CreateSupplierRequest request = request("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createSupplier.execute(request)
        );

        assertEquals("Supplier name is required", exception.getMessage());
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    private CreateSupplierRequest request(String name) {
        return new CreateSupplierRequest(
                kioskOwnerId,
                name,
                "11223344",
                "test@supplier.com",
                "Lunes y Martes",
                "Notas del proveedor"
        );
    }

    private Proveedor savedSupplier() {
        Proveedor supplier = new Proveedor();
        supplier.setId(UUID.randomUUID());
        supplier.setKiosqueroId(kioskOwnerId);
        supplier.setNombre("Proveedor Test");
        supplier.setTelefono("11223344");
        supplier.setEmail("test@supplier.com");
        supplier.setDiasVisita("Lunes y Martes");
        supplier.setNotas("Notas del proveedor");
        return supplier;
    }
}
