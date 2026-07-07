package com.recred.backend.application;

import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.repositories.SchoolCreditRepository;

import java.util.Optional;


public class GetActiveSchoolCredit {

    private final SchoolCreditRepository schoolCreditRepository;

    public GetActiveSchoolCredit(SchoolCreditRepository schoolCreditRepository) {
        this.schoolCreditRepository = schoolCreditRepository;
    }

    public Optional<GetActiveSchoolCreditResponse> execute(GetActiveSchoolCreditRequest request) {
        return schoolCreditRepository.findByStudentIdAndStatus(
                request.studentId(), CreditStatus.ACTIVE)
                .map(this::mapToResponse);
    }

    private GetActiveSchoolCreditResponse mapToResponse(SchoolCredit credit) {
        return new GetActiveSchoolCreditResponse(
                credit.getId(),
                credit.getAmount(),
                credit.getStatus().name(),
                credit.getInstallments()
        );
    }
}
