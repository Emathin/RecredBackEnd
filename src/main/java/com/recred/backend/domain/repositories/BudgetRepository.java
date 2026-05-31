package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.StudentBudget;

import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {
    StudentBudget save(StudentBudget presupuesto);
    Optional<StudentBudget> findByActiveStudentID(UUID alumnoId);
    Optional<StudentBudget> findById(UUID budgetId);
}
