package com.recred.backend.unit.application;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelPresentialPurchaseTest {

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private InventoryAvailabilityService inventoryAvailabilityService;
    @Mock
    private FranjaHorariaRepository timeSlotRepository;

    @InjectMocks
    private CancelPresentialPurchase useCase;

    private UUID purchaseId;
    private Purchase purchase;
    private Billetera wallet;

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Alumno student = new Alumno();
        student.setId(studentId);

        purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStudent(student);
        purchase.setTotalAmount(new BigDecimal("50.00"));
        purchase.setStatus(EstadoCompra.PENDIENTE);

        wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(new BigDecimal("100.00"));
    }

    @Test
    void execute_WhenPurchaseExists_ShouldCancelAndRefund() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(walletRepository.findByStudentId(purchase.getStudent().getId()))
                .thenReturn(Optional.of(wallet));

        useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId));

        assertEquals(EstadoCompra.CANCELADO, purchase.getStatus());
        assertEquals(new BigDecimal("150.00"), wallet.getSaldoCreditos());
        verify(purchaseRepository).save(purchase);
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_WithShortConstructor_ShouldCancelAndRefundWithoutMovementRepository() {
        CancelPresentialPurchase localUseCase = new CancelPresentialPurchase(
                purchaseRepository,
                walletRepository,
                inventoryAvailabilityService,
                timeSlotRepository
        );
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(walletRepository.findByStudentId(purchase.getStudent().getId()))
                .thenReturn(Optional.of(wallet));

        localUseCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId));

        assertEquals(EstadoCompra.CANCELADO, purchase.getStatus());
        assertEquals(new BigDecimal("150.00"), wallet.getSaldoCreditos());
    }

    @Test
    void execute_WhenAdvancePurchaseIsPending_ShouldReleaseReservation() {
        UUID buffetId = UUID.randomUUID();
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setBuffetId(buffetId);
        purchase.setItems(Map.of(product, 2));

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(walletRepository.findByStudentId(purchase.getStudent().getId()))
                .thenReturn(Optional.of(wallet));

        useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId));

        verify(inventoryAvailabilityService).releaseReservation(
                buffetId,
                purchase.getItems(),
                purchaseId
        );
    }

    @Test
    void execute_WhenPurchaseIsNotPending_ShouldThrowException() {
        purchase.setStatus(EstadoCompra.ENTREGADO);

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        assertThrows(
                InvalidPurchaseStatusTransitionException.class,
                () -> useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId))
        );

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenPurchaseNotFound_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId)));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenWalletNotFound_ShouldThrowException() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(walletRepository.findByStudentId(purchase.getStudent().getId()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId)));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenPurchaseHasNoStudent_ShouldThrowException() {
        purchase.setStudent(null);
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId)));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenPurchaseStudentHasNoId_ShouldThrowException() {
        purchase.getStudent().setId(null);
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId)));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenAdvancePurchaseIsMoreThanOneHourBeforePickup_ShouldCancelAndRefund() {
        purchase.setTipo(TipoCompra.ANTICIPADA);
        UUID slotId = UUID.randomUUID();
        purchase.setPickupSlotId(slotId);

        LocalDateTime allowedPickupTime = LocalDateTime.now(
                ZoneId.of("America/Argentina/Buenos_Aires")).plusMinutes(61);
        purchase.setPickupDate(allowedPickupTime.toLocalDate());

        FranjaHoraria franja = new FranjaHoraria();
        franja.setId(slotId);
        franja.setHoraInicio(allowedPickupTime.toLocalTime());

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(franja));
        when(walletRepository.findByStudentId(purchase.getStudent().getId()))
                .thenReturn(Optional.of(wallet));

        useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId));

        assertEquals(EstadoCompra.CANCELADO, purchase.getStatus());
        assertEquals(new BigDecimal("150.00"), wallet.getSaldoCreditos());
        verify(purchaseRepository).save(purchase);
        verify(walletRepository).save(wallet);
    }

    @Test
    void execute_WhenAdvancePurchaseIsExactlyOneHourBeforePickup_ShouldThrowException() {
        purchase.setTipo(TipoCompra.ANTICIPADA);
        UUID slotId = UUID.randomUUID();
        purchase.setPickupSlotId(slotId);

        LocalDateTime blockedPickupTime = LocalDateTime.now(
                ZoneId.of("America/Argentina/Buenos_Aires")).plusHours(1);
        purchase.setPickupDate(blockedPickupTime.toLocalDate());

        FranjaHoraria franja = new FranjaHoraria();
        franja.setId(slotId);
        franja.setHoraInicio(blockedPickupTime.toLocalTime());

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(franja));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId)));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenAdvancePurchaseTimeSlotDoesNotExist_ShouldThrowException() {
        purchase.setTipo(TipoCompra.ANTICIPADA);
        UUID slotId = UUID.randomUUID();
        purchase.setPickupSlotId(slotId);
        purchase.setPickupDate(LocalDateTime.now(
                ZoneId.of("America/Argentina/Buenos_Aires")).plusHours(2).toLocalDate());

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId)));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenAdvancePurchaseIsLessThanOneHourBeforePickup_ShouldThrowException() {
        purchase.setTipo(TipoCompra.ANTICIPADA);
        UUID slotId = UUID.randomUUID();
        purchase.setPickupSlotId(slotId);

        LocalDateTime blockedPickupTime = LocalDateTime.now(
                ZoneId.of("America/Argentina/Buenos_Aires")).plusMinutes(59);
        purchase.setPickupDate(blockedPickupTime.toLocalDate());

        FranjaHoraria franja = new FranjaHoraria();
        franja.setId(slotId);
        franja.setHoraInicio(blockedPickupTime.toLocalTime());

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(franja));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId)));

        verify(walletRepository, never()).save(any());
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void execute_WhenMovementRepositoryExists_ShouldRegisterRefundMovement() {
        MovimientoBilleteraRepository movimientoBilleteraRepository =
                mock(MovimientoBilleteraRepository.class);
        CancelPresentialPurchase localUseCase = new CancelPresentialPurchase(
                purchaseRepository,
                walletRepository,
                inventoryAvailabilityService,
                timeSlotRepository,
                movimientoBilleteraRepository
        );
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(walletRepository.findByStudentId(purchase.getStudent().getId()))
                .thenReturn(Optional.of(wallet));

        localUseCase.execute(new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId));

        verify(movimientoBilleteraRepository).registrarMovimientoSistema(
                eq(wallet.getId()),
                eq(TipoMovimientoBilletera.REEMBOLSO),
                eq(new BigDecimal("50.00")),
                eq("Reembolso por cancelacion de compra")
        );
    }
}
