package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.WalletRepository;

import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class PaySchoolCredit {

    private final SchoolCreditRepository schoolCreditRepository;
    private final WalletRepository walletRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public PaySchoolCredit(
            SchoolCreditRepository schoolCreditRepository,
            WalletRepository walletRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this.schoolCreditRepository = schoolCreditRepository;
        this.walletRepository = walletRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public SchoolCredit execute(UUID creditId) {
        SchoolCredit credit = schoolCreditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit not found"));

        if (credit.getStatus() != CreditStatus.ACTIVE) {
            throw new IllegalStateException("Only active credits can be paid.");
        }

        Billetera wallet = walletRepository.findByStudentId(credit.getStudentId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for student."));

        // Deduct balance and disable credit flag
        wallet.payCredit(credit.getAmount());

        // Update credit status
        credit.setStatus(CreditStatus.PAID);

        // Save changes
        walletRepository.save(wallet);
        schoolCreditRepository.save(credit);

        // Register system movement
        if (movimientoBilleteraRepository != null) {
            movimientoBilleteraRepository.registrarMovimientoSistema(
                    wallet.getId(),
                    TipoMovimientoBilletera.AJUSTE,
                    credit.getAmount().negate(),
                    "Pago de adelanto / microcrédito escolar"
            );
        }

        return credit;
    }
}
