package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaySchoolCredit {
    private final SchoolCreditRepository creditRepository;
    private final AlumnoRepository alumnoRepository;

    public PaySchoolCredit(SchoolCreditRepository creditRepository,
                           AlumnoRepository alumnoRepository) {
        this.creditRepository = creditRepository;
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional
    public SchoolCredit execute(UUID creditId) {
        SchoolCredit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit not found"));
        
        if (credit.getStatus() == CreditStatus.PAID) {
            throw new IllegalStateException("Credit is already paid");
        }

        Alumno alumno = alumnoRepository.findById(credit.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (!alumno.getBilletera().hasSufficientBalance(credit.getAmount())) {
            throw new IllegalStateException("Insufficient balance to pay the credit");
        }

        alumno.getBilletera().debit(credit.getAmount());
        credit.setStatus(CreditStatus.PAID);

        alumnoRepository.save(alumno);
        return creditRepository.save(credit);
    }
}
