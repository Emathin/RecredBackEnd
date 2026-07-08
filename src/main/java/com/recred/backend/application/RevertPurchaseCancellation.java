package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.springframework.transaction.annotation.Transactional;

public class RevertPurchaseCancellation {
    private final PurchaseRepository purchaseRepository;
    private final WalletRepository walletRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public RevertPurchaseCancellation(
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public RevertPurchaseCancellationResponse execute(RevertPurchaseCancellationRequest request) {
        request.validate();
        
        Purchase purchase = purchaseRepository.findById(request.purchaseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Purchase not found: " + request.purchaseId()));

        purchase.revertCancellation();

        Billetera wallet = walletRepository.findByStudentId(purchase.requireStudentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Student wallet not found for debit."));

        if (wallet.getSaldoCreditos().compareTo(purchase.getTotalAmount()) < 0) {
            throw new IllegalStateException("Insufficient funds to revert cancellation.");
        }

        wallet.debit(purchase.getTotalAmount());
        walletRepository.save(wallet);

        if (movimientoBilleteraRepository != null) {
            movimientoBilleteraRepository.registrarMovimientoSistema(
                    wallet.getId(),
                    TipoMovimientoBilletera.COMPRA,
                    purchase.getTotalAmount().negate(),
                    "Débito por reversión de cancelación de compra");
        }

        Purchase savedPurchase = purchaseRepository.save(purchase);
        return RevertPurchaseCancellationResponse.fromDomain(
                savedPurchase == null ? purchase : savedPurchase);
    }
}
