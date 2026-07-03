package com.recred.backend.application;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.NotificationRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class UpdatePurchaseStatus {

    private final PurchaseRepository purchaseRepository;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final WalletRepository walletRepository;
    private final KiosqueroRealtimeEventPublisher realtimeEventPublisher;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;
    private final TutorRepository tutorRepository;
    private final NotificationRepository notificationRepository;
    private final PushNotificationSender pushNotificationSender;

    public UpdatePurchaseStatus(
            PurchaseRepository purchaseRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            WalletRepository walletRepository) {
        this(purchaseRepository, inventoryAvailabilityService, walletRepository, event -> {
        });
    }

    public UpdatePurchaseStatus(
            PurchaseRepository purchaseRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            WalletRepository walletRepository,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher) {
        this(purchaseRepository, inventoryAvailabilityService, walletRepository,
                realtimeEventPublisher, null);
    }

    public UpdatePurchaseStatus(
            PurchaseRepository purchaseRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            WalletRepository walletRepository,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this(purchaseRepository, inventoryAvailabilityService, walletRepository,
                realtimeEventPublisher, movimientoBilleteraRepository, null, null, null);
    }

    public UpdatePurchaseStatus(
            PurchaseRepository purchaseRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            WalletRepository walletRepository,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher,
            MovimientoBilleteraRepository movimientoBilleteraRepository,
            TutorRepository tutorRepository,
            NotificationRepository notificationRepository,
            PushNotificationSender pushNotificationSender) {
        this.purchaseRepository = purchaseRepository;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.walletRepository = walletRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
        this.tutorRepository = tutorRepository;
        this.notificationRepository = notificationRepository;
        this.pushNotificationSender = pushNotificationSender;
    }

    @Transactional
    public UpdatePurchaseStatusResponse execute(UpdatePurchaseStatusRequest request) {
        requireRequest(request);
        Purchase purchase = findPurchase(request.purchaseId());
        EstadoCompra previousStatus = purchase.getStatus();
        purchase.changeTrackingStatusTo(request.requestedStatus());
        handleStatusChangeSideEffects(purchase, previousStatus, request.requestedStatus());
        Purchase savedPurchase = purchaseRepository.save(purchase);
        publishOperationalEvent(savedPurchase, request.requestedStatus());
        notifyTutorsOnStatusChange(purchase, previousStatus, request.requestedStatus());
        return response(savedPurchase);
    }

    private void requireRequest(UpdatePurchaseStatusRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
    }

    private UpdatePurchaseStatusResponse response(Purchase savedPurchase) {
        return new UpdatePurchaseStatusResponse(
                savedPurchase.getId(),
                savedPurchase.studentIdOrNull(),
                savedPurchase.statusName(),
                savedPurchase.statusLabel(),
                savedPurchase.getDate());
    }

    private Purchase findPurchase(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
    }

    private void handleStatusChangeSideEffects(
            Purchase purchase,
            EstadoCompra previousStatus,
            EstadoCompra requestedStatus) {
        handleInventoryUpdates(purchase, previousStatus, requestedStatus);
        handleRefundIfNecessary(purchase, requestedStatus);
    }

    private void handleInventoryUpdates(
            Purchase purchase,
            EstadoCompra previousStatus,
            EstadoCompra requestedStatus) {
        if (purchase.shouldConsumeReservationWhenChangingTo(requestedStatus)) {
            inventoryAvailabilityService.consumeReservation(
                    purchase.getBuffetId(),
                    purchase.getItems(),
                    purchase.getId());
        }

        if (purchase.shouldReleaseReservationWhenChangingFrom(previousStatus, requestedStatus)) {
            inventoryAvailabilityService.releaseReservation(
                    purchase.getBuffetId(),
                    purchase.getItems(),
                    purchase.getId());
        }
    }

    private void handleRefundIfNecessary(Purchase purchase, EstadoCompra requestedStatus) {
        if (purchase.shouldRefundWhenChangingTo(requestedStatus)) {
            refund(purchase);
        }
    }

    private void publishOperationalEvent(Purchase purchase, EstadoCompra requestedStatus) {
        if (!purchase.shouldPublishExpirationEventFor(requestedStatus)) {
            return;
        }

        realtimeEventPublisher.publish(KiosqueroRealtimeEvent.purchase(
                KiosqueroRealtimeEventType.ORDER_EXPIRED,
                purchase,
                "Pedido vencido o no retirado"));
    }

    private void refund(Purchase purchase) {
        Billetera wallet = walletRepository.findByStudentId(purchase.requireStudentId())
                .orElseThrow(() -> new IllegalStateException("Student wallet not found for refund."));
        wallet.credit(purchase.getTotalAmount());
        walletRepository.save(wallet);
        registerRefundMovement(wallet, purchase);
    }

    private void registerRefundMovement(Billetera wallet, Purchase purchase) {
        if (movimientoBilleteraRepository == null) {
            return;
        }
        movimientoBilleteraRepository.registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.REEMBOLSO,
                purchase.getTotalAmount(),
                "Reembolso por rechazo de compra");
    }

    private void notifyTutorsOnStatusChange(
            Purchase purchase,
            EstadoCompra previousStatus,
            EstadoCompra requestedStatus) {
        if (tutorRepository == null
                || notificationRepository == null
                || pushNotificationSender == null) {
            return;
        }
        if (previousStatus == requestedStatus) {
            return;
        }

        String title;
        String message;
        String studentName = purchase.getStudent() != null
                ? purchase.getStudent().getNombre()
                : "El alumno";

        switch (requestedStatus) {
            case EN_PREPARACION -> {
                title = "Pedido en preparación";
                message = "El pedido de " + studentName + " está en preparación.";
            }
            case LISTO -> {
                title = "Pedido listo para retirar";
                message = "El pedido de " + studentName + " está listo para retirar.";
            }
            case ENTREGADO -> {
                title = "Pedido entregado";
                message = "El pedido de " + studentName + " fue entregado.";
            }
            case CANCELADO -> {
                title = "Pedido cancelado";
                message = "El pedido de " + studentName + " fue cancelado.";
            }
            case RECHAZADO -> {
                title = "Pedido rechazado";
                message = "El pedido de " + studentName + " fue rechazado.";
            }
            case VENCIDO -> {
                title = "Pedido vencido";
                message = "El pedido de " + studentName + " ha vencido.";
            }
            case NO_RETIRADO -> {
                title = "Pedido no retirado";
                message = "El pedido de " + studentName + " no fue retirado.";
            }
            default -> {
                return;
            }
        }

        UUID studentId = purchase.studentIdOrNull();
        if (studentId == null) {
            return;
        }

        java.util.List<com.recred.backend.domain.models.Tutor> tutors = tutorRepository.findByAlumnoId(studentId);
        if (tutors == null || tutors.isEmpty()) {
            return;
        }

        for (com.recred.backend.domain.models.Tutor tutor : tutors) {
            if (tutor.getUsuario() == null) {
                continue;
            }
            UUID tutorUserId = tutor.getUsuario().getId();

            com.recred.backend.domain.models.Notification notification = new com.recred.backend.domain.models.Notification(
                    null,
                    com.recred.backend.domain.models.TipoNotificacion.ESTADO_COMPRA,
                    title,
                    message,
                    false,
                    java.time.LocalDateTime.now(),
                    null,
                    studentId,
                    null,
                    purchase.getId(),
                    null);
            notificationRepository.save(notification, tutorUserId);

            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("alumnoId", studentId.toString());
            data.put("compraId", purchase.getId().toString());

            com.recred.backend.domain.models.PushNotification pushNotification = new com.recred.backend.domain.models.PushNotification(
                    tutorUserId,
                    title,
                    message,
                    data);
            pushNotificationSender.send(pushNotification);
        }
    }
}
