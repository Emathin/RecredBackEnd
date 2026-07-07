package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.springframework.transaction.annotation.Transactional;

public class ConfirmPaymentAndCredit {

    private final WalletRepository walletRepository;
    private final ExternalPaymentService externalPaymentService;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public ConfirmPaymentAndCredit(
            WalletRepository walletRepository,
            ExternalPaymentService externalPaymentService) {
        this(walletRepository, externalPaymentService, null);
    }

    public ConfirmPaymentAndCredit(
            WalletRepository walletRepository,
            ExternalPaymentService externalPaymentService,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this.walletRepository = walletRepository;
        this.externalPaymentService = externalPaymentService;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public ConfirmPaymentAndCreditResponse execute(ConfirmPaymentAndCreditRequest request) {
        PaymentDetails details = paymentDetails(request);
        creditWalletWhenPaymentIsApprovedTopUp(details);
        return new ConfirmPaymentAndCreditResponse();
    }

    private void creditWalletWhenPaymentIsApprovedTopUp(PaymentDetails details) {
        if (!isApprovedTopUp(details)) {
            return;
        }

        Billetera wallet = wallet(details);
        creditWallet(wallet, details);
        registerTopUpMovement(wallet, details);
    }

    private boolean isApprovedTopUp(PaymentDetails details) {
        return details.isTopUp() && details.status() == PaymentStatus.APPROVED;
    }

    private Billetera wallet(PaymentDetails details) {
        return walletRepository.findByStudentId(details.studentId())
                .orElseThrow(() -> new RuntimeException(
                        "Billetera no encontrada para alumno: " + details.studentId()));
    }

    private void creditWallet(Billetera wallet, PaymentDetails details) {
        wallet.credit(details.amount());
        walletRepository.save(wallet);
    }

    private void registerTopUpMovement(Billetera wallet, PaymentDetails details) {
        registerSystemMovement(
                wallet,
                TipoMovimientoBilletera.CARGA,
                details.amount(),
                "Acreditacion de pago Mercado Pago: " + details.paymentId());
    }

    private PaymentDetails paymentDetails(ConfirmPaymentAndCreditRequest request) {
        if (request.paymentDetails() != null) {
            return request.paymentDetails();
        }

        return externalPaymentService.getPaymentDetails(request.paymentId());
    }

    private void registerSystemMovement(
            Billetera wallet,
            TipoMovimientoBilletera type,
            java.math.BigDecimal amount,
            String description) {
        if (movimientoBilleteraRepository == null) {
            return;
        }
        movimientoBilleteraRepository.registrarMovimientoSistema(
                wallet.getId(),
                type,
                amount,
                description);
    }
}
