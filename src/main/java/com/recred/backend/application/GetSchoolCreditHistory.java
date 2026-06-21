package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetSchoolCreditHistory {
    private final SchoolCreditRepository creditRepository;

    public GetSchoolCreditHistory(SchoolCreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    public List<SchoolCredit> execute(UUID studentId) {
        return creditRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId);
    }
}
