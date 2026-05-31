package com.recred.backend.application;

import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.repositories.BudgetRepository;

import java.util.Optional;
import java.util.UUID;

public class GetActiveBudgetByStudent {

    private final BudgetRepository repository;

    public GetActiveBudgetByStudent(BudgetRepository repository) {
        this.repository = repository;
    }

    public Optional<StudentBudget> execute(UUID studentId) {
        return repository.findByActiveStudentID(studentId);
    }
}