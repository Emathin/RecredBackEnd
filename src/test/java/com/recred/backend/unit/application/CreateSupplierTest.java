package com.recred.backend.unit.application;

import com.recred.backend.application.CreateSupplier;
import com.recred.backend.application.CreateSupplierCommand;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSupplierTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private CreateSupplier createSupplier;

    private UUID kiosqueroId;

    @BeforeEach
    void setUp() {
        kiosqueroId = UUID.randomUUID();
    }

    @Test
    void execute_CuandoTodoEsValido_CreaYRetornaProveedor() {
        // Arrange
        CreateSupplierCommand command = new CreateSupplierCommand(
                kiosqueroId,
                "Proveedor Test",
                "11223344",
                "test@supplier.com",
                "Lunes y Martes",
                "Notas del proveedor"
        );

        Proveedor savedProveedor = new Proveedor();
        savedProveedor.setId(UUID.randomUUID());
        savedProveedor.setKiosqueroId(kiosqueroId);
        savedProveedor.setNombre("Proveedor Test");
        savedProveedor.setTelefono("11223344");
        savedProveedor.setEmail("test@supplier.com");
        savedProveedor.setDiasVisita("Lunes y Martes");
        savedProveedor.setNotas("Notas del proveedor");

        when(proveedorRepository.save(any(Proveedor.class))).thenReturn(savedProveedor);

        // Act
        Proveedor result = createSupplier.execute(command);

        // Assert
        assertNotNull(result);
        assertEquals(savedProveedor.getId(), result.getId());
        assertEquals("Proveedor Test", result.getNombre());
        assertEquals(kiosqueroId, result.getKiosqueroId());

        verify(proveedorRepository).save(any(Proveedor.class));
    }

    @Test
    void execute_CuandoNombreEsNulo_LanzaIllegalArgumentException() {
        // Arrange
        CreateSupplierCommand command = new CreateSupplierCommand(
                kiosqueroId,
                null,
                "11223344",
                "test@supplier.com",
                "Lunes",
                "Notas"
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            createSupplier.execute(command);
        });

        assertEquals("El nombre del proveedor es obligatorio", exception.getMessage());
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    @Test
    void execute_CuandoNombreEstaVacio_LanzaIllegalArgumentException() {
        // Arrange
        CreateSupplierCommand command = new CreateSupplierCommand(
                kiosqueroId,
                "   ",
                "11223344",
                "test@supplier.com",
                "Lunes",
                "Notas"
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            createSupplier.execute(command);
        });

        assertEquals("El nombre del proveedor es obligatorio", exception.getMessage());
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }
}
