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
    public void execute(String paymentId) {

        PaymentDetails details = externalPaymentService.getPaymentDetails(paymentId);

        if (details.status() == PaymentStatus.APPROVED) {
            Billetera wallet = walletRepository.findByStudentId(details.studentId())
                    .orElseThrow(() -> new RuntimeException(
                            "Billetera no encontrada para alumno: " + details.studentId()));

            wallet.credit(details.amount());

            walletRepository.save(wallet);
            registerSystemMovement(
                    wallet,
                    TipoMovimientoBilletera.CARGA,
                    details.amount(),
                    "Acreditacion de pago Mercado Pago: " + details.paymentId());
        }
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
