package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    public RevertPurchaseCancellationResponse execute(
            RevertPurchaseCancellationRequest cancellationRequest
    ) {
        Purchase purchase = revertCancellation(cancellationRequest);
        return responseFrom(savePurchase(purchase), purchase);
    }

    private Purchase revertCancellation(RevertPurchaseCancellationRequest cancellationRequest) {
        Purchase purchase = findPurchase(cancellationRequest.purchaseId());
        purchase.revertCancellation();
        debitStudentWallet(purchase);
        return purchase;
    }

    private Purchase findPurchase(UUID purchaseId) {
        ensurePurchaseIdIsPresent(purchaseId);
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Purchase not found: " + purchaseId));
    }

    private void ensurePurchaseIdIsPresent(UUID purchaseId) {
        if (purchaseId == null) {
            throw new IllegalArgumentException("purchaseId cannot be null");
        }
    }

    private void debitStudentWallet(Purchase purchase) {
        Billetera wallet = findStudentWallet(purchase);
        validateWalletHasFunds(wallet, purchase);

        wallet.debit(purchase.getTotalAmount());
        walletRepository.save(wallet);
        registerWalletMovement(wallet, purchase);
    }

    private Billetera findStudentWallet(Purchase purchase) {
        return walletRepository.findByStudentId(purchase.requireStudentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Student wallet not found for debit."));
    }

    private void validateWalletHasFunds(Billetera wallet, Purchase purchase) {
        if (wallet.getSaldoCreditos().compareTo(purchase.getTotalAmount()) < 0) {
            throw new IllegalStateException("Insufficient funds to revert cancellation.");
        }
    }

    private void registerWalletMovement(Billetera wallet, Purchase purchase) {
        if (movimientoBilleteraRepository == null) {
            return;
        }

        movimientoBilleteraRepository.registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.COMPRA,
                purchase.getTotalAmount().negate(),
                "Débito por reversión de cancelación de compra");
    }

    private Purchase savePurchase(Purchase purchase) {
        return purchaseRepository.save(purchase);
    }

    private RevertPurchaseCancellationResponse responseFrom(
            Purchase savedPurchase,
            Purchase purchase
    ) {
        return RevertPurchaseCancellationResponse.fromDomain(
                savedPurchase == null ? purchase : savedPurchase);
    }
}
