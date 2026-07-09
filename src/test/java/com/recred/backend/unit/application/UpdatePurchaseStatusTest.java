package com.recred.backend.unit.application;

import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.application.UpdatePurchaseStatusRequest;
import com.recred.backend.application.UpdatePurchaseStatusResponse;
import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Notification;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import com.recred.backend.domain.services.PushNotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.recred.backend.domain.models.EstadoCompra.CANCELADO;
import static com.recred.backend.domain.models.EstadoCompra.ENTREGADO;
import static com.recred.backend.domain.models.EstadoCompra.EN_PREPARACION;
import static com.recred.backend.domain.models.EstadoCompra.LISTO;
import static com.recred.backend.domain.models.EstadoCompra.PENDIENTE;
import static com.recred.backend.domain.models.EstadoCompra.RECHAZADO;
import static com.recred.backend.domain.models.EstadoCompra.VENCIDO;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private KiosqueroRealtimeEventPublisher realtimeEventPublisher;
    @Mock
    private MovimientoBilleteraRepository movimientoBilleteraRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PushNotificationSender pushNotificationSender;

    @InjectMocks
    private UpdatePurchaseStatus useCase;

    private UUID purchaseId;
    private UUID buffetId;
    private Purchase purchase;
    private UpdatePurchaseStatusRequest updateRequest;
    private UpdatePurchaseStatusResponse updateResponse;

    @BeforeEach
    void setUp() {
        purchaseId = randomUUID();
        buffetId = randomUUID();
        purchase = purchase(purchaseId, buffetId, PENDIENTE);
    }

    @Test
    void givenExistingPurchase_whenUpdatingStatus_thenSavesStatusAndReturnsResponse() {
        givenPurchaseExists();
        givenPurchaseCanBeSaved();
        givenStatusUpdateRequest(EN_PREPARACION);

        whenUpdatingPurchaseStatus();

        thenResponseStatusIs("EN_PREPARACION");
        thenPurchaseWasSaved();
    }

    @Test
    void givenMissingPurchase_whenUpdatingStatus_thenThrowsException() {
        givenPurchaseDoesNotExist();
        givenStatusUpdateRequest(EN_PREPARACION);

        whenUpdatePurchaseStatusFails(PurchaseNotFoundException.class);
    }

    @Test
    void givenInvalidTrackingTransition_whenUpdatingStatus_thenThrowsException() {
        givenPurchaseExists();
        givenStatusUpdateRequest(CANCELADO);

        whenUpdatePurchaseStatusFails(InvalidPurchaseStatusTransitionException.class);

        thenPurchaseWasNotSaved();
    }

    @Test
    void givenNullRequest_whenUpdatingStatus_thenThrowsException() {
        updateRequest = null;

        whenUpdatePurchaseStatusFails(IllegalArgumentException.class);
    }

    @Test
    void givenAdvancePurchaseReady_whenDeliveringPurchase_thenConsumesReservation() {
        givenAdvancePurchaseInStatus(LISTO);
        givenPurchaseExists();
        givenPurchaseCanBeSaved();
        givenStatusUpdateRequest(ENTREGADO);

        whenUpdatingPurchaseStatus();

        thenReservationWasConsumed();
    }

    @Test
    void givenAdvancePurchasePending_whenRejectingPurchase_thenReleasesReservationAndRefunds() {
        Alumno student = student(randomUUID(), "Juan");
        Billetera wallet = wallet("0.00");
        givenAdvancePurchasePendingFor(student, "100.0");
        givenPurchaseExists();
        givenWalletExistsFor(student, wallet);
        givenPurchaseCanBeSaved();
        givenStatusUpdateRequest(RECHAZADO);

        whenUpdatingPurchaseStatus();

        thenReservationWasReleased();
        thenWalletWasRefunded(wallet, "100.0");
        thenRefundMovementWasRegistered(wallet, "100.0");
    }

    @Test
    void givenAdvancePurchasePending_whenExpiringPurchase_thenReleasesReservationAndPublishesEvent() {
        givenAdvancePurchasePending();
        givenPurchaseExists();
        givenPurchaseCanBeSaved();
        givenStatusUpdateRequest(VENCIDO);

        whenUpdatingPurchaseStatus();

        thenReservationWasReleased();
        thenExpirationEventWasPublished();
    }

    @Test
    void givenStudentTutors_whenStatusChanges_thenNotifiesTutors() {
        Alumno student = student(randomUUID(), "Juan");
        Tutor tutor = tutorWithUser(randomUUID());
        givenPurchaseForStudent(student);
        givenPurchaseExists();
        givenPurchaseCanBeSaved();
        givenTutorsForStudent(student, tutor);
        givenStatusUpdateRequest(EN_PREPARACION);

        whenUpdatingPurchaseStatus();

        thenTutorWasNotified(tutor);
    }

    private void givenPurchaseExists() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
    }

    private void givenPurchaseDoesNotExist() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());
    }

    private void givenPurchaseCanBeSaved() {
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);
    }

    private void givenStatusUpdateRequest(EstadoCompra status) {
        updateRequest = new UpdatePurchaseStatusRequest(purchaseId, status);
    }

    private void givenAdvancePurchaseInStatus(EstadoCompra status) {
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(PENDIENTE);
        if (status == LISTO) {
            purchase.changeStatusTo(EN_PREPARACION);
            purchase.changeStatusTo(LISTO);
        } else {
            purchase.setStatus(status);
        }
    }

    private void givenAdvancePurchasePending() {
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(PENDIENTE);
    }

    private void givenAdvancePurchasePendingFor(Alumno student, String amount) {
        givenAdvancePurchasePending();
        purchase.setStudent(student);
        purchase.setTotalAmount(new BigDecimal(amount));
    }

    private void givenPurchaseForStudent(Alumno student) {
        purchase.setStudent(student);
    }

    private void givenWalletExistsFor(Alumno student, Billetera wallet) {
        when(walletRepository.findByStudentId(student.getId())).thenReturn(Optional.of(wallet));
    }

    private void givenTutorsForStudent(Alumno student, Tutor tutor) {
        when(tutorRepository.findByAlumnoId(student.getId())).thenReturn(List.of(tutor));
    }

    private void whenUpdatingPurchaseStatus() {
        updateResponse = useCase.execute(updateRequest);
    }

    private <T extends Throwable> void whenUpdatePurchaseStatusFails(Class<T> exceptionType) {
        Executable execution = () -> useCase.execute(updateRequest);
        assertThrows(exceptionType, execution);
    }

    private void thenResponseStatusIs(String expectedStatus) {
        assertEquals(expectedStatus, updateResponse.status());
    }

    private void thenPurchaseWasSaved() {
        verify(purchaseRepository).save(purchase);
    }

    private void thenPurchaseWasNotSaved() {
        verify(purchaseRepository, never()).save(any());
    }

    private void thenReservationWasConsumed() {
        verify(inventoryAvailabilityService).consumeReservation(
                buffetId,
                purchase.getItems(),
                purchaseId);
    }

    private void thenReservationWasReleased() {
        verify(inventoryAvailabilityService).releaseReservation(
                buffetId,
                purchase.getItems(),
                purchaseId);
    }

    private void thenWalletWasRefunded(Billetera wallet, String expectedBalance) {
        verify(walletRepository).save(wallet);
        assertEquals(0, new BigDecimal(expectedBalance).compareTo(wallet.getSaldoCreditos()));
    }

    private void thenRefundMovementWasRegistered(Billetera wallet, String expectedAmount) {
        verify(movimientoBilleteraRepository).registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.REEMBOLSO,
                new BigDecimal(expectedAmount),
                "Reembolso por rechazo de compra");
    }

    private void thenExpirationEventWasPublished() {
        verify(realtimeEventPublisher).publish(any(KiosqueroRealtimeEvent.class));
    }

    private void thenTutorWasNotified(Tutor tutor) {
        verify(notificationRepository).save(
                any(Notification.class),
                eq(tutor.getUsuario().getId()));
        verify(pushNotificationSender).send(any(PushNotification.class));
    }

    private Purchase purchase(UUID id, UUID buffetId, EstadoCompra status) {
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setStatus(status);
        purchase.setBuffetId(buffetId);
        return purchase;
    }

    private Alumno student(UUID id, String name) {
        Alumno student = new Alumno();
        student.setId(id);
        student.setNombre(name);
        return student;
    }

    private Billetera wallet(String balance) {
        Billetera wallet = new Billetera();
        wallet.setId(randomUUID());
        wallet.setSaldoCreditos(new BigDecimal(balance));
        return wallet;
    }

    private Tutor tutorWithUser(UUID userId) {
        Usuario tutorUser = new Usuario();
        tutorUser.setId(userId);

        Tutor tutor = new Tutor();
        tutor.setId(randomUUID());
        tutor.setUsuario(tutorUser);
        return tutor;
    }
}
