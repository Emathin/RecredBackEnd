package com.recred.backend.application;

import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.repositories.SchoolCreditRepository;

import java.util.Optional;
import java.util.UUID;

public class GetActiveSchoolCredit {

    private final SchoolCreditRepository schoolCreditRepository;

    public GetActiveSchoolCredit(SchoolCreditRepository schoolCreditRepository) {
        this.schoolCreditRepository = schoolCreditRepository;
    }

    public Optional<SchoolCredit> execute(UUID studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("El ID del alumno no puede ser nulo");
        }
        return schoolCreditRepository.findByStudentIdAndStatus(studentId, CreditStatus.ACTIVE);
    }
}
