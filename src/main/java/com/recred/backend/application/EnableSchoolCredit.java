package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ActiveCreditExistsException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.WalletRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class EnableSchoolCredit {

    private final TutorRepository tutorRepository;
    private final AlumnoRepository alumnoRepository;
    private final SchoolCreditRepository schoolCreditRepository;
    private final PurchaseRepository purchaseRepository;
    private final WalletRepository walletRepository;
    private final Clock clock;

    public EnableSchoolCredit(
            TutorRepository tutorRepository,
            AlumnoRepository alumnoRepository,
            SchoolCreditRepository schoolCreditRepository,
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            Clock clock) {
        this.tutorRepository = tutorRepository;
        this.alumnoRepository = alumnoRepository;
        this.schoolCreditRepository = schoolCreditRepository;
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
        this.clock = clock;
    }

    @Transactional
    public SchoolCredit execute(
            UUID tutorId, UUID studentId, BigDecimal amount, Integer installments) {
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new IllegalArgumentException("Tutor not found"));
        Alumno alumno = alumnoRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        Billetera wallet = alumno.getBilletera();
        if (wallet == null) {
            wallet = new Billetera();
            wallet.setId(UUID.randomUUID());
            wallet.setAlumno(alumno);
            wallet.setSaldoCreditos(BigDecimal.ZERO);
            wallet.setLimiteCredito(BigDecimal.ZERO);
            wallet.setSaldoDeudaInterno(BigDecimal.ZERO);
            wallet.setCreditoHabilitado(false);
            alumno.setBilletera(wallet);
            alumno = alumnoRepository.save(alumno);
        }

        schoolCreditRepository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE)
                .ifPresent(c -> {
                    throw new ActiveCreditExistsException(
                            "The student already has an active credit."
                    );
                });

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }

        if (installments == null || installments < 1) {
            installments = 1;
        }

        SchoolCredit newCredit = new SchoolCredit();
        newCredit.setStudentId(studentId);
        newCredit.setAmount(amount);
        newCredit.setStatus(CreditStatus.ACTIVE);
        newCredit.setInstallments(installments);
        newCredit.setCreatedAt(LocalDateTime.now(clock));

        SchoolCredit savedCredit = schoolCreditRepository.save(newCredit);
        
        wallet.credit(amount);
        wallet.setCreditoHabilitado(true);
        walletRepository.save(wallet);

        return savedCredit;
    }
}
