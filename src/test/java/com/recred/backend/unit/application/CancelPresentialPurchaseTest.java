package com.recred.backend.unit.application;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CancelPresentialPurchaseRequest;
import com.recred.backend.application.CancelPresentialPurchaseResponse;
import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
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
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelPresentialPurchaseTest {

    private static final BigDecimal PURCHASE_TOTAL = new BigDecimal("50.00");
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100.00");
    private static final BigDecimal REFUNDED_BALANCE = new BigDecimal("150.00");
    private static final String REFUND_DESCRIPTION = "Reembolso por cancelacion de compra";
    private static final ZoneId ZONE_ID = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-02T12:00:00Z"),
            ZONE_ID);

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private InventoryAvailabilityService inventoryAvailabilityService;
    @Mock
    private FranjaHorariaRepository timeSlotRepository;
    @Mock
    private MovimientoBilleteraRepository movimientoBilleteraRepository;

    private CancelPresentialPurchase useCase;
    private UUID purchaseId;
    private UUID studentId;
    private Purchase purchase;
    private Billetera wallet;
    private CancelPresentialPurchaseResponse response;
    private Throwable thrownException;
    private LocalDateTime pickupTime;

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        purchase = pendingPurchase();
        wallet = wallet();
        useCase = new CancelPresentialPurchase(
                purchaseRepository,
                walletRepository,
                inventoryAvailabilityService,
                timeSlotRepository,
                movimientoBilleteraRepository,
                CLOCK
        );
    }

    @Test
    void pendingPurchaseCanBeCancelledAndRefunded() {
        givenPurchaseExists();
        givenWalletExists();

        whenCancellingPurchase();

        thenPurchaseWasCancelled();
        thenWalletWasRefunded();
        thenPurchaseWasSaved();
        thenWalletWasSaved();
        thenRefundMovementWasRegistered();
    }

    @Test
    void matchingStudentCanCancelPurchase() {
        givenPurchaseExists();
        givenWalletExists();
        givenPurchaseWillBeSaved();

        whenCancellingPurchase(requestForStudent(studentId));

        thenResponseRepresentsCancelledPurchase();
        thenWalletWasRefunded();
        thenPurchaseWasSaved();
        thenWalletWasSaved();
    }

    @Test
    void differentStudentCannotCancelPurchase() {
        givenPurchaseExists();

        whenCancellationFails(
                requestForStudent(UUID.randomUUID()),
                PurchaseAccessDeniedException.class);

        thenErrorMessageIs("No tiene permisos para acceder a la compra " + purchaseId);
        thenWalletWasNotSaved();
        thenPurchaseWasNotSaved();
        thenNoRefundMovementWasRegistered();
    }

    @Test
    void movementRegistrationCanStayDisabled() {
        CancelPresentialPurchase useCaseWithoutMovementRepository =
                useCaseWithoutMovementRepository();
        givenPurchaseExists();
        givenWalletExists();

        useCaseWithoutMovementRepository.execute(defaultRequest());

        thenPurchaseWasCancelled();
        thenWalletWasRefunded();
        thenPurchaseWasSaved();
        thenWalletWasSaved();
        thenNoRefundMovementWasRegistered();
    }

    @Test
    void pendingAdvancePurchaseReleasesReservationWhenCancelled() {
        givenPendingAdvancePurchaseWithPickupAt(minutesFromNow(61));
        givenPurchaseExists();
        givenPickupSlotExists();
        givenWalletExists();

        whenCancellingPurchase();

        thenReservationWasReleased();
    }

    @Test
    void deliveredPurchaseCannotBeCancelled() {
        purchase.setStatus(EstadoCompra.ENTREGADO);
        givenPurchaseExists();

        whenCancellationFails(InvalidPurchaseStatusTransitionException.class);

        thenErrorMessageIs("No se puede cambiar el estado de ENTREGADO a CANCELADO");
        thenWalletWasNotSaved();
        thenPurchaseWasNotSaved();
    }

    @Test
    void missingPurchaseStopsCancellation() {
        givenPurchaseDoesNotExist();

        whenCancellationFails(IllegalArgumentException.class);

        thenErrorMessageIs("Purchase not found: " + purchaseId);
        thenWalletWasNotSaved();
        thenPurchaseWasNotSaved();
    }

    @Test
    void missingWalletStopsRefundAndPersistence() {
        givenPurchaseExists();
        givenWalletDoesNotExist();

        whenCancellationFails(IllegalStateException.class);

        thenErrorMessageIs("Student wallet not found for refund.");
        thenWalletWasNotSaved();
        thenPurchaseWasNotSaved();
    }

    @Test
    void purchaseWithoutStudentCannotBeRefunded() {
        purchase.setStudent(null);
        givenPurchaseExists();

        whenCancellationFails(IllegalStateException.class);

        thenErrorMessageIs("Purchase does not have an associated student.");
        thenWalletWasNotSaved();
        thenPurchaseWasNotSaved();
    }

    @Test
    void purchaseWithStudentWithoutIdCannotBeRefunded() {
        purchase.getStudent().setId(null);
        givenPurchaseExists();

        whenCancellationFails(IllegalStateException.class);

        thenErrorMessageIs("Purchase does not have an associated student.");
        thenWalletWasNotSaved();
        thenPurchaseWasNotSaved();
    }

    @Test
    void advancePurchaseMoreThanOneHourBeforePickupCanBeCancelled() {
        givenPendingAdvancePurchaseWithPickupAt(minutesFromNow(61));
        givenPurchaseExists();
        givenPickupSlotExists();
        givenWalletExists();

        whenCancellingPurchase();

        thenPurchaseWasCancelled();
        thenWalletWasRefunded();
        thenPurchaseWasSaved();
        thenWalletWasSaved();
    }

    @Test
    void advancePurchaseExactlyOneHourBeforePickupCannotBeCancelled() {
        givenPendingAdvancePurchaseWithPickupAt(hoursFromNow(1));
        givenPurchaseExists();
        givenPickupSlotExists();

        whenCancellationFails(IllegalArgumentException.class);

        thenErrorMessageIs(
                "Cannot cancel an order less than an hour before the pickup time starts");
        thenWalletWasNotSaved();
        thenPurchaseWasNotSaved();
    }

    @Test
    void advancePurchaseWithoutTimeSlotCannotBeCancelled() {
        givenPendingAdvancePurchaseWithPickupAt(hoursFromNow(2));
        givenPurchaseExists();
        givenPickupSlotDoesNotExist();

        whenCancellationFails(IllegalStateException.class);

        thenErrorMessageIs("Time slot not found for purchase");
        thenWalletWasNotSaved();
        thenPurchaseWasNotSaved();
    }

    @Test
    void advancePurchaseLessThanOneHourBeforePickupCannotBeCancelled() {
        givenPendingAdvancePurchaseWithPickupAt(minutesFromNow(59));
        givenPurchaseExists();
        givenPickupSlotExists();

        whenCancellationFails(IllegalArgumentException.class);

        thenErrorMessageIs(
                "Cannot cancel an order less than an hour before the pickup time starts");
        thenWalletWasNotSaved();
        thenPurchaseWasNotSaved();
    }

    @Test
    void refundRegistersSystemMovementWhenMovementRepositoryExists() {
        givenPurchaseExists();
        givenWalletExists();

        whenCancellingPurchase();

        thenRefundMovementWasRegistered();
    }

    private void givenPurchaseExists() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
    }

    private void givenPurchaseDoesNotExist() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());
    }

    private void givenWalletExists() {
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
    }

    private void givenWalletDoesNotExist() {
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());
    }

    private void givenPurchaseWillBeSaved() {
        when(purchaseRepository.save(purchase)).thenReturn(purchase);
    }

    private void givenPendingAdvancePurchaseWithPickupAt(LocalDateTime pickupTime) {
        UUID buffetId = UUID.randomUUID();
        Producto product = product();
        this.pickupTime = pickupTime;
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setBuffetId(buffetId);
        purchase.setItems(Map.of(product, 2));
        purchase.setPickupSlotId(UUID.randomUUID());
        purchase.setPickupDate(pickupTime.toLocalDate());
    }

    private void givenPickupSlotExists() {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(purchase.getPickupSlotId());
        timeSlot.setHoraInicio(pickupStartTime());
        when(timeSlotRepository.findById(purchase.getPickupSlotId()))
                .thenReturn(Optional.of(timeSlot));
    }

    private void givenPickupSlotDoesNotExist() {
        when(timeSlotRepository.findById(purchase.getPickupSlotId()))
                .thenReturn(Optional.empty());
    }

    private void whenCancellingPurchase() {
        whenCancellingPurchase(defaultRequest());
    }

    private void whenCancellingPurchase(CancelPresentialPurchaseRequest request) {
        response = useCase.execute(request);
    }

    private <T extends Throwable> void whenCancellationFails(Class<T> exceptionType) {
        whenCancellationFails(defaultRequest(), exceptionType);
    }

    private <T extends Throwable> void whenCancellationFails(
            CancelPresentialPurchaseRequest request,
            Class<T> exceptionType) {
        Executable execution = () -> useCase.execute(request);
        thrownException = assertThrows(exceptionType, execution);
    }

    private void thenPurchaseWasCancelled() {
        assertEquals(EstadoCompra.CANCELADO, purchase.getStatus());
    }

    private void thenWalletWasRefunded() {
        assertEquals(REFUNDED_BALANCE, wallet.getSaldoCreditos());
    }

    private void thenPurchaseWasSaved() {
        verify(purchaseRepository).save(purchase);
    }

    private void thenWalletWasSaved() {
        verify(walletRepository).save(wallet);
    }

    private void thenWalletWasNotSaved() {
        verify(walletRepository, never()).save(any());
    }

    private void thenPurchaseWasNotSaved() {
        verify(purchaseRepository, never()).save(any());
    }

    private void thenReservationWasReleased() {
        verify(inventoryAvailabilityService).releaseReservation(
                purchase.getBuffetId(),
                purchase.getItems(),
                purchaseId
        );
    }

    private void thenRefundMovementWasRegistered() {
        verify(movimientoBilleteraRepository).registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.REEMBOLSO,
                PURCHASE_TOTAL,
                REFUND_DESCRIPTION
        );
    }

    private void thenNoRefundMovementWasRegistered() {
        verifyNoInteractions(movimientoBilleteraRepository);
    }

    private void thenResponseRepresentsCancelledPurchase() {
        assertEquals(EstadoCompra.CANCELADO, response.status());
        assertEquals(purchaseId, response.purchaseId());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private CancelPresentialPurchase useCaseWithoutMovementRepository() {
        return new CancelPresentialPurchase(
                purchaseRepository,
                walletRepository,
                inventoryAvailabilityService,
                timeSlotRepository
        );
    }

    private CancelPresentialPurchaseRequest defaultRequest() {
        return new CancelPresentialPurchaseRequest(purchaseId);
    }

    private CancelPresentialPurchaseRequest requestForStudent(UUID requestStudentId) {
        return new CancelPresentialPurchaseRequest(purchaseId, requestStudentId);
    }

    private Purchase pendingPurchase() {
        Purchase pendingPurchase = new Purchase();
        pendingPurchase.setId(purchaseId);
        pendingPurchase.setStudent(student());
        pendingPurchase.setTotalAmount(PURCHASE_TOTAL);
        pendingPurchase.setStatus(EstadoCompra.PENDIENTE);
        return pendingPurchase;
    }

    private Alumno student() {
        Alumno student = new Alumno();
        student.setId(studentId);
        return student;
    }

    private Billetera wallet() {
        Billetera studentWallet = new Billetera();
        studentWallet.setId(UUID.randomUUID());
        studentWallet.setSaldoCreditos(INITIAL_BALANCE);
        return studentWallet;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        return product;
    }

    private LocalDateTime minutesFromNow(int minutes) {
        return now().plusMinutes(minutes);
    }

    private LocalDateTime hoursFromNow(int hours) {
        return now().plusHours(hours);
    }

    private java.time.LocalTime pickupStartTime() {
        return pickupTime.toLocalTime();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
