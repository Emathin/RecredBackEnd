package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePurchaseStatusTest {

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private InventoryAvailabilityService inventoryAvailabilityService;
    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private UpdatePurchaseStatus useCase;

    private UUID purchaseId;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();

        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());

        purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStudent(student);
        purchase.setStatus(EstadoCompra.PENDIENTE);
    }

    @Test
    void execute_WhenTransitionIsValid_ShouldSaveAndReturnStatus() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(purchaseRepository.save(purchase)).thenReturn(purchase);

        PurchaseStatus result = useCase.execute(purchaseId, EstadoCompra.EN_PREPARACION);

        assertEquals(EstadoCompra.EN_PREPARACION, purchase.getStatus());
        assertEquals(EstadoCompra.EN_PREPARACION, result.status());
        verify(purchaseRepository).save(purchase);
    }

    @Test
    void execute_WhenTransitionIsInvalid_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> useCase.execute(purchaseId, EstadoCompra.LISTO));

        verify(purchaseRepository, never()).save(purchase);
    }

    @Test
    void execute_WhenRequestedStatusIsCancelled_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> useCase.execute(purchaseId, EstadoCompra.CANCELADO));

        verify(purchaseRepository, never()).save(purchase);
    }

    @Test
    void execute_WhenAdvancePurchaseIsDelivered_ShouldConsumeReservation() {
        UUID buffetId = UUID.randomUUID();
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        purchase.setStatus(EstadoCompra.LISTO);
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setBuffetId(buffetId);
        purchase.setItems(Map.of(product, 1));
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(purchaseRepository.save(purchase)).thenReturn(purchase);

        useCase.execute(purchaseId, EstadoCompra.ENTREGADO);

        verify(inventoryAvailabilityService).consumeReservation(
                buffetId,
                purchase.getItems(),
                purchaseId);
    }

    @Test
    void execute_WhenAdvancePurchaseAlreadyDelivered_ShouldNotConsumeReservationAgain() {
        UUID buffetId = UUID.randomUUID();
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        purchase.setStatus(EstadoCompra.ENTREGADO);
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setBuffetId(buffetId);
        purchase.setItems(Map.of(product, 1));
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> useCase.execute(purchaseId, EstadoCompra.ENTREGADO));

        verify(inventoryAvailabilityService, never())
                .consumeReservation(buffetId, purchase.getItems());
        verify(purchaseRepository, never()).save(purchase);
    }

    @Test
    void execute_WhenAdvancePurchaseIsRejected_ShouldReleaseReservation() {
        UUID buffetId = UUID.randomUUID();
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setBuffetId(buffetId);
        purchase.setItems(Map.of(product, 1));
        purchase.setTotalAmount(new BigDecimal("25.00"));
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(BigDecimal.ZERO);
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(purchaseRepository.save(purchase)).thenReturn(purchase);
        when(walletRepository.findByStudentId(purchase.getStudent().getId()))
                .thenReturn(Optional.of(wallet));

        useCase.execute(purchaseId, EstadoCompra.RECHAZADO);

        verify(inventoryAvailabilityService).releaseReservation(
                buffetId,
                purchase.getItems(),
                purchaseId);
        verify(walletRepository).save(wallet);
        assertEquals(new BigDecimal("25.00"), wallet.getSaldoCreditos());
    }

    @Test
    void execute_WhenPurchaseDoesNotExist_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());

        assertThrows(PurchaseNotFoundException.class,
                () -> useCase.execute(purchaseId, EstadoCompra.EN_PREPARACION));
    }
}
