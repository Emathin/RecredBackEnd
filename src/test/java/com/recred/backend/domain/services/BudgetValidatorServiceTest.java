package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.ExceededBudgetException;
import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.models.PeriodBudget;
import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.models.RangoFechas;
import com.recred.backend.domain.models.BudgetCategoryRule;
import com.recred.backend.domain.repositories.BudgetRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetValidatorServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private PurchaseRepository compraRepository;

    @Mock
    private BudgetPeriodCalculator calculadorPeriodo;

    @InjectMocks
    private BudgetValidatorService validador;

    private UUID alumnoId;
    private LocalDateTime momentoCompra;
    private RangoFechas rangoSimulado;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        momentoCompra = LocalDateTime.now();
        rangoSimulado = new RangoFechas(momentoCompra.minusHours(1), momentoCompra.plusHours(1));
    }

    @Test
    void validateBudgetActivo_NoHaceNada() {
        when(budgetRepository.findByActiveStudentID(alumnoId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validador.validateBudget(
                alumnoId, BigDecimal.valueOf(1000), Collections.emptyMap(), momentoCompra
        ));

        verifyNoInteractions(calculadorPeriodo, compraRepository);
    }

    @Test
    void validateBudgetNoVigenteAun_NoHaceNada() {
        StudentBudget presupuesto = new StudentBudget();

        presupuesto.setStartDate(momentoCompra.toLocalDate().plusDays(1));

        when(budgetRepository.findByActiveStudentID(alumnoId)).thenReturn(Optional.of(presupuesto));

        assertDoesNotThrow(() -> validador.validateBudget(
                alumnoId, BigDecimal.valueOf(1000), Collections.emptyMap(), momentoCompra
        ));

        verifyNoInteractions(calculadorPeriodo, compraRepository);
    }

    @Test
    void validateBudget_SuperaLimiteGeneral_LanzaExcepcion() {
        StudentBudget presupuesto = crearPresupuestoBase(BigDecimal.valueOf(2000));
        when(budgetRepository.findByActiveStudentID(alumnoId)).thenReturn(Optional.of(presupuesto));
        when(calculadorPeriodo.calculateRange(any(), any())).thenReturn(rangoSimulado);

        when(compraRepository.sumExpensesByStudentInRange(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.valueOf(1500));

        ExceededBudgetException ex = assertThrows(ExceededBudgetException.class, () -> {
            validador.validateBudget(alumnoId, BigDecimal.valueOf(600), Collections.emptyMap(), momentoCompra);
        });

        assertTrue(ex.getMessage().contains("Presupuesto diario excedido"));
    }

    @Test
    void validateBudget_SuperaLimiteDeCategoria_LanzaExcepcion() {

        StudentBudget presupuesto = crearPresupuestoBase(BigDecimal.valueOf(5000));

        UUID golosinasId = UUID.randomUUID();
        ProductCategory golosinas = new ProductCategory();
        golosinas.setId(golosinasId);

        BudgetCategoryRule reglaGolosinas = new BudgetCategoryRule(
                UUID.randomUUID(), golosinas, BigDecimal.valueOf(20), BigDecimal.valueOf(1000), true
        );
        presupuesto.setCategoryRules(List.of(reglaGolosinas));

        when(budgetRepository.findByActiveStudentID(alumnoId)).thenReturn(Optional.of(presupuesto));
        when(calculadorPeriodo.calculateRange(any(), any())).thenReturn(rangoSimulado);

        when(compraRepository.sumExpensesByStudentInRange(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(compraRepository.sumExpensesByCategoryInRange(eq(alumnoId), eq(golosinasId), any(), any()))
                .thenReturn(BigDecimal.valueOf(800));

        Map<UUID, BigDecimal> gastosPorCategoria = Map.of(golosinasId, BigDecimal.valueOf(300));

        ExceededBudgetException ex = assertThrows(ExceededBudgetException.class, () -> {
            validador.validateBudget(alumnoId, BigDecimal.valueOf(500), gastosPorCategoria, momentoCompra);
        });

        assertTrue(ex.getMessage().contains("Límite superado para una categoría"));
    }

    @Test
    void validateBudget_TodoDentroDeLosLimites_NoLanzaExcepcion() {
        StudentBudget presupuesto = crearPresupuestoBase(BigDecimal.valueOf(2000));
        when(budgetRepository.findByActiveStudentID(alumnoId)).thenReturn(Optional.of(presupuesto));
        when(calculadorPeriodo.calculateRange(any(), any())).thenReturn(rangoSimulado);

        when(compraRepository.sumExpensesByStudentInRange(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.valueOf(1000));

        assertDoesNotThrow(() -> {
            validador.validateBudget(alumnoId, BigDecimal.valueOf(500), Collections.emptyMap(), momentoCompra);
        });
    }

    private StudentBudget crearPresupuestoBase(BigDecimal limiteGeneral) {
        StudentBudget p = new StudentBudget();
        p.setId(UUID.randomUUID());
        p.setActive(true);
        p.setStartDate(LocalDate.now().minusDays(1));
        p.setPeriod(PeriodBudget.DIARIO);
        p.setLimitAmount(limiteGeneral);
        return p;
    }
}