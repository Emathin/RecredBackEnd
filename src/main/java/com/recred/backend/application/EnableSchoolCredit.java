package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public class EnableSchoolCredit {

    private final TutorRepository tutorRepository;
    private final AlumnoRepository alumnoRepository;
    private final SchoolCreditRepository schoolCreditRepository;
    private final WalletRepository walletRepository;
    private final Clock clock;

    public EnableSchoolCredit(
            TutorRepository tutorRepository,
            AlumnoRepository alumnoRepository,
            SchoolCreditRepository schoolCreditRepository,
            WalletRepository walletRepository,
            Clock clock) {
        this.tutorRepository = tutorRepository;
        this.alumnoRepository = alumnoRepository;
        this.schoolCreditRepository = schoolCreditRepository;
        this.walletRepository = walletRepository;
        this.clock = clock;
    }

    @Transactional
    public EnableSchoolCreditResponse execute(EnableSchoolCreditRequest enableCreditRequest) {
        Tutor tutor = tutorRepository.findById(enableCreditRequest.tutorId())
                .orElseThrow(() -> new IllegalArgumentException("Tutor not found"));
        if (tutor.getUsuario() == null || tutor.getUsuario().hasFreePlan()) {
            throw new AccessDeniedException(
                    "La funcionalidad de Microcréditos está disponible solo para el plan AVANZADO");
        }

        Alumno student = findStudent(enableCreditRequest.studentId());
        Billetera wallet = getOrCreateWallet(student);

        wallet.enableCredit(enableCreditRequest.amount());
        SchoolCredit savedCredit = createAndSaveCredit(enableCreditRequest);
        walletRepository.save(wallet);

        return responseFrom(savedCredit);
    }

    private void validateTutorExists(UUID tutorId) {
        tutorRepository.findById(tutorId)
                .orElseThrow(() -> new IllegalArgumentException("Tutor not found"));
    }

    private Alumno findStudent(UUID studentId) {
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

    private SchoolCredit createAndSaveCredit(EnableSchoolCreditRequest request) {
        SchoolCredit newCredit = new SchoolCredit();
        newCredit.setStudentId(request.studentId());
        newCredit.setAmount(request.amount());
        newCredit.setStatus(CreditStatus.ACTIVE);
        newCredit.setInstallments(normalizeInstallments(request.installments()));
        newCredit.setCreatedAt(LocalDateTime.now(clock));
        return schoolCreditRepository.save(newCredit);
    }

    private int normalizeInstallments(Integer installments) {
        if (installments == null || installments < 1) {
            return 1;
        }
        return installments;
    }

    private EnableSchoolCreditResponse responseFrom(SchoolCredit savedCredit) {
        return new EnableSchoolCreditResponse(
                savedCredit.getId(),
                savedCredit.getAmount(),
                savedCredit.getStatus().name(),
                savedCredit.getInstallments()
        );
    }
}
