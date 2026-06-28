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
    public EnableSchoolCreditResponse execute(EnableSchoolCreditRequest request) {
        request.validate();

        Tutor tutor = validateParentAndGetTutor(request.tutorId());
        Alumno alumno = getHijo(request.studentId());

        Billetera wallet = getOrCreateWallet(alumno);

        ensureNoActiveCredits(alumno.getId());

        int installments = request.installments() == null || request.installments() < 1 
                ? 1 
                : request.installments();

        SchoolCredit savedCredit = createAndSaveCredit(
                alumno.getId(), request.amount(), installments);

        updateWalletWithCredit(wallet, request.amount());

        return new EnableSchoolCreditResponse(
                savedCredit.getId(),
                savedCredit.getAmount(),
                savedCredit.getStatus().name(),
                savedCredit.getInstallments()
        );
    }

    private Tutor validateParentAndGetTutor(UUID tutorId) {
        return tutorRepository.findById(tutorId)
                .orElseThrow(() -> new IllegalArgumentException("Tutor not found"));
    }

    private Alumno getHijo(UUID studentId) {
        return alumnoRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
    }

    private Billetera getOrCreateWallet(Alumno alumno) {
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
            alumnoRepository.save(alumno);
        }
        return wallet;
    }

    private void ensureNoActiveCredits(UUID studentId) {
        schoolCreditRepository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE)
                .ifPresent(c -> {
                    throw new ActiveCreditExistsException(
                            "The student already has an active credit."
                    );
                });
    }

    private SchoolCredit createAndSaveCredit(UUID studentId, BigDecimal amount, int installments) {
        SchoolCredit newCredit = new SchoolCredit();
        newCredit.setStudentId(studentId);
        newCredit.setAmount(amount);
        newCredit.setStatus(CreditStatus.ACTIVE);
        newCredit.setInstallments(installments);
        newCredit.setCreatedAt(LocalDateTime.now(clock));
        return schoolCreditRepository.save(newCredit);
    }

    private void updateWalletWithCredit(Billetera wallet, BigDecimal amount) {
        wallet.credit(amount);
        wallet.setCreditoHabilitado(true);
        walletRepository.save(wallet);
    }
}
