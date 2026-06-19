package com.recred.backend.unit.application;

import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.PurchaseSpending;
import com.recred.backend.domain.models.SpendingPeriodPreference;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictStudentSpendingTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-21T12:00:00Z"),
            ZoneId.of("UTC")
    );

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private SpendingPreferenceRepository spendingPreferenceRepository;

    @Mock
    private PurchaseSpendingRepository purchaseSpendingRepository;

    @Mock
    private SpendingPredictionAnalyzer spendingPredictionAnalyzer;

    @Mock
    private AIEventRepository aiEventRepository;

    private PredictStudentSpending predictStudentSpending;

    @BeforeEach
    void setUp() {
        predictStudentSpending = new PredictStudentSpending(
                alumnoRepository,
                spendingPreferenceRepository,
                purchaseSpendingRepository,
                spendingPredictionAnalyzer,
                aiEventRepository,
                new SpendingPredictionCalculator(),
                CLOCK
        );
    }

    @Test
    void useActivePreferenceWhenPeriodIsNotRequested() {
        UUID studentId = UUID.randomUUID();
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(spendingPreferenceRepository.findActivePreferences(
                studentId,
                LocalDate.of(2026, 5, 21)
        )).thenReturn(List.of(new SpendingPeriodPreference(
                BudgetPeriod.QUINCENAL,
                new BigDecimal("200.00")
        )));
        when(purchaseSpendingRepository.findSpending(any(), anyCollection(), any(), any()))
                .thenReturn(
                        List.of(spending("2026-05-21T10:00:00", "50.00")),
                        List.of(spending("2026-04-10T10:00:00", "100.00"))
                );
        when(purchaseSpendingRepository.findMostConsumedCategories(
                any(),
                anyCollection(),
                any(),
                any()
        ))
                .thenReturn(List.of(
                        category("Bebidas", "20.00"),
                        category("Snacks", "15.00"),
                        category("Frutas", "10.00"),
                        category("Panificados", "8.00"),
                        category("Golosinas", "5.00"),
                        category("Otros", "1.00")
                ));
        when(spendingPredictionAnalyzer.analyze(any())).thenReturn(Optional.of(analysis()));

        SpendingPredictionResult response = predictStudentSpending.execute(studentId, null);

        assertEquals(BudgetPeriod.QUINCENAL, response.spendingPrediction().period());
        assertEquals(new BigDecimal("200.00"), response.spendingPrediction().limitAmount());
        assertEquals(5, response.spendingPrediction().mostConsumedCategories().size());
        assertEquals("Reducir snacks.", response.aiAnalysis().orElseThrow().recommendations()
                .get(0));
        verify(spendingPreferenceRepository).findActivePreferences(
                studentId,
                LocalDate.of(2026, 5, 21)
        );
        ArgumentCaptor<SpendingPredictionResult> captor = ArgumentCaptor.forClass(
                SpendingPredictionResult.class);
        verify(aiEventRepository).registerPrediction(captor.capture());
        assertEquals(BudgetPeriod.QUINCENAL, captor.getValue().spendingPrediction().period());
    }

    @Test
    void requestedPeriodOverridesPreference() {
        UUID studentId = UUID.randomUUID();
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(spendingPreferenceRepository.findActivePreferences(any(), any()))
                .thenReturn(List.of(new SpendingPeriodPreference(
                        BudgetPeriod.QUINCENAL,
                        new BigDecimal("200.00")
                )));
        when(purchaseSpendingRepository.findSpending(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(), List.of());
        when(purchaseSpendingRepository.findMostConsumedCategories(
                any(),
                anyCollection(),
                any(),
                any()
        ))
                .thenReturn(List.of());
        when(spendingPredictionAnalyzer.analyze(any())).thenReturn(Optional.empty());

        SpendingPredictionResult response = predictStudentSpending.execute(
                studentId,
                BudgetPeriod.MENSUAL
        );

        assertEquals(BudgetPeriod.MENSUAL, response.spendingPrediction().period());
        verify(aiEventRepository).registerPrediction(any(SpendingPredictionResult.class));
    }

    @Test
    void resolveStudentIdFromUserIdWhenStudentIdDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(alumnoRepository.existsById(userId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(spendingPreferenceRepository.findActivePreferences(any(), any()))
                .thenReturn(List.of());
        when(purchaseSpendingRepository.findSpending(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(), List.of());
        when(purchaseSpendingRepository.findMostConsumedCategories(
                any(),
                anyCollection(),
                any(),
                any()
        ))
                .thenReturn(List.of());
        when(spendingPredictionAnalyzer.analyze(any())).thenReturn(Optional.empty());

        SpendingPredictionResult response = predictStudentSpending.execute(
                userId,
                BudgetPeriod.MENSUAL
        );

        assertEquals(studentId, response.spendingPrediction().studentId());
        verify(spendingPreferenceRepository).findActivePreferences(
                studentId,
                LocalDate.of(2026, 5, 21)
        );
        verify(purchaseSpendingRepository).findSpending(
                eq(studentId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-05-01T00:00:00")),
                eq(LocalDateTime.parse("2026-05-21T23:59:59.999999999"))
        );
    }

    @Test
    void useMonthlyPeriodWhenNoPreferenceAndNoRequestedPeriod() {
        UUID studentId = UUID.randomUUID();
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(spendingPreferenceRepository.findActivePreferences(any(), any()))
                .thenReturn(List.of());
        when(purchaseSpendingRepository.findSpending(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(), List.of());
        when(purchaseSpendingRepository.findMostConsumedCategories(
                any(),
                anyCollection(),
                any(),
                any()
        ))
                .thenReturn(List.of());
        when(spendingPredictionAnalyzer.analyze(any())).thenReturn(Optional.empty());

        SpendingPredictionResult response = predictStudentSpending.execute(studentId, null);

        assertEquals(BudgetPeriod.MENSUAL, response.spendingPrediction().period());
    }

    @Test
    void throwExceptionWhenStudentDoesNotExist() {
        UUID studentId = UUID.randomUUID();
        when(alumnoRepository.existsById(studentId)).thenReturn(false);

        assertThrows(AlumnoNoEncontradoException.class, () -> predictStudentSpending.execute(
                studentId,
                BudgetPeriod.MENSUAL
        ));
        verify(spendingPreferenceRepository, never()).findActivePreferences(any(), any());
        verify(purchaseSpendingRepository, never()).findSpending(
                any(),
                anyCollection(),
                any(),
                any()
        );
        verify(spendingPredictionAnalyzer, never()).analyze(any());
        verify(aiEventRepository, never()).registerPrediction(any());
    }

    @Test
    void querySpendingWithCurrentAndHistoricalRanges() {
        UUID studentId = UUID.randomUUID();
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(spendingPreferenceRepository.findActivePreferences(any(), any()))
                .thenReturn(List.of());
        when(purchaseSpendingRepository.findSpending(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(), List.of());
        when(purchaseSpendingRepository.findMostConsumedCategories(
                any(),
                anyCollection(),
                any(),
                any()
        ))
                .thenReturn(List.of());
        when(spendingPredictionAnalyzer.analyze(any())).thenReturn(Optional.empty());

        predictStudentSpending.execute(studentId, BudgetPeriod.MENSUAL);

        verify(purchaseSpendingRepository).findSpending(
                eq(studentId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-05-01T00:00:00")),
                eq(LocalDateTime.parse("2026-05-21T23:59:59.999999999"))
        );
        verify(purchaseSpendingRepository).findSpending(
                eq(studentId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-01-31T00:00:00")),
                eq(LocalDateTime.parse("2026-04-30T23:59:59.999999999"))
        );
    }

    private PurchaseSpending spending(String dateTime, String amount) {
        return new PurchaseSpending(LocalDateTime.parse(dateTime), new BigDecimal(amount));
    }

    private ConsumptionCategory category(String description, String amount) {
        return new ConsumptionCategory(description, new BigDecimal(amount));
    }

    private SpendingPredictionAnalysis analysis() {
        return new SpendingPredictionAnalysis(
                "Puede superar el presupuesto.",
                List.of("Alerta de gasto."),
                List.of("Reducir snacks."),
                "gemini-test"
        );
    }
}
