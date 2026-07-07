package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public class ConfirmPaymentAndCredit {

    private static final String PAYMENT_CREDIT_DESCRIPTION =
            "Acreditacion de pago Mercado Pago: ";

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
    public ConfirmPaymentAndCreditResponse execute(ConfirmPaymentAndCreditRequest paymentRequest) {
        PaymentDetails paymentDetails = findPaymentDetails(paymentRequest);
        creditWalletIfPaymentWasApproved(paymentDetails);

        return new ConfirmPaymentAndCreditResponse();
    }

    private PaymentDetails findPaymentDetails(ConfirmPaymentAndCreditRequest paymentRequest) {
        return externalPaymentService.getPaymentDetails(paymentRequest.paymentId());
    }

    private void creditWalletIfPaymentWasApproved(PaymentDetails paymentDetails) {
        if (!paymentDetails.wasApproved()) {
            return;
        }

        Billetera wallet = findStudentWallet(paymentDetails);
        creditWallet(wallet, paymentDetails.amount());
        registerPaymentCreditMovement(wallet, paymentDetails);
    }

    private Billetera findStudentWallet(PaymentDetails paymentDetails) {
        return walletRepository.findByStudentId(paymentDetails.studentId())
                .orElseThrow(() -> new RuntimeException(
                        "Billetera no encontrada para alumno: "
                                + paymentDetails.studentId()));
    }

    private void creditWallet(Billetera wallet, BigDecimal amount) {
        wallet.credit(amount);
        walletRepository.save(wallet);
    }

    private void registerPaymentCreditMovement(
            Billetera wallet,
            PaymentDetails paymentDetails) {
        registerSystemMovement(
                wallet,
                TipoMovimientoBilletera.CARGA,
                paymentDetails.amount(),
                PAYMENT_CREDIT_DESCRIPTION + paymentDetails.paymentId());
    }

    private void registerSystemMovement(
            Billetera wallet,
            TipoMovimientoBilletera type,
            BigDecimal amount,
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
