package com.recred.backend.application;

import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;
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
    private PresupuestoRepository repository;

    @InjectMocks
    private GetActiveBudgetByStudent getActiveBudgetByStudent;

    @Test
    void execute_DelegaLaConsultaAlRepositorio() {
        UUID studentId = UUID.randomUUID();
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();

        when(repository.findActivoByAlumnoId(studentId)).thenReturn(Optional.of(presupuesto));

        Optional<PresupuestoAlumno> resultado = getActiveBudgetByStudent.execute(studentId);

        assertTrue(resultado.isPresent());
        assertEquals(presupuesto, resultado.get());
        verify(repository).findActivoByAlumnoId(studentId);
    }
}