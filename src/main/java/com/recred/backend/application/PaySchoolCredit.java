package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaySchoolCredit {

    private final SchoolCreditRepository creditRepository;
    private final WalletRepository walletRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public PaySchoolCredit(SchoolCreditRepository creditRepository,
                           WalletRepository walletRepository,
                           MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this.creditRepository = creditRepository;
        this.walletRepository = walletRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public SchoolCredit execute(UUID creditId) {
        SchoolCredit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit not found"));

        if (credit.getStatus() == CreditStatus.PAID) {
            throw new IllegalArgumentException("Credit is already paid");
        }

        Billetera wallet = walletRepository.findByStudentId(credit.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        wallet.debit(credit.getAmount());
        walletRepository.save(wallet);

        credit.setStatus(CreditStatus.PAID);
        SchoolCredit savedCredit = creditRepository.save(credit);

        if (movimientoBilleteraRepository != null) {
            movimientoBilleteraRepository.registrarMovimientoSistema(
                    wallet.getId(),
                    TipoMovimientoBilletera.AJUSTE,
                    credit.getAmount().negate(),
                    "Pago de adelanto"
            );
        }

        return savedCredit;
    }
}
