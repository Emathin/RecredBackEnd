package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class CancelPresentialPurchase {

    private final PurchaseRepository purchaseRepository;
    private final WalletRepository walletRepository;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final FranjaHorariaRepository timeSlotRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public CancelPresentialPurchase(
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            FranjaHorariaRepository timeSlotRepository) {
        this(purchaseRepository, walletRepository, inventoryAvailabilityService,
                timeSlotRepository, null);
    }

    public CancelPresentialPurchase(
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            FranjaHorariaRepository timeSlotRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.timeSlotRepository = timeSlotRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public Purchase execute(CancelPresentialPurchaseRequest request) {
        request.validate();
        Purchase purchase = purchase(request.purchaseId());
        requireStudentOwner(purchase, request.studentId());
        EstadoCompra previousStatus = requirePendingPurchase(purchase);

        validateAdvancePurchaseCancellationWindow(purchase);
        purchase.cancel();
        releaseReservationIfNeeded(purchase, previousStatus);
        processRefund(purchase);

        return purchaseRepository.save(purchase);
    }

    private Purchase purchase(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Purchase not found: " + purchaseId));
    }

    private void requireStudentOwner(Purchase purchase, UUID expectedStudentId) {
        if (expectedStudentId == null) {
            return;
        }

        if (!purchase.belongsToStudent(expectedStudentId)) {
            throw new PurchaseAccessDeniedException(purchase.getId());
        }
    }

    private EstadoCompra requirePendingPurchase(Purchase purchase) {
        EstadoCompra previousStatus = purchase.getStatus();

        if (!purchase.isPending()) {
            throw new InvalidPurchaseStatusTransitionException(
                    previousStatus,
                    EstadoCompra.CANCELADO);
        }

        return previousStatus;
    }

    private void processRefund(Purchase purchase) {
        Billetera wallet = walletRepository.findByStudentId(purchase.requireStudentId())
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found for refund."));

        wallet.credit(purchase.getTotalAmount());
        walletRepository.save(wallet);

        registerRefundMovement(wallet, purchase);
    }

    private void validateAdvancePurchaseCancellationWindow(Purchase purchase) {
        if (!purchase.hasScheduledPickup()) {
            return;
        }

        FranjaHoraria franja = timeSlotRepository.findById(purchase.getPickupSlotId())
                .orElseThrow(() -> new IllegalStateException(
                        "Franja horaria no encontrada para el pedido"));

        ZoneId zoneId = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDateTime nowInZone = LocalDateTime.now(zoneId);
        LocalDateTime startDateTime = LocalDateTime.of(
                purchase.getPickupDate(),
                franja.getHoraInicio());

        if (!nowInZone.isBefore(startDateTime.minusHours(1))) {
            throw new IllegalArgumentException(
                    "No se puede cancelar el pedido faltando una hora o menos "
                            + "para que comience el horario de retiro");
        }
    }

    private void releaseReservationIfNeeded(Purchase purchase, EstadoCompra previousStatus) {
        if (!purchase.shouldReleaseReservationAfterCancellationFrom(previousStatus)) {
            return;
        }

        inventoryAvailabilityService.releaseReservation(
                purchase.getBuffetId(),
                purchase.getItems(),
                purchase.getId());
    }

    private void registerRefundMovement(Billetera wallet, Purchase purchase) {
        if (movimientoBilleteraRepository == null) {
            return;
        }
        movimientoBilleteraRepository.registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.REEMBOLSO,
                purchase.getTotalAmount(),
                "Reembolso por cancelacion de compra");
    }
}
