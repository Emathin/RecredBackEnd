package com.recred.backend.application;

import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.repositories.BudgetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActiveBudgetByStudentTest {

    @Mock
    private BudgetRepository repository;

    @InjectMocks
    private GetActiveBudgetByStudent getActiveBudgetByStudent;

    @Test
    void execute_DelegaLaConsultaAlRepositorio() {
        UUID studentId = UUID.randomUUID();
        StudentBudget presupuesto = new StudentBudget();

        when(repository.findByActiveStudentID(studentId)).thenReturn(Optional.of(presupuesto));

        Optional<StudentBudget> resultado = getActiveBudgetByStudent.execute(studentId);

        assertTrue(resultado.isPresent());
        assertEquals(presupuesto, resultado.get());
        verify(repository).findByActiveStudentID(studentId);
    }
}