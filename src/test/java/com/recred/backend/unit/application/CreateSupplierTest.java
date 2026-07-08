package com.recred.backend.unit.application;

import com.recred.backend.application.CreateSupplier;
import com.recred.backend.application.CreateSupplierRequest;
import com.recred.backend.application.SupplierDetailsResponse;
import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateSupplierTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-02T12:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final String SUPPLIER_NAME = "Proveedor Test";
    private static final String PHONE = "11223344";
    private static final String EMAIL = "test@supplier.com";
    private static final String VISIT_DAYS = "Lunes y Martes";
    private static final String NOTES = "Notas del proveedor";

    @Mock
    private ProveedorRepository proveedorRepository;
    @Captor
    private ArgumentCaptor<Proveedor> supplierCaptor;

    private CreateSupplier useCase;
    private UUID kioskOwnerId;
    private UUID supplierId;
    private SupplierDetailsResponse response;

    @BeforeEach
    void setUp() {
        kioskOwnerId = UUID.randomUUID();
        supplierId = UUID.randomUUID();
        useCase = new CreateSupplier(proveedorRepository, CLOCK);
    }

    @Test
    void validRequestCreatesSupplierAndReturnsResponse() {
        givenSupplierCanBeSaved();

        whenCreatingSupplier(validRequest());

        thenSupplierWasPersisted();
        thenResponseRepresentsSavedSupplier();
    }

    @Test
    void supplierNameIsTrimmedBeforeSaving() {
        givenSupplierCanBeSaved();

        whenCreatingSupplier(requestWithName("  " + SUPPLIER_NAME + "  "));

        thenSavedSupplierNameIs(SUPPLIER_NAME);
    }

    private void givenSupplierCanBeSaved() {
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(invocation -> {
            Proveedor supplier = invocation.getArgument(0);
            supplier.setId(supplierId);
            return supplier;
        });
    }

    private void whenCreatingSupplier(CreateSupplierRequest request) {
        response = useCase.execute(request);
    }

    private void thenSupplierWasPersisted() {
        verify(proveedorRepository).save(supplierCaptor.capture());
        Proveedor savedSupplier = supplierCaptor.getValue();
        assertEquals(kioskOwnerId, savedSupplier.getKiosqueroId());
        assertEquals(SUPPLIER_NAME, savedSupplier.getNombre());
        assertEquals(PHONE, savedSupplier.getTelefono());
        assertEquals(EMAIL, savedSupplier.getEmail());
        assertEquals(VISIT_DAYS, savedSupplier.getDiasVisita());
        assertEquals(NOTES, savedSupplier.getNotas());
        assertEquals(NOW, savedSupplier.getCreadoEn());
        assertEquals(NOW, savedSupplier.getActualizadoEn());
    }

    private void thenResponseRepresentsSavedSupplier() {
        assertNotNull(response);
        assertEquals(supplierId, response.id());
        assertEquals(kioskOwnerId, response.kioskOwnerId());
        assertEquals(SUPPLIER_NAME, response.name());
        assertEquals(PHONE, response.phone());
        assertEquals(EMAIL, response.email());
        assertEquals(VISIT_DAYS, response.visitDays());
        assertEquals(NOTES, response.notes());
        assertEquals(NOW, response.createdAt());
        assertEquals(NOW, response.updatedAt());
    }

    private void thenSavedSupplierNameIs(String expectedName) {
        verify(proveedorRepository).save(supplierCaptor.capture());
        assertEquals(expectedName, supplierCaptor.getValue().getNombre());
    }

    private CreateSupplierRequest validRequest() {
        return requestWithKioskOwner(kioskOwnerId);
    }

    private CreateSupplierRequest requestWithKioskOwner(UUID requestedKioskOwnerId) {
        return request(requestedKioskOwnerId, SUPPLIER_NAME);
    }

    private CreateSupplierRequest requestWithName(String name) {
        return request(kioskOwnerId, name);
    }

    private CreateSupplierRequest request(UUID requestedKioskOwnerId, String name) {
        return new CreateSupplierRequest(
                requestedKioskOwnerId,
                name,
                PHONE,
                EMAIL,
                VISIT_DAYS,
                NOTES
        );
    }
}
