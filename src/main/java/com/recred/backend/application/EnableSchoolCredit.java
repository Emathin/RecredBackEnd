package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ActiveCreditExistsException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.WalletRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

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

    public SchoolCredit execute(UUID tutorId, UUID studentId) {
        tutorRepository.findById(tutorId)
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

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime startOfLastMonth = now.minusMonths(1)
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfLastMonth = startOfLastMonth.plusMonths(1).minusNanos(1);

        BigDecimal lastMonthExpense = purchaseRepository
                .sumarGastosPorAlumnoEnRango(studentId, startOfLastMonth, endOfLastMonth);

        BigDecimal calculatedAmount;
        if (lastMonthExpense == null || lastMonthExpense.compareTo(BigDecimal.ZERO) <= 0) {
            calculatedAmount = BigDecimal.valueOf(50.0).multiply(BigDecimal.valueOf(1.10));
        } else {
            calculatedAmount = lastMonthExpense.multiply(BigDecimal.valueOf(1.10));
        }

        SchoolCredit credit = new SchoolCredit();
        credit.setStudentId(studentId);
        credit.setAmount(calculatedAmount);
        credit.setStatus(CreditStatus.ACTIVE);
        credit.setCreatedAt(now);

        SchoolCredit savedCredit = schoolCreditRepository.save(credit);
        
        wallet.credit(calculatedAmount);
        wallet.setCreditoHabilitado(true);
        walletRepository.save(wallet);

        return savedCredit;
    }
}
