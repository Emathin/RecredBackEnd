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
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

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
    private Purchase purchase;
    private UUID buffetId;

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setBuffetId(buffetId);
    }

    @Test
    void execute_WhenPurchaseExists_ShouldUpdateStatus() {
        givenPurchaseExists();
        givenPurchaseCanBeSaved();

        UpdatePurchaseStatusResponse response = whenUpdatingStatusTo(
                EstadoCompra.EN_PREPARACION);

        assertEquals("EN_PREPARACION", response.status());
        verify(purchaseRepository).save(purchase);
    }

    @Test
    void execute_WhenPurchaseNotFound_ShouldThrowException() {
        givenPurchaseDoesNotExist();

        assertThrows(PurchaseNotFoundException.class,
                () -> whenUpdatingStatusTo(EstadoCompra.EN_PREPARACION));
    }

    @Test
    void execute_WhenRequestedStatusIsCancelado_ShouldThrowException() {
        givenPurchaseExists();

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> whenUpdatingStatusTo(EstadoCompra.CANCELADO));
    }

    @Test
    void execute_WhenRequestIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_WhenAdvancePurchaseDelivered_ShouldConsumeReservation() {
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.changeStatusTo(EstadoCompra.EN_PREPARACION);
        purchase.changeStatusTo(EstadoCompra.LISTO);
        givenPurchaseExists();
        givenPurchaseCanBeSaved();

        whenUpdatingStatusTo(EstadoCompra.ENTREGADO);

        verify(inventoryAvailabilityService).consumeReservation(
                buffetId, purchase.getItems(), purchaseId);
    }

    @Test
    void execute_WhenAdvancePurchaseRejected_ShouldReleaseReservationAndRefund() {
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);

        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        purchase.setStudent(student);
        purchase.setTotalAmount(new BigDecimal("100.0"));

        Billetera wallet = new Billetera();
        wallet.setId(UUID.randomUUID());
        wallet.setSaldoCreditos(BigDecimal.ZERO);

        givenPurchaseExists();
        givenWalletExistsFor(student, wallet);
        givenPurchaseCanBeSaved();
        whenUpdatingStatusTo(EstadoCompra.RECHAZADO);

        verify(inventoryAvailabilityService).releaseReservation(
                buffetId, purchase.getItems(), purchaseId);
        verify(walletRepository).save(wallet);
        assertEquals(new BigDecimal("100.0"), wallet.getSaldoCreditos());
        verify(movimientoBilleteraRepository).registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.REEMBOLSO,
                new BigDecimal("100.0"),
                "Reembolso por rechazo de compra"
        );
    }

    @Test
    void execute_WhenAdvancePurchaseExpired_ShouldReleaseReservationAndPublishEvent() {
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        givenPurchaseExists();
        givenPurchaseCanBeSaved();

        whenUpdatingStatusTo(EstadoCompra.VENCIDO);

        verify(inventoryAvailabilityService).releaseReservation(
                buffetId, purchase.getItems(), purchaseId);
        verify(realtimeEventPublisher).publish(any(KiosqueroRealtimeEvent.class));
    }

    @Test
    void execute_WhenStatusChanges_ShouldNotifyStudentTutors() {
        purchase.setStatus(EstadoCompra.PENDIENTE);

        Alumno student = new Alumno();
        UUID studentId = UUID.randomUUID();
        student.setId(studentId);
        student.setNombre("Juan");
        purchase.setStudent(student);

        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        Usuario tutorUser = new Usuario();
        UUID tutorUserId = UUID.randomUUID();
        tutorUser.setId(tutorUserId);
        tutor.setUsuario(tutorUser);

        givenPurchaseExists();
        givenPurchaseCanBeSaved();
        when(tutorRepository.findByAlumnoId(studentId))
                .thenReturn(java.util.List.of(tutor));

        whenUpdatingStatusTo(EstadoCompra.EN_PREPARACION);

        verify(notificationRepository).save(
                any(com.recred.backend.domain.models.Notification.class),
                eq(tutorUserId)
        );
        verify(pushNotificationSender).send(
                any(com.recred.backend.domain.models.PushNotification.class)
        );
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

    private void givenWalletExistsFor(Alumno student, Billetera wallet) {
        when(walletRepository.findByStudentId(student.getId())).thenReturn(Optional.of(wallet));
    }

    private UpdatePurchaseStatusResponse whenUpdatingStatusTo(EstadoCompra status) {
        return useCase.execute(new UpdatePurchaseStatusRequest(purchaseId, status));
    }
}
