package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
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

    public CancelPresentialPurchase(
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            FranjaHorariaRepository timeSlotRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public void execute(UUID purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Purchase not found: " + purchaseId));

        EstadoCompra previousStatus = purchase.getStatus();

        if (previousStatus != EstadoCompra.PENDIENTE) {
            throw new InvalidPurchaseStatusTransitionException(
                    previousStatus,
                    EstadoCompra.CANCELADO
            );
        }

        validateAdvancePurchaseCancellationWindow(purchase);

        purchase.cancel();

        if (purchase.getStudent() == null || purchase.getStudent().getId() == null) {
            throw new IllegalStateException("Purchase does not have an associated student.");
        }

        Billetera wallet = walletRepository.findByStudentId(purchase.getStudent().getId())
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found for refund."));

        if (shouldReleaseReservation(purchase, previousStatus)) {
            inventoryAvailabilityService.releaseReservation(
                    purchase.getBuffetId(),
                    purchase.getItems(),
                    purchase.getId()
            );
        }

        wallet.credit(purchase.getTotalAmount());

        walletRepository.save(wallet);
        purchaseRepository.save(purchase);
    }

    private void validateAdvancePurchaseCancellationWindow(Purchase purchase) {
        if (!hasScheduledPickup(purchase)) {
            return;
        }

        FranjaHoraria franja = timeSlotRepository.findById(purchase.getPickupSlotId())
                .orElseThrow(() -> new IllegalStateException(
                        "Franja horaria no encontrada para el pedido"));

        ZoneId zoneId = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDateTime nowInZone = LocalDateTime.now(zoneId);
        LocalDateTime startDateTime = LocalDateTime.of(
                purchase.getPickupDate(),
                franja.getHoraInicio()
        );

        if (!nowInZone.isBefore(startDateTime.minusHours(1))) {
            throw new IllegalArgumentException(
                    "No se puede cancelar el pedido faltando una hora o menos "
                            + "para que comience el horario de retiro");
        }
    }

    private boolean hasScheduledPickup(Purchase purchase) {
        return purchase.getTipo() == TipoCompra.ANTICIPADA
                && purchase.getPickupSlotId() != null
                && purchase.getPickupDate() != null;
    }

    private boolean shouldReleaseReservation(Purchase purchase, EstadoCompra previousStatus) {
        return purchase.getTipo() == TipoCompra.ANTICIPADA
                && previousStatus == EstadoCompra.PENDIENTE;
    }
}
