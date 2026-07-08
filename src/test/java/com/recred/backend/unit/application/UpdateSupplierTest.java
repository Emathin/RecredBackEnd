package com.recred.backend.unit.application;

import com.recred.backend.application.SupplierDetailsResponse;
import com.recred.backend.application.UpdateSupplier;
import com.recred.backend.application.UpdateSupplierRequest;
import com.recred.backend.domain.exceptions.OperationNotAllowedException;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSupplierTest {

    private static final UUID SUPPLIER_ID = UUID.randomUUID();
    private static final UUID KIOSK_OWNER_ID = UUID.randomUUID();
    private static final UUID OTHER_OWNER_ID = UUID.randomUUID();

    private static final String NEW_NAME = "Nuevo proveedor";
    private static final String NEW_PHONE = "1155551111";
    private static final String NEW_EMAIL = "nuevo@supplier.com";
    private static final String NEW_VISIT_DAYS = "Lunes";
    private static final String NEW_NOTES = "Notas nuevas";

    @Mock
    private ProveedorRepository repository;

    private UpdateSupplier useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateSupplier(repository);
    }

    @Test
    void shouldUpdateSupplierWhenOwnerMatches() {
        givenSupplierExistsWithOwner(KIOSK_OWNER_ID);

        SupplierDetailsResponse response = whenUpdatingSupplier();

        thenSupplierWasUpdatedAndSaved(response);
    }

    @Test
    void shouldThrowExceptionWhenSupplierIsNotFound() {
        givenSupplierNotFound();

        Executable execution = this::whenUpdatingSupplier;

        thenThrowsIllegalArgumentExceptionWithMessage(execution, "Supplier not found");
        thenNoSupplierIsSaved();
    }

    @Test
    void shouldThrowExceptionWhenSupplierBelongsToAnotherOwner() {
        givenSupplierExistsWithOwner(OTHER_OWNER_ID);

        Executable execution = this::whenUpdatingSupplier;

        thenThrowsOperationNotAllowedException(execution);
        thenNoSupplierIsSaved();
    }

    private SupplierDetailsResponse whenUpdatingSupplier() {
        UpdateSupplierRequest request = new UpdateSupplierRequest(
                SUPPLIER_ID,
                KIOSK_OWNER_ID,
                "  " + NEW_NAME + "  ",
                NEW_PHONE,
                NEW_EMAIL,
                NEW_VISIT_DAYS,
                NEW_NOTES
        );
        return useCase.execute(request);
    }

    private void givenSupplierExistsWithOwner(UUID ownerId) {
        Proveedor supplier = new Proveedor();
        supplier.setId(SUPPLIER_ID);
        supplier.setKiosqueroId(ownerId);
        supplier.setNombre("Proveedor viejo");
        supplier.setTelefono("1100000000");
        supplier.setEmail("viejo@supplier.com");
        supplier.setDiasVisita("Martes");
        supplier.setNotas("Notas viejas");
        supplier.setCreadoEn(LocalDateTime.of(2026, 7, 1, 10, 0));

        when(repository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));

        if (ownerId.equals(KIOSK_OWNER_ID)) {
            when(repository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }
    }

    private void givenSupplierNotFound() {
        when(repository.findById(SUPPLIER_ID)).thenReturn(Optional.empty());
    }

    private void thenSupplierWasUpdatedAndSaved(SupplierDetailsResponse response) {
        ArgumentCaptor<Proveedor> captor = ArgumentCaptor.forClass(Proveedor.class);
        verify(repository).save(captor.capture());
        
        Proveedor savedSupplier = captor.getValue();
        assertEquals(NEW_NAME, savedSupplier.getNombre());
        assertEquals(NEW_PHONE, savedSupplier.getTelefono());
        assertEquals(NEW_EMAIL, savedSupplier.getEmail());
        assertEquals(NEW_VISIT_DAYS, savedSupplier.getDiasVisita());
        assertEquals(NEW_NOTES, savedSupplier.getNotas());
        assertNotNull(savedSupplier.getActualizadoEn());

        assertEquals(NEW_PHONE, response.phone());
        assertEquals(NEW_EMAIL, response.email());
        assertNotNull(response.updatedAt());
    }

    private void thenThrowsIllegalArgumentExceptionWithMessage(Executable execution, String message) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals(message, exception.getMessage());
    }

    private void thenThrowsOperationNotAllowedException(Executable execution) {
        assertThrows(OperationNotAllowedException.class, execution);
    }

    private void thenNoSupplierIsSaved() {
        verify(repository, never()).save(any(Proveedor.class));
    }
}
