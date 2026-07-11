package com.recred.backend.application;

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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class CancelPresentialPurchase {

    private static final ZoneId CANCELLATION_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String REFUND_MOVEMENT_DESCRIPTION =
            "Reembolso por cancelacion de compra";

    private final PurchaseRepository purchaseRepository;
    private final WalletRepository walletRepository;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final FranjaHorariaRepository timeSlotRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;
    private final Clock clock;

    public CancelPresentialPurchase(
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            FranjaHorariaRepository timeSlotRepository) {
        this(purchaseRepository, walletRepository, inventoryAvailabilityService,
                timeSlotRepository, null, Clock.system(CANCELLATION_ZONE));
    }

    public CancelPresentialPurchase(
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            FranjaHorariaRepository timeSlotRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this(purchaseRepository, walletRepository, inventoryAvailabilityService,
                timeSlotRepository, movimientoBilleteraRepository,
                Clock.system(CANCELLATION_ZONE));
    }

    public CancelPresentialPurchase(
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            FranjaHorariaRepository timeSlotRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository,
            Clock clock) {
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.timeSlotRepository = timeSlotRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
        this.clock = clock;
    }

    @Transactional
    public CancelPresentialPurchaseResponse execute(
            CancelPresentialPurchaseRequest cancellationRequest) {
        Purchase purchase = findPurchase(cancellationRequest.purchaseId());
        ensureStudentCanCancelPurchase(purchase, cancellationRequest.studentId());

        EstadoCompra statusBeforeCancellation = purchase.getStatus();
        ensurePurchaseCanBeCancelled(purchase);
        cancelPurchaseAndReleaseReservation(
                purchase, statusBeforeCancellation, cancellationRequest);
        refundStudentWallet(purchase);

        Purchase savedPurchase = savePurchase(purchase);
        return CancelPresentialPurchaseResponse.fromDomain(savedPurchase);
    }

    private Purchase savePurchase(Purchase purchase) {
        Purchase savedPurchase = purchaseRepository.save(purchase);
        return savedPurchase == null ? purchase : savedPurchase;
    }

    private Purchase findPurchase(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Purchase not found: " + purchaseId));
    }

    private void ensureStudentCanCancelPurchase(Purchase purchase, UUID expectedStudentId) {
        if (expectedStudentId == null) {
            return;
        }

        if (!purchase.belongsToStudent(expectedStudentId)) {
            throw new PurchaseAccessDeniedException(purchase.getId());
        }
    }

    private void ensurePurchaseCanBeCancelled(Purchase purchase) {
        validateAdvancePurchaseCancellationWindow(purchase);
    }

    private void cancelPurchaseAndReleaseReservation(
            Purchase purchase,
            EstadoCompra statusBeforeCancellation,
            CancelPresentialPurchaseRequest cancellationRequest) {
        purchase.cancel(cancellationRequest.cancelledById(), cancellationRequest.cancelledByRole());
        releaseReservationIfNeeded(purchase, statusBeforeCancellation);
    }

    private void refundStudentWallet(Purchase purchase) {
        Billetera wallet = walletRepository.findByStudentId(purchase.requireStudentId())
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found for refund."));

        creditWallet(wallet, purchase);
        registerRefundMovement(wallet, purchase);
    }

    private void validateAdvancePurchaseCancellationWindow(Purchase purchase) {
        if (!purchase.hasScheduledPickup()) {
            return;
        }

        FranjaHoraria pickupSlot = findPickupSlot(purchase.getPickupSlotId());
        purchase.validateCancellationWindow(pickupSlot, now());
    }

    private FranjaHoraria findPickupSlot(UUID pickupSlotId) {
        return timeSlotRepository.findById(pickupSlotId)
                .orElseThrow(() -> new IllegalStateException(
                        "Time slot not found for purchase"));
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

    private void creditWallet(Billetera wallet, Purchase purchase) {
        wallet.credit(purchase.getTotalAmount());
        walletRepository.save(wallet);
    }

    private void registerRefundMovement(Billetera wallet, Purchase purchase) {
        if (movimientoBilleteraRepository == null) {
            return;
        }

        movimientoBilleteraRepository.registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.REEMBOLSO,
                purchase.getTotalAmount(),
                REFUND_MOVEMENT_DESCRIPTION);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
