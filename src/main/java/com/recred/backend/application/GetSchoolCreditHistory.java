package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import java.util.List;
import java.util.UUID;

public class GetSchoolCreditHistory {

    private final SchoolCreditRepository schoolCreditRepository;

    public GetSchoolCreditHistory(SchoolCreditRepository schoolCreditRepository) {
        this.schoolCreditRepository = schoolCreditRepository;
    }

    public List<SchoolCredit> execute(UUID studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null.");
        }
        return schoolCreditRepository.findByStudentId(studentId);
    }
}
