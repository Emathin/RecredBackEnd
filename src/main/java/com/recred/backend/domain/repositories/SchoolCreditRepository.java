package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolCreditRepository {
    Optional<SchoolCredit> findById(UUID id);
    Optional<SchoolCredit> findByStudentIdAndStatus(UUID studentId, CreditStatus status);
    List<SchoolCredit> findAllByStudentIdOrderByCreatedAtDesc(UUID studentId);
    SchoolCredit save(SchoolCredit schoolCredit);
}
